package com.lagency.airmouse.models

data class SessionInfo(
    val serverIp: String = "",
    val connectedAt: Long = 0,
    val isActive: Boolean = false
)
