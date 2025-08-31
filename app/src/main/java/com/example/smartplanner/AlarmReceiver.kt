package com.example.smartplanner

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "task_alarm_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("taskId") ?: return
        Log.d("AlarmReceiver", "Alarm triggered for taskId: $taskId")

        AlarmSoundManager.playAlarm(context)
        showNotification(context, taskId)
    }


    private fun playAlarmSound(context: Context) {
        try {
            val uri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val ringtone: Ringtone = RingtoneManager.getRingtone(context, uri)

            // Ensure the ringtone plays in ALARM mode with max volume
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }

            if (!ringtone.isPlaying) {
                ringtone.play()
                Log.d("AlarmReceiver", "Ringtone started playing")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("AlarmReceiver", "Failed to play ringtone: ${e.message}")
        }
    }

    private fun showNotification(context: Context, taskId: String) {
        val intent = Intent(context, AlarmDialogActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("taskId", taskId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Task Alarm Notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.workorder)
            .setContentTitle("Task Due")
            .setContentText("Your task is due!")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
