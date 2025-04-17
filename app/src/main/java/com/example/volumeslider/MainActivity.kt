package com.example.volumeslider

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    private val OVERLAY_PERMISSION_REQUEST_CODE = 1234
    
    private var sensitivity: Int = 50
    private var serviceEnabled: Boolean = true
    private var activeEdge: String = "right"
    private var bound: Boolean = false
    private var serverIp:String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val ipInput: EditText = findViewById(R.id.server_ip_input)
        val connectButton: Button = findViewById(R.id.connect_button)
        val serviceButton: Button = findViewById(R.id.service_button)
        
        // Check for overlay permission
        checkOverlayPermission()
        
        // Setup sensitivity slider
        val sensitivitySlider: SeekBar = findViewById(R.id.sensitivity_slider)
        sensitivitySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener { // added object
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    sensitivity = progress
                    updateServiceSettings()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        connectButton.setOnClickListener {
                serverIp = ipInput.text.toString()
                Toast.makeText(this, "Connecting to :$serverIp", Toast.LENGTH_SHORT).show()
        }
        serviceButton.setOnClickListener {
            if(serviceEnabled)
            {
                toggleService(false)
                serviceButton.text = "Start service"
            }
            else
            {
                toggleService(true)
                serviceButton.text = "Stop service"

            }
            serviceEnabled = !serviceEnabled
            if(serviceEnabled) {
                startVolumeSliderService()
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
                    serviceEnabled = false //added
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
