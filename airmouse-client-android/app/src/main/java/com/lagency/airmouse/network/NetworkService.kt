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
    
    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    suspend fun connect(ip: String, port: Int): Boolean = client.connect(ip, port)

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
        val packet = SystemPacket(Action = action, Message = message)
        return client.send(packet)
    }

    suspend fun disconnect() {
        client.disconnect()
    }

    fun isConnected(): Boolean = client.isConnected()

    suspend fun startListening() = withContext(Dispatchers.IO) {
        try {
            while (client.isConnected()) {
                val line = client.read()
                if (line != null) {
                    _messages.emit(line)
                } else {
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
