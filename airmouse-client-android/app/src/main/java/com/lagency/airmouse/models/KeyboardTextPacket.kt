package com.lagency.airmouse.models

data class KeyboardTextPacket(
    val Type: String = "keyboard_text",
    val Text: String
)
