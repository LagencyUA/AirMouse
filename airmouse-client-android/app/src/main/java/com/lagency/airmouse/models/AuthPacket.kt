package com.lagency.airmouse.models

data class AuthPacket(
    val Type: String = "auth",
    val Pin: String,
    val DeviceName: String
)
