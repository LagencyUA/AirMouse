package com.lagency.airmouse.models

import com.google.gson.Gson

enum class ControlType {
    BUTTON,
    MOUSE_PAD,
//    KEYBOARD,
//    ACCELEROMETER
//Implement in the future
}

data class LayoutData(
    var name: String,
    val gridWidth: Int = 12,
    val gridHeight: Int = 20,
    val controls: MutableList<ControlElement> = mutableListOf()
)

data class ControlElement(
    val id: String,
    var name: String,
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int,
    var type: ControlType = ControlType.BUTTON,
    var action: String = "",
    var payload: String = "", // raw JSON string
    var zIndex: Int = 0,
    var isModifier: Boolean = false,
    var sensitivity: Float = 1.0f,
    var scrollSensitivity: Float = 1.0f
) {
    fun getDisplayName(gson: Gson): String {
        if (name.isNotEmpty()) return name
        
        return when (type) {
            ControlType.MOUSE_PAD -> "Touchpad"
            ControlType.BUTTON -> {
                when (action) {
                    "mouse_button" -> {
                        try {
                            val p = gson.fromJson(payload, MousePayload::class.java)
                            when (p.Button?.lowercase()) {
                                "left" -> "LMB"
                                "right" -> "RMB"
                                "middle" -> "MMB"
                                "x1" -> "X1"
                                "x2" -> "X2"
                                else -> p.Button?.uppercase() ?: "Btn"
                            }
                        } catch (e: Exception) { "Btn" }
                    }
                    "key_press" -> {
                        try {
                            val p = gson.fromJson(payload, KeyPayload::class.java)
                            p.Key?.replace("MOUSE_", "") ?: "Key"
                        } catch (e: Exception) { "Key" }
                    }
                    "key_combo" -> {
                        try {
                            val p = gson.fromJson(payload, KeyPayload::class.java)
                            p.Keys?.joinToString("+") ?: "Combo"
                        } catch (e: Exception) { "Combo" }
                    }
                    "mouse_scroll" -> "Scroll"
                    else -> "Btn"
                }
            }
        }
    }
}
