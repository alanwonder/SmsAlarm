package com.example.smsalarm

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
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
    private var audioFocusRequest: AudioFocusRequest? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isStopped = false

    override fun onCreate() {
        super.onCreate()
        createNotification()
        setVolumeMax()
        playSound()

        val playDuration = MonitorConfig.getRingDurationMinutes(this) * 60 * 1000L
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
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            requestAlarmAudioFocus(audioAttributes)

            mediaPlayer = MediaPlayer.create(this, R.raw.alarm, audioAttributes, 0)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        }
    }

    private fun setVolumeMax() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val alarmMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, alarmMaxVolume, 0)

        // 兼容部分 ROM：MediaPlayer 默认可能仍走音乐流，双保险拉满。
        val musicMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            musicMaxVolume,
            0
        )
    }

    private fun requestAlarmAudioFocus(audioAttributes: AudioAttributes) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(audioAttributes)
                .build()
            audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
    }

    /**
     * ★ 统一的停止逻辑（自动 / 手动 都走这里）
     */
    private fun stopAlarm() {
        if (isStopped) {
            return
        }
        isStopped = true

        handler.removeCallbacksAndMessages(null)
        releaseMediaPlayer()
        abandonAudioFocus()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.let {
            runCatching { it.stop() }
            it.release()
        }
        mediaPlayer = null
    }

    private fun abandonAudioFocus() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
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
        releaseMediaPlayer()
        abandonAudioFocus()

        super.onDestroy()
    }


    override fun onBind(intent: Intent?): IBinder? = null
}
