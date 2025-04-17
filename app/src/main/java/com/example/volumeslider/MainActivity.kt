package com.example.volumeslider

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    private val OVERLAY_PERMISSION_REQUEST_CODE = 1234
    
    private var sensitivity: Int = 50
    private var serviceEnabled: Boolean = true
    private var activeEdge: String = "right"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Check for overlay permission
        checkOverlayPermission()
        
        // Setup sensitivity slider
        val sensitivitySlider = findViewById<SeekBar>(R.id.sensitivity_slider)
        sensitivitySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                sensitivity = progress
                updateServiceSettings()
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Setup service toggle
        val serviceSwitch = findViewById<Switch>(R.id.service_switch)
        serviceSwitch.setOnCheckedChangeListener { _, isChecked ->
            serviceEnabled = isChecked
            toggleService(isChecked)
        }
        
        // Setup edge selection
        val edgeGroup = findViewById<RadioGroup>(R.id.edge_selection)
        edgeGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.edge_right -> activeEdge = "right"
                R.id.edge_left -> activeEdge = "left"
                R.id.edge_both -> activeEdge = "both"
            }
            updateServiceSettings()
        }
    }
    
    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
            } else {
                // Permission already granted, start service
                startVolumeSliderService()
            }
        } else {
            // Not needed for lower API levels
            startVolumeSliderService()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    startVolumeSliderService()
                } else {
                    Toast.makeText(this, 
                        "Overlay permission denied. Service can't run in background.", 
                        Toast.LENGTH_LONG).show()
                    
                    // Disable service switch since permissions not granted
                    findViewById<Switch>(R.id.service_switch).isChecked = false
                    serviceEnabled = false
                }
            }
        }
    }
    
    private fun startVolumeSliderService() {
        val intent = Intent(this, VolumeSliderService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        updateServiceSettings()
    }
    
    private fun toggleService(enabled: Boolean) {
        if (enabled) {
            startVolumeSliderService()
        } else {
            val intent = Intent(this, VolumeSliderService::class.java)
            intent.action = "STOP_SERVICE"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }
    
    private fun updateServiceSettings() {
        if (serviceEnabled) {
            val intent = Intent(this, VolumeSliderService::class.java)
            intent.action = "UPDATE_SETTINGS"
            intent.putExtra("sensitivity", sensitivity)
            intent.putExtra("activeEdge", activeEdge)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }
}
