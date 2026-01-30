package com.example.smsalarm

import android.app.*
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.*
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    private var player: MediaPlayer? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification())
        setVolumeMax()
        playSound()

        Handler(Looper.getMainLooper()).postDelayed({
            stopSelf()
        }, 10 * 60 * 1000)
    }

    private fun playSound() {
        player = MediaPlayer.create(this, R.raw.alarm)
        player?.isLooping = true
        player?.start()
    }

    private fun setVolumeMax() {
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        am.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
            0
        )
    }

    private fun createNotification(): Notification {
        val channelId = "alarm_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "短信报警", NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("短信报警中")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .build()
    }

    override fun onDestroy() {
        player?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}
