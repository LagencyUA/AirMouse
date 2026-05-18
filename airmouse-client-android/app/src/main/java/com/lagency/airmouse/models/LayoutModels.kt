package com.lagency.airmouse.models

data class LayoutData(
    val name: String,
    val gridWidth: Int = 12, // Default grid width (columns)
    val gridHeight: Int = 20, // Default grid height (rows)
    val controls: List<ControlElement> = emptyList()
)

data class ControlElement(
    val id: String,
    val name: String,
    val x: Int, // Grid position X
    val y: Int, // Grid position Y
    val width: Int, // Span columns
    val height: Int, // Span rows
    val action: String, // "key_press", "mouse_button", "mouse_scroll", etc.
    val payload: Any? = null
)
