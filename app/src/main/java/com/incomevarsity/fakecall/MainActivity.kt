package com.incomevarsity.fakecall

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var alarmManager: AlarmManager
    private lateinit var prefs: android.content.SharedPreferences

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        prefs = getSharedPreferences("fakecall_prefs", Context.MODE_PRIVATE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        checkExactAlarmPermission()

        for (slot in 1..5) {
            setupSlot(slot)
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(
                    this,
                    "Please allow 'Alarms & reminders' for this app so calls arrive on time.",
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    // device may not support this settings screen
                }
            }
        }
    }

    private fun setupSlot(slot: Int) {
        val numberInput = findViewById<EditText>(resId("numberInput$slot"))
        val delayInput = findViewById<EditText>(resId("delayInput$slot"))
        val scheduleBtn = findViewById<Button>(resId("scheduleBtn$slot"))
        val cancelBtn = findViewById<Button>(resId("cancelBtn$slot"))
        val statusText = findViewById<TextView>(resId("statusText$slot"))

        // Restore saved number for convenience
        prefs.getString("slot_${slot}_number", null)?.let { numberInput.setText(it) }
        updateStatus(slot, statusText)

        scheduleBtn.setOnClickListener {
            val number = numberInput.text.toString().trim()
            val delaySecStr = delayInput.text.toString().trim()

            if (number.isEmpty()) {
                Toast.makeText(this, "Enter a fake number for Call $slot", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (delaySecStr.isEmpty()) {
                Toast.makeText(this, "Enter delay in seconds for Call $slot", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val delaySec = delaySecStr.toLongOrNull()
            if (delaySec == null || delaySec <= 0) {
                Toast.makeText(this, "Delay must be a positive number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val triggerAt = System.currentTimeMillis() + delaySec * 1000

            val intent = Intent(this, CallReceiver::class.java).apply {
                putExtra("number", number)
                putExtra("slot", slot)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                this, slot, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(this, "Please allow exact alarms permission first.", Toast.LENGTH_LONG).show()
                    checkExactAlarmPermission()
                    return@setOnClickListener
                }
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent
                )
            } catch (e: SecurityException) {
                Toast.makeText(this, "Permission denied for exact alarms.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            prefs.edit()
                .putString("slot_${slot}_number", number)
                .putLong("slot_${slot}_time", triggerAt)
                .apply()

            updateStatus(slot, statusText)
            Toast.makeText(this, "Call $slot scheduled", Toast.LENGTH_SHORT).show()
        }

        cancelBtn.setOnClickListener {
            val intent = Intent(this, CallReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this, slot, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            prefs.edit().remove("slot_${slot}_time").apply()
            updateStatus(slot, statusText)
            Toast.makeText(this, "Call $slot cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStatus(slot: Int, statusText: TextView) {
        val time = prefs.getLong("slot_${slot}_time", -1L)
        if (time > 0 && time > System.currentTimeMillis()) {
            val fmt = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
            statusText.text = "Scheduled for ${fmt.format(Date(time))}"
        } else {
            statusText.text = "Not scheduled"
        }
    }

    private fun resId(name: String): Int =
        resources.getIdentifier(name, "id", packageName)
}
