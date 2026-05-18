package com.lagency.airmouse

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lagency.airmouse.data.SettingsManager
import com.lagency.airmouse.databinding.ActivityConnectBinding
import com.lagency.airmouse.network.ConnectionHolder
import kotlinx.coroutines.launch

class ConnectActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConnectBinding
    private lateinit var settingsManager: SettingsManager
    private val networkService = ConnectionHolder.networkService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConnectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsManager = SettingsManager(this)
        
        binding.ipInput.setText(settingsManager.serverIp ?: "")
        binding.portInput.setText(settingsManager.serverPort.toString())

        binding.connectButton.setOnClickListener {
            val ip = binding.ipInput.text.toString().trim()
            val portStr = binding.portInput.text.toString().trim()
            val pin = binding.pinInput.text.toString().trim()

            if (ip.isEmpty() || pin.isEmpty()) {
                binding.statusText.text = "Status: Please enter IP and PIN"
                return@setOnClickListener
            }

            val port = portStr.toIntOrNull() ?: 5000
            connectToServer(ip, port, pin)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!networkService.isConnected()) {
            binding.statusText.text = "Status: Disconnected"
        } else {
            binding.statusText.text = "Status: Connected!"
        }
    }

    private fun connectToServer(ip: String, port: Int, pin: String) {
        lifecycleScope.launch {
            binding.connectButton.isEnabled = false
            binding.statusText.text = "Status: Connecting to $ip:$port..."

            val connected = networkService.connect(ip, port)
            if (connected) {
                binding.statusText.text = "Status: Authenticating..."
                
                val response = networkService.authenticate(pin, Build.MODEL)
                if (response != null) {
                    if (response.Success) {
                        settingsManager.serverIp = ip
                        settingsManager.serverPort = port
                        binding.statusText.text = "Status: Connected!"
                        navigateToControl()
                    } else {
                        binding.statusText.text = "Status: Auth failed: ${response.Message}"
                        networkService.disconnect()
                    }
                } else {
                    binding.statusText.text = "Status: No response from server"
                    networkService.disconnect()
                }
            } else {
                binding.statusText.text = "Status: Connection failed"
            }
            binding.connectButton.isEnabled = true
        }
    }

    private fun navigateToControl() {
        startActivity(Intent(this, ControlActivity::class.java))
    }
}
