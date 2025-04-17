package com.example.volumeslider

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class VolumeSliderService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 1001
    }


    private lateinit var vibrator: Vibrator

    private lateinit var windowManager: WindowManager
    private lateinit var audioManager: AudioManager
    private var volumeSlider: VolumeSliderView? = null
    private var sensitivity: Int = 50
    private var activeEdge: String = "right" // Default to right edge
    private val CHANNEL_ID = "VolumeSliderChannel" // Notification channel ID
    private var viewParams: WindowManager.LayoutParams? = null
    private var isSliderAdded = false // Track if slider is currently added to the window

    // Service lifecycle methods
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        createVolumeSliders()
    }

    // Notification-related methods
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
        // Initialize view parameters
        viewParams = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            (resources.displayMetrics.heightPixels * 0.6).toInt(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            // Initial gravity will be updated in updateActiveEdge()
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            x = 0
            y = 0
        }

        // Create SliderView
        volumeSlider = VolumeSliderView(this)
        volumeSlider?.setOnVolumeChangeListener(object : VolumeSliderView.OnVolumeChangeListener {
            override fun onVolumeChanged(volumePercent: Float) {
                changeVolume(volumePercent)
                provideHapticFeedback()
            }
        })

        // Set default values
        updateActiveEdge()
    }


    // Vibration feedback method
    private fun provideHapticFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION") // Suppress the deprecation warning for older APIs
            vibrator.vibrate(10)
        }
    }

    // Setting methods
    private fun setSensitivity(value: Int) {
        sensitivity = value
    }

    // Update and change edge side
    fun setActiveEdge(edge: String) {
        activeEdge = edge.lowercase()
        updateActiveEdge()
    }

    private fun updateActiveEdge() {
        // Remove slider from window if it exists
        if (isSliderAdded && volumeSlider != null) {
            windowManager.removeView(volumeSlider)
            isSliderAdded = false
        }

        // Update gravity based on active edge
        viewParams?.gravity = when (activeEdge) {
            "left" -> Gravity.START or Gravity.CENTER_VERTICAL
            "right" -> Gravity.END or Gravity.CENTER_VERTICAL
            else -> Gravity.END or Gravity.CENTER_VERTICAL // Default to right
        }

        // Set x-offset to align with the edge
        viewParams?.x = 0

        // Add the view with updated params
        if (volumeSlider != null) {
            windowManager.addView(volumeSlider, viewParams)
            isSliderAdded = true
        }

        volumeSlider?.setSliderSensitivity(sensitivity.toFloat())
    }

    private fun changeVolume(volumePercent: Float) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val newVolume = (maxVolume.toFloat() * volumePercent).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
    }

    // Service commands
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

    // Binding method
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // Cleanup on service destruction
    override fun onDestroy() {
        super.onDestroy()
        // Remove the view from the window if it was added
        if (isSliderAdded && volumeSlider != null) {
            windowManager.removeView(volumeSlider)
            isSliderAdded = false
        }
        // Remove slider reference
        volumeSlider = null
    }



}