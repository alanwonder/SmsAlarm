package com.example.smsalarm

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    companion object {
        const val CHANNEL_ID = "alarm_channel"
        const val ACTION_STOP_ALARM = "com.example.smsalarm.STOP_ALARM"
        const val NOTIFICATION_ID = 1
    }

    private var mediaPlayer: MediaPlayer? = null
    private val playDuration = 10 * 60 * 1000L // 10分钟
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        createNotification()
        setVolumeMax()
        playSound()

        // 10分钟后自动停止
        handler.postDelayed({
            stopAlarm()
        }, playDuration)
    }

    /**
     * ★ 关键：用于接收“停止报警”Action
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_ALARM) {
            stopAlarm()
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    private fun playSound() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.alarm)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        }
    }

    private fun setVolumeMax() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
            0
        )
    }

    /**
     * ★ 统一的停止逻辑（自动 / 手动 都走这里）
     */
    private fun stopAlarm() {
        handler.removeCallbacksAndMessages(null)

        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification() {

        // Android 8+
        val channel = NotificationChannel(
            CHANNEL_ID,
            "短信报警",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        // ===== 通知上的“停止报警”按钮 =====
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_STOP_ALARM
        }

        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("交通提示报警")
            .setContentText("点击可提前停止报警")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setOngoing(true) // 前台服务，不能滑掉
            .addAction(
                android.R.drawable.ic_media_pause,
                "停止报警",
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)

        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        super.onDestroy()
    }


    override fun onBind(intent: Intent?): IBinder? = null
}
