package com.scott.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("scott_prefs", Context.MODE_PRIVATE)

        val scroll = ScrollView(this)
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(40, 60, 40, 60)
        }

        val toggleAuto = Switch(this).apply {
            text = "Enable SCOTT WhatsApp Auto-Reply"
            isChecked = prefs.getBoolean("auto_reply_enabled", false)
        }

        val apiKeyInput = EditText(this).apply {
            hint = "Paste free Groq API key (gsk_...)"
            setText(prefs.getString("groq_api_key", ""))
        }

        val friendsInput = EditText(this).apply {
            hint = "Allowed friend names (comma separated, e.g. Rohan, Arjun)"
            setText(prefs.getString("allowed_friends", ""))
        }

        val scheduleInput = EditText(this).apply {
            hint = "Add schedule or reminder (e.g. 5 PM football)"
        }

        val schedulesDisplay = TextView(this).apply {
            text = "Saved Schedules:\n" + (prefs.getString("schedules_list", "None yet") ?: "None yet")
            setPadding(0, 20, 0, 20)
        }

        val saveBtn = Button(this).apply {
            text = "Save SCOTT Settings & Schedule"
            setOnClickListener {
                val newItem = scheduleInput.text.toString().trim()
                val currentList = prefs.getString("schedules_list", "") ?: ""
                val updatedList = if (newItem.isNotEmpty()) {
                    if (currentList.isEmpty()) "• $newItem" else "$currentList\n• $newItem"
                } else {
                    currentList
                }

                prefs.edit()
                    .putBoolean("auto_reply_enabled", toggleAuto.isChecked)
                    .putString("groq_api_key", apiKeyInput.text.toString().trim())
                    .putString("allowed_friends", friendsInput.text.toString().trim())
                    .putString("schedules_list", updatedList)
                    .apply()

                scheduleInput.setText("")
                schedulesDisplay.text = "Saved Schedules:\n" +
                    (if (updatedList.isEmpty()) "None yet" else updatedList)
                Toast.makeText(this@MainActivity, "Saved!", Toast.LENGTH_SHORT).show()
            }
        }

        val openWhatsAppBtn = Button(this).apply {
            text = "Open WhatsApp"
            setOnClickListener {
                val launchIntent = packageManager.getLaunchIntentForPackage("com.whatsapp")
                if (launchIntent != null) {
                    startActivity(launchIntent)
                } else {
                    Toast.makeText(this@MainActivity, "WhatsApp not found", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val permBtn = Button(this).apply {
            text = "Open Notification Access Settings"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        layout.addView(toggleAuto)
        layout.addView(apiKeyInput)
        layout.addView(friendsInput)
        layout.addView(scheduleInput)
        layout.addView(saveBtn)
        layout.addView(schedulesDisplay)
        layout.addView(openWhatsAppBtn)
        layout.addView(permBtn)

        scroll.addView(layout)
        setContentView(scroll)
    }
}
