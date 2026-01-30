package com.example.smsalarm

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.smsalarm.R
class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val playDuration = 10 * 60 * 1000L // 10分钟

    override fun onCreate() {
        super.onCreate()
        createNotification()
        setVolumeMax()
        playSound()

        // 10分钟后停止服务
        Handler(Looper.getMainLooper()).postDelayed({
            stopSelf()
        }, playDuration)
    }

    private fun playSound() {
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm) // 放在 res/raw/alarm.mp3
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
    }

    private fun setVolumeMax() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
            0
        )
    }

    private fun createNotification() {
        val channelId = "alarm_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "短信报警",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("交通提示报警")
            .setContentText("已触发关键词短信")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
