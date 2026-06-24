package com.incomevarsity.fakecall

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val number = intent.getStringExtra("number") ?: "Unknown"
        val slot = intent.getIntExtra("slot", 0)

        val callIntent = Intent(context, IncomingCallActivity::class.java).apply {
            putExtra("number", number)
            putExtra("slot", slot)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }
        context.startActivity(callIntent)

        // Mark slot as no longer scheduled
        val prefs = context.getSharedPreferences("fakecall_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("slot_${slot}_time").apply()
    }
}
