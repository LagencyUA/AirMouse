package com.lagency.airmouse.network

object ConnectionHolder {
    val networkService = NetworkService()
    
    // Legacy support for parts not yet refactored
    val tcpClientManager: TcpClientManager? 
        get() = null // We should migrate away from direct manager access
}
