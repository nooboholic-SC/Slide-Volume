package com.example.volumeslider

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import android.widget.RadioGroup
import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.Switch
import android.view.View

class MainActivity : AppCompatActivity() {
    
    private lateinit var volumeSliderRight: VolumeSliderView
    private lateinit var volumeSliderLeft: VolumeSliderView
    private lateinit var audioManager: AudioManager
    private lateinit var vibrator: Vibrator
    
    private var sensitivity: Int = 50
    private var serviceEnabled: Boolean = true
    private var activeEdge: String = "right"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize audio manager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        // Initialize vibrator service
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        
        // Find views
        volumeSliderRight = findViewById(R.id.volume_slider_right)
        volumeSliderLeft = findViewById(R.id.volume_slider_left)
        
        // Configure volume sliders
        setupVolumeSliders()
        
        // Setup sensitivity slider
        val sensitivitySlider = findViewById<SeekBar>(R.id.sensitivity_slider)
        sensitivitySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                sensitivity = progress
                updateSensitivity()
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Setup service toggle
        val serviceSwitch = findViewById<Switch>(R.id.service_switch)
        serviceSwitch.setOnCheckedChangeListener { _, isChecked ->
            serviceEnabled = isChecked
            updateServiceState()
        }
        
        // Setup edge selection
        val edgeGroup = findViewById<RadioGroup>(R.id.edge_selection)
        edgeGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.edge_right -> activeEdge = "right"
                R.id.edge_left -> activeEdge = "left"
                R.id.edge_both -> activeEdge = "both"
            }
            updateActiveEdge()
        }
        
        // Initial settings
        updateSensitivity()
        updateServiceState()
        updateActiveEdge()
    }
    
    private fun setupVolumeSliders() {
        // Set up the right volume slider
        volumeSliderRight.setOnVolumeChangeListener(object : VolumeSliderView.OnVolumeChangeListener {
            override fun onVolumeChanged(volumePercent: Float) {
                changeVolume(volumePercent)
                // Provide haptic feedback
                provideHapticFeedback()
            }
        })
        
        // Set up the left volume slider
        volumeSliderLeft.setOnVolumeChangeListener(object : VolumeSliderView.OnVolumeChangeListener {
            override fun onVolumeChanged(volumePercent: Float) {
                changeVolume(volumePercent)
                // Provide haptic feedback
                provideHapticFeedback()
            }
        })
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
            AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
        )
        
        // Show current volume as a toast message
        Toast.makeText(this, "Volume: $newVolume/$maxVolume", Toast.LENGTH_SHORT).show()
    }
    
    private fun provideHapticFeedback() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(10)
        }
    }
    
    private fun updateSensitivity() {
        volumeSliderRight.setSensitivity(sensitivity)
        volumeSliderLeft.setSensitivity(sensitivity)
    }
    
    private fun updateServiceState() {
        if (serviceEnabled) {
            // Enable the active edges based on settings
            updateActiveEdge()
        } else {
            // Disable both edges
            volumeSliderRight.visibility = VolumeSliderView.View.INVISIBLE
            volumeSliderLeft.visibility = VolumeSliderView.View.INVISIBLE
        }
    }
    
    private fun updateActiveEdge() {
        if (!serviceEnabled) {
            volumeSliderRight.visibility = VolumeSliderView.View.INVISIBLE
            volumeSliderLeft.visibility = VolumeSliderView.View.INVISIBLE
            return
        }
        
        when (activeEdge) {
            "right" -> {
                volumeSliderRight.visibility = VolumeSliderView.View.VISIBLE
                volumeSliderLeft.visibility = VolumeSliderView.View.INVISIBLE
            }
            "left" -> {
                volumeSliderRight.visibility = VolumeSliderView.View.INVISIBLE
                volumeSliderLeft.visibility = VolumeSliderView.View.VISIBLE
            }
            "both" -> {
                volumeSliderRight.visibility = VolumeSliderView.View.VISIBLE
                volumeSliderLeft.visibility = VolumeSliderView.View.VISIBLE
            }
        }
    }
}
