package com.lagency.airmouse.network

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

class TcpClientManager {
    private var socket: Socket? = null
    private var out: PrintWriter? = null
    private var input: BufferedReader? = null
    private val gson = Gson()

    suspend fun connect(ip: String, port: Int = 8888): Boolean = withContext(Dispatchers.IO) {
        try {
            socket = Socket()
            socket?.connect(InetSocketAddress(ip, port), 5000)
            val s = socket ?: return@withContext false
            out = PrintWriter(s.getOutputStream(), true)
            input = BufferedReader(InputStreamReader(s.getInputStream()))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun send(packet: Any): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(packet)
            out?.println(json)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun read(): String? = withContext(Dispatchers.IO) {
        try {
            input?.readLine()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            withContext(Dispatchers.IO) {
                out?.close()
                input?.close()
                socket?.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            socket = null
            out = null
            input = null
        }
    }

    fun isConnected(): Boolean {
        return socket?.isConnected == true && !socket!!.isClosed
    }
}
