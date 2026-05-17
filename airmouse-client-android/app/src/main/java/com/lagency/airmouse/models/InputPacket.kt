package com.lagency.airmouse.models

data class InputPacket(
    val Type: String = "input",
    val Action: String,
    val Payload: String // JSON string
)
