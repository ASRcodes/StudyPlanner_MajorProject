package com.example.smartplanner
import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log

object AlarmSoundManager {
    private var ringtone: Ringtone? = null

    fun playAlarm(context: Context) {
        if (ringtone?.isPlaying == true) return // Prevent double play

        try {
            val uri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
            ringtone?.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            ringtone?.play()
            Log.d("AlarmSoundManager", "Ringtone started")

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("AlarmSoundManager", "Failed to play alarm: ${e.message}")
        }
    }

    fun stopAlarm() {
        try {
            ringtone?.stop()
            Log.d("AlarmSoundManager", "Ringtone stopped")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
