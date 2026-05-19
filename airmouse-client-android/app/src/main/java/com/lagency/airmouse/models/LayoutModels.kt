package com.lagency.airmouse.models

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
)
