package com.lagency.airmouse.models

data class CommandPacket(
    val Type: String = "command",
    val Button: String
)
