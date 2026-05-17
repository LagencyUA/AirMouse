package com.lagency.airmouse.models

data class AuthResponse(
    val Type: String = "auth_response",
    val Success: Boolean,
    val Message: String? = null
)
