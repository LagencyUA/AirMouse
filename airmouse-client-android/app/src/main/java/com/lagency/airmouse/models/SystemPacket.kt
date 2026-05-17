package com.lagency.airmouse.models

data class SystemPacket(
    val Type: String = "system",
    val Action: String,
    val Message: String? = null
)
