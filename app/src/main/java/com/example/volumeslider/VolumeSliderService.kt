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
    private lateinit var volumeSliderRight: VolumeSliderView
    private lateinit var volumeSliderLeft: VolumeSliderView
    private lateinit var audioManager: AudioManager
    private lateinit var vibrator: Vibrator
    
    private var sensitivity: Int = 50
    private var activeEdge: String = "right"
    
    // Notification channel ID for foreground service
    private val CHANNEL_ID = "VolumeSliderChannel"
    private val NOTIFICATION_ID = 1001
    
    private var rightViewParams: WindowManager.LayoutParams? = null
    private var leftViewParams: WindowManager.LayoutParams? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Initialize services
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        
        // Create and add volume sliders
        createVolumeSliders()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Volume Slider Service",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Controls system volume from screen edges"
            
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
        )
        rightViewParams?.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        
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
        )
        leftViewParams?.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        
        // Create right slider
        volumeSliderRight = VolumeSliderView(this)
        volumeSliderRight.layoutParams = ViewGroup.LayoutParams(36, ViewGroup.LayoutParams.MATCH_PARENT)
        volumeSliderRight.setOnVolumeChangeListener(object : VolumeSliderView.OnVolumeChangeListener {
            override fun onVolumeChanged(volumePercent: Float) {
                changeVolume(volumePercent)
                provideHapticFeedback()
            }
        })
        
        // Create left slider
        volumeSliderLeft = VolumeSliderView(this)
        volumeSliderLeft.layoutParams = ViewGroup.LayoutParams(36, ViewGroup.LayoutParams.MATCH_PARENT)
        volumeSliderLeft.setOnVolumeChangeListener(object : VolumeSliderView.OnVolumeChangeListener {
            override fun onVolumeChanged(volumePercent: Float) {
                changeVolume(volumePercent)
                provideHapticFeedback()
            }
        })
        
        // Add views based on active edge
        updateActiveEdge()
    }
    
    private fun changeVolume(volumePercent: Float) {
        // Get max volume level
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        
        // Calculate new volume level
        val newVolume = (maxVolume * volumePercent / 100).toInt()
        
        // Set volume
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
            @Suppress("DEPRECATION")
            vibrator.vibrate(10)
        }
    }
    
    fun setSensitivity(value: Int) {
        sensitivity = value
        volumeSliderRight.setSensitivity(sensitivity)
        volumeSliderLeft.setSensitivity(sensitivity)
    }
    
    fun setActiveEdge(edge: String) {
        activeEdge = edge
        updateActiveEdge()
    }
    
    private fun updateActiveEdge() {
        // Remove any existing views first
        try {
            windowManager.removeView(volumeSliderRight)
        } catch (e: Exception) {
            // View might not be attached
        }
        
        try {
            windowManager.removeView(volumeSliderLeft)
        } catch (e: Exception) {
            // View might not be attached
        }
        
        // Add views based on active edge setting
        when (activeEdge) {
            "right" -> {
                windowManager.addView(volumeSliderRight, rightViewParams)
            }
            "left" -> {
                windowManager.addView(volumeSliderLeft, leftViewParams)
            }
            "both" -> {
                windowManager.addView(volumeSliderRight, rightViewParams)
                windowManager.addView(volumeSliderLeft, leftViewParams)
            }
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                "UPDATE_SETTINGS" -> {
                    setSensitivity(intent.getIntExtra("sensitivity", 50))
                    setActiveEdge(intent.getStringExtra("activeEdge") ?: "right")
                }
                "STOP_SERVICE" -> {
                    stopSelf()
                }
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
            windowManager.removeView(volumeSliderRight)
            windowManager.removeView(volumeSliderLeft)
        } catch (e: Exception) {
            // Views might not be attached
        }
    }
}
