package com.scott.ai

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

class ScottNotificationService : NotificationListenerService() {

    private val executor = Executors.newSingleThreadExecutor()
    private var tts: TextToSpeech? = null

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("en", "IN")
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val item = sbn ?: return
        if (item.packageName != "com.whatsapp") return

        val prefs = getSharedPreferences("scott_prefs", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("auto_reply_enabled", false)
        val apiKey = prefs.getString("groq_api_key", "") ?: ""
        val allowedRaw = prefs.getString("allowed_friends", "") ?: ""

        val extras = item.notification.extras
        val sender = extras.getString(Notification.EXTRA_TITLE)?.trim() ?: return
        val message = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: return
        if (message.isEmpty()) return

        val allowedList = allowedRaw.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

        val senderMatches = allowedList.isNotEmpty() &&
            allowedList.any { sender.lowercase().contains(it) }

        if (!senderMatches) return

        val lowerMsg = message.lowercase()
        if (lowerMsg.contains("call me") || lowerMsg.contains("urgent") || lowerMsg.contains("emergency")) {
            triggerUrgentAlert(sender, message)
        }

        if (!enabled || apiKey.isEmpty()) return

        val replyAction = findReplyAction(item.notification) ?: return

        executor.execute {
            val aiReply = askGroq(apiKey, sender, message) ?: return@execute
            sendRemoteReply(replyAction, aiReply)
        }
    }

    private fun triggerUrgentAlert(sender: String, message: String) {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val ringtone = RingtoneManager.getRingtone(applicationContext, uri)
            ringtone?.play()
        } catch (_: Exception) {
        }
        tts?.speak("Urgent alert from $sender: $message", TextToSpeech.QUEUE_FLUSH, null, "scott_alert")
    }

    private fun findReplyAction(notification: Notification): Notification.Action? {
        val actions = notification.actions ?: return null
        for (action in actions) {
            val inputs = action.remoteInputs ?: continue
            if (inputs.any { it.allowFreeFormInput }) {
                return action
            }
        }
        return null
    }

    private fun sendRemoteReply(action: Notification.Action, replyText: String) {
        val remoteInputs = action.remoteInputs ?: return
        val resultsBundle = Bundle()
        for (input in remoteInputs) {
            resultsBundle.putCharSequence(input.resultKey, replyText)
        }
        val fillInIntent = Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        RemoteInput.addResultsToIntent(remoteInputs, fillInIntent, resultsBundle)
        try {
            action.actionIntent.send(this, 0, fillInIntent)
        } catch (_: Exception) {
        }
    }

    private fun askGroq(apiKey: String, sender: String, incomingText: String): String? {
        return try {
            val url = URL("https://api.groq.com/openai/v1/chat/completions")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json")
            }

            val body = JSONObject().apply {
                put("model", "llama-3.3-70b-versatile")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", "You are SCOTT, replying casually like a friendly 15-year-old Indian guy to a close friend on WhatsApp. Keep it short, natural, 1-2 lines max.")
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "Friend $sender sent: $incomingText")
                    })
                })
            }

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

            if (conn.responseCode !in 200..299) return null

            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)
            val choices = json.getJSONArray("choices")
            if (choices.length() == 0) return null
            choices.getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
        } catch (_: Exception) {
            null
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        executor.shutdown()
        super.onDestroy()
    }
}
