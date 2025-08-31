package com.example.smartplanner

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class AlarmDialogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_dialog)

        // 🚫 Do NOT stop alarm here! Let it keep ringing until user presses stop.

        val stopButton: Button = findViewById(R.id.btnStopAlarm)
        stopButton.setOnClickListener {
            // ✅ Stop alarm only when user taps stop
            AlarmSoundManager.stopAlarm()
            navigateToHome()
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        // 🚫 Do not stop alarm on destroy — only stop on button press
        super.onDestroy()
    }
}
