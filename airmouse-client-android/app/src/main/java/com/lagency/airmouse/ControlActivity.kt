package com.lagency.airmouse

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.lagency.airmouse.databinding.ActivityControlBinding
import androidx.activity.OnBackPressedCallback
import com.lagency.airmouse.models.InputPacket
import com.lagency.airmouse.models.KeyPayload
import com.lagency.airmouse.models.MousePayload
import com.lagency.airmouse.models.SystemPacket
import com.lagency.airmouse.network.ConnectionHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ControlActivity : AppCompatActivity() {
    private lateinit var binding: ActivityControlBinding
    private val gson = Gson()
    private var listenJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityControlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val tcpClient = ConnectionHolder.tcpClientManager

        if (tcpClient == null || !tcpClient.isConnected()) {
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                disconnectAndExit()
            }
        })

        startListening()

        binding.btnDisconnect.setOnClickListener {
            disconnectAndExit()
        }

        binding.btnMouseMove.setOnClickListener {
            sendInput("mouse_move", MousePayload(DX = 10, DY = 10))
        }

        binding.btnMouseButton.setOnClickListener {
            sendInput("mouse_button", MousePayload(Button = "left", State = "down"))
            // Usually we'd want to send "up" shortly after or on release, but for test:
            lifecycleScope.launch {
                kotlinx.coroutines.delay(100)
                sendInput("mouse_button", MousePayload(Button = "left", State = "up"))
            }
        }

        binding.btnMouseScroll.setOnClickListener {
            sendInput("mouse_scroll", MousePayload(Scroll = -1)) // Scroll down
        }

        binding.btnKeyPress.setOnClickListener {
            sendInput("key_press", KeyPayload(Key = "Enter"))
        }

        binding.btnKeyCombo.setOnClickListener {
            sendInput("key_combo", KeyPayload(Keys = listOf("Ctrl", "C")))
        }
    }

    private fun sendInput(action: String, payload: Any) {
        val tcpClient = ConnectionHolder.tcpClientManager ?: return
        lifecycleScope.launch {
            val payloadJson = gson.toJson(payload)
            val packet = InputPacket(Action = action, Payload = payloadJson)
            val sent = tcpClient.send(packet)
            if (!sent) {
                Toast.makeText(this@ControlActivity, "Failed to send $action", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startListening() {
        val tcpClient = ConnectionHolder.tcpClientManager ?: return
        listenJob = lifecycleScope.launch {
            while (tcpClient.isConnected()) {
                val line = try {
                    tcpClient.read()
                } catch (e: Exception) {
                    null
                }

                if (line == null) {
                    // Socket closed or error
                    runOnUiThread {
                        if (!isFinishing) {
                            Toast.makeText(this@ControlActivity, "Server connection lost", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                    break
                }

                try {
                    val basePacket = gson.fromJson(line, Map::class.java)
                    if (basePacket["Type"] == "system" && basePacket["Action"] == "disconnect") {
                        val message = basePacket["Message"] as? String
                        runOnUiThread {
                            Toast.makeText(this@ControlActivity, message, Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        break
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun disconnectAndExit() {
        val tcpClient = ConnectionHolder.tcpClientManager
        lifecycleScope.launch {
            withContext(NonCancellable) {
                if (tcpClient != null && tcpClient.isConnected()) {
                    tcpClient.send(SystemPacket(Action = "disconnect"))
                    tcpClient.disconnect()
                }
                runOnUiThread {
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        listenJob?.cancel()
        // Ensure we disconnect if we haven't already
        val tcpClient = ConnectionHolder.tcpClientManager
        if (tcpClient != null && tcpClient.isConnected()) {
            lifecycleScope.launch {
                withContext(NonCancellable) {
                    tcpClient.disconnect()
                }
            }
        }
        super.onDestroy()
    }
}
