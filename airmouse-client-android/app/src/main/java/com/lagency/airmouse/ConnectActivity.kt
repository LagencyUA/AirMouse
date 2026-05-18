package com.lagency.airmouse

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.lagency.airmouse.data.SettingsManager
import com.lagency.airmouse.databinding.ActivityConnectBinding
import com.lagency.airmouse.models.AuthPacket
import com.lagency.airmouse.models.AuthResponse
import com.lagency.airmouse.network.ConnectionHolder
import com.lagency.airmouse.network.TcpClientManager
import kotlinx.coroutines.launch

class ConnectActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConnectBinding
    private val tcpClientManager = TcpClientManager()
    private lateinit var settingsManager: SettingsManager
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConnectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsManager = SettingsManager(this)
        
        // Попередньо заповнюємо IP та порт, якщо вони є в налаштуваннях
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
        // Update status if we returned from ControlActivity and connection is lost
        val tcpClient = ConnectionHolder.tcpClientManager
        if (tcpClient == null || !tcpClient.isConnected()) {
            binding.statusText.text = "Status: Disconnected"
        } else {
            binding.statusText.text = "Status: Connected!"
        }
    }

    private fun connectToServer(ip: String, port: Int, pin: String) {
        lifecycleScope.launch {
            binding.connectButton.isEnabled = false
            binding.statusText.text = "Status: Connecting to $ip:$port..."

            val connected = tcpClientManager.connect(ip, port)
            if (connected) {
                binding.statusText.text = "Status: Authenticating..."
                
                val authPacket = AuthPacket(
                    Pin = pin,
                    DeviceName = Build.MODEL
                )

                val sent = tcpClientManager.send(authPacket)
                if (sent) {
                    val responseJson = tcpClientManager.read()
                    if (responseJson != null) {
                        try {
                            val response = gson.fromJson(responseJson, AuthResponse::class.java)
                            if (response.Success) {
                                // Зберігаємо успішні налаштування
                                settingsManager.serverIp = ip
                                settingsManager.serverPort = port
                                
                                // Зберігаємо з'єднання для наступної Activity
                                ConnectionHolder.tcpClientManager = tcpClientManager
                                
                                binding.statusText.text = "Status: Connected!"
                                navigateToControl()
                            } else {
                                binding.statusText.text = "Status: Auth failed: ${response.Message}"
                                tcpClientManager.disconnect()
                            }
                        } catch (e: Exception) {
                            binding.statusText.text = "Status: Protocol error"
                            tcpClientManager.disconnect()
                        }
                    } else {
                        binding.statusText.text = "Status: No response from server"
                        tcpClientManager.disconnect()
                    }
                } else {
                    binding.statusText.text = "Status: Failed to send auth packet"
                    tcpClientManager.disconnect()
                }
            } else {
                binding.statusText.text = "Status: Connection failed"
            }
            binding.connectButton.isEnabled = true
        }
    }

    private fun navigateToControl() {
        val intent = Intent(this, ControlActivity::class.java)
        startActivity(intent)
        // Можна завершити ConnectActivity, якщо ми не плануємо сюди повертатися кнопкою "Назад"
        // finish() 
    }

    override fun onDestroy() {
        super.onDestroy()
        // Не закриваємо з'єднання тут, бо ми передали його в ConnectionHolder
    }
}
