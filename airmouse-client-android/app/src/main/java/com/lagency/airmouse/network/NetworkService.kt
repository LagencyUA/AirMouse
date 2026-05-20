package com.lagency.airmouse.network

import com.google.gson.Gson
import com.lagency.airmouse.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

class NetworkService {
    private val client = TcpClientManager()
    private val gson = Gson()
    private var isIntentionalDisconnect = false
    
    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    suspend fun connect(ip: String, port: Int): Boolean {
        isIntentionalDisconnect = false
        return client.connect(ip, port)
    }

    suspend fun authenticate(pin: String, deviceName: String): AuthResponse? {
        val packet = AuthPacket(Pin = pin, DeviceName = deviceName)
        if (client.send(packet)) {
            val response = client.read() ?: return null
            return try {
                gson.fromJson(response, AuthResponse::class.java)
            } catch (e: Exception) {
                null
            }
        }
        return null
    }

    suspend fun sendInput(action: String, payloadJson: String): Boolean {
        val packet = InputPacket(Action = action, Payload = payloadJson)
        return client.send(packet)
    }

    suspend fun sendSystemAction(action: String, message: String? = null): Boolean {
        if (action == "disconnect") isIntentionalDisconnect = true
        val packet = SystemPacket(Action = action, Message = message)
        return client.send(packet)
    }

    suspend fun disconnect() {
        isIntentionalDisconnect = true
        client.disconnect()
    }

    fun isConnected(): Boolean = client.isConnected()

    suspend fun startListening() = withContext(Dispatchers.IO) {
        try {
            while (client.isConnected()) {
                val line = client.read()
                if (line != null) {
                    _messages.emit(line)
                    // If the message itself was a disconnect signal from server, we might want to mark it
                    try {
                        val map = gson.fromJson(line, Map::class.java)
                        if (map["Type"] == "system" && map["Action"] == "disconnect") {
                            isIntentionalDisconnect = true
                        }
                    } catch (e: Exception) {}
                } else {
                    // Stream closed
                    if (!isIntentionalDisconnect) {
                        _messages.emit(gson.toJson(mapOf("Type" to "system", "Action" to "disconnect", "Message" to "Connection lost")))
                    }
                    break
                }
            }
        } catch (e: Exception) {
            if (!isIntentionalDisconnect) {
                _messages.emit(gson.toJson(mapOf("Type" to "system", "Action" to "disconnect", "Message" to "Connection lost")))
            }
            if (e !is java.io.IOException || !isIntentionalDisconnect) {
                e.printStackTrace()
            }
        }
    }
}
