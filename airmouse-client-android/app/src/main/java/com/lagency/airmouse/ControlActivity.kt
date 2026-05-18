package com.lagency.airmouse

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.lagency.airmouse.data.LayoutManager
import com.lagency.airmouse.databinding.ActivityControlBinding
import com.lagency.airmouse.models.ControlElement
import com.lagency.airmouse.models.InputPacket
import com.lagency.airmouse.models.KeyPayload
import com.lagency.airmouse.models.LayoutData
import com.lagency.airmouse.models.MousePayload
import com.lagency.airmouse.models.SystemPacket
import com.lagency.airmouse.network.ConnectionHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ControlActivity : AppCompatActivity() {
    private lateinit var binding: ActivityControlBinding
    private lateinit var layoutManager: LayoutManager
    private val gson = Gson()
    private var listenJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityControlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        layoutManager = LayoutManager(this)
        val tcpClient = ConnectionHolder.tcpClientManager

        if (tcpClient == null || !tcpClient.isConnected()) {
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupHeader()
        startListening()
        loadDefaultLayout()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                disconnectAndExit()
            }
        })
    }

    private fun setupHeader() {
        // ... (previous setupHeader code)
    }

    private fun loadDefaultLayout() {
        // Create test layout if no layouts exist
        if (layoutManager.getAllLayoutNames().isEmpty()) {
            layoutManager.createDefaultTestLayout()
        }

        val testLayout = layoutManager.loadLayout("Test Layout")
        if (testLayout != null) {
            binding.layoutWorkingArea.setLayout(testLayout) { control ->
                handleControlClick(control)
            }
        }
    }

    private fun handleControlClick(control: ControlElement) {
        val payload = control.payload ?: return
        
        sendInput(control.action, payload)
        
        // If it was a mouse_button "down", send "up" after delay for test
        // When loading from JSON, payload might be a LinkedTreeMap
        val mousePayload = if (payload is MousePayload) payload 
                          else try { gson.fromJson(gson.toJson(payload), MousePayload::class.java) } catch(e: Exception) { null }

        if (control.action == "mouse_button" && mousePayload?.State == "down") {
            lifecycleScope.launch {
                kotlinx.coroutines.delay(100)
                val upPayload = mousePayload.copy(State = "up")
                sendInput(control.action, upPayload)
            }
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
                            val toastMsg = if (message == "host_shutdown") "Server closed connection" else "Disconnected"
                            Toast.makeText(this@ControlActivity, toastMsg, Toast.LENGTH_SHORT).show()
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
