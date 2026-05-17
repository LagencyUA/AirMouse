package com.lagency.airmouse.models

data class MousePayload(
    val DX: Int = 0,
    val DY: Int = 0,
    val Button: String? = null,
    val State: String? = null,
    val Scroll: Int = 0
)
