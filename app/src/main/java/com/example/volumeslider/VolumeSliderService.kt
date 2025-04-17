package com.example.volumeslider

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.*
import android.view.*
import android.widget.Toast
import androidx.core.app.NotificationCompat

class VolumeSliderService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var volumeSliderLeft: VolumeSliderView
    private lateinit var volumeSliderRight: VolumeSliderView
    private lateinit var audioManager: AudioManager
    companion object {
        private const val NOTIFICATION_ID = 1001
    }


    private lateinit var vibrator: Vibrator

    private var sensitivity: Int = 50
    private var activeEdge: String = "right"

    // Notification channel ID for foreground service
    private val CHANNEL_ID = "VolumeSliderChannel"
    private var rightViewParams: WindowManager.LayoutParams? = null
    private var leftViewParams: WindowManager.LayoutParams? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        createVolumeSliders()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Volume Slider Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls system volume from screen edges."
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Volume Slider Active")
            .setContentText("Swipe at the edge of the screen to adjust volume")
            .setSmallIcon(R.drawable.ic_volume_icon)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createVolumeSliders() {
        // Layout params for right edge
        rightViewParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            (resources.displayMetrics.heightPixels * 0.6).toInt(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }

        // Layout params for left edge
        leftViewParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            (resources.displayMetrics.heightPixels * 0.6).toInt(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
        }

        // Create Slider
        volumeSliderLeft = VolumeSliderView(this)
        volumeSliderLeft.layoutParams = ViewGroup.LayoutParams(36, ViewGroup.LayoutParams.MATCH_PARENT)
        volumeSliderLeft.setOnVolumeChangeListener(object : VolumeSliderView.OnVolumeChangeListener {
            override fun onVolumeChanged(volumePercent: Float) {
                changeVolume(volumePercent)
                provideHapticFeedback()
            }
        })

        updateActiveEdge()
    }
    
    private fun changeVolume(volumePercent: Float) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val newVolume = (maxVolume.toFloat() * volumePercent).toInt()

        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            newVolume,
            AudioManager.FLAG_SHOW_UI
        )
    }

    private fun provideHapticFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION") // Suppress the deprecation warning for older APIs
            vibrator.vibrate(10)
        }
    }
    private fun setSensitivity(value: Int) {
        sensitivity = value
    }
    
    fun setActiveEdge(edge: String) {
        activeEdge = edge
        updateActiveEdge()
    }
    
    private fun updateActiveEdge() {        
        try {
            windowManager.removeView(volumeSliderLeft)
        } catch (e: Exception) {
            //View not present
        }

        when (activeEdge) {
            "right" -> {
                rightViewParams?.let { windowManager.addView(volumeSliderLeft, it) }
            }
            "left" -> {
                leftViewParams?.let { windowManager.addView(volumeSliderLeft, it) }
            }
            else -> {
                rightViewParams?.let { windowManager.addView(volumeSliderLeft, it) }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                "UPDATE_SETTINGS" -> setSensitivity(it.getIntExtra("sensitivity", 50))
                "UPDATE_SETTINGS" -> setActiveEdge(it.getStringExtra("activeEdge") ?: "right")
                "STOP_SERVICE" -> stopSelf()
            }
        }
        return START_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
 override fun onDestroy() {
        super.onDestroy()
        // Clean up views
        try {
            windowManager.removeView(volumeSliderLeft)
        } catch (e: Exception) {
            //View not present
        }
    }
}