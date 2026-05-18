package com.lagency.airmouse.models

data class LayoutData(
    val name: String,
    val gridWidth: Int = 12,
    val gridHeight: Int = 20,
    val controls: List<ControlElement> = emptyList()
)

data class ControlElement(
    val id: String,
    val name: String,
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int,
    val action: String,
    val payload: String, // Changed to String (raw JSON) for better performance/reliability
    var zIndex: Int = 0
)
