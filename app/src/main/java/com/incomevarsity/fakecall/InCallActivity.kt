package com.incomevarsity.fakecall

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class InCallActivity : AppCompatActivity() {

    private var seconds = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timerText: TextView

    private val tick = object : Runnable {
        override fun run() {
            seconds++
            val mins = seconds / 60
            val secs = seconds % 60
            timerText.text = String.format("%02d:%02d", mins, secs)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_call)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        val number = intent.getStringExtra("number") ?: "Unknown"
        findViewById<TextView>(R.id.inCallNumberText).text = number
        timerText = findViewById(R.id.callTimerText)

        handler.post(tick)

        findViewById<android.widget.Button>(R.id.endCallButton).setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }
}
