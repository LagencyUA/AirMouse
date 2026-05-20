package com.lagency.airmouse.ui

import com.google.gson.Gson
import com.lagency.airmouse.models.ControlElement
import com.lagency.airmouse.models.KeyPayload
import com.lagency.airmouse.models.MousePayload

class KeyHandler(
    private val onControlClick: (ControlElement) -> Unit,
    private val onActivationChanged: (ControlElement, Boolean) -> Unit
) {
    private val gson = Gson()
    private val activeModifiers = mutableSetOf<ControlElement>()

    fun resetModifiers() {
        val modifiersToClear = activeModifiers.toList()
        activeModifiers.clear()
        modifiersToClear.forEach { onActivationChanged(it, false) }
    }

    fun handleTouch(control: ControlElement, state: String) {
        if (control.isModifier) {
            if (state == "down") {
                if (activeModifiers.contains(control)) {
                    activeModifiers.remove(control)
                    onActivationChanged(control, false)
                } else {
                    activeModifiers.add(control)
                    onActivationChanged(control, true)
                }
            }
            return
        }

        // For non-modifiers, if it's a key_press and we have active modifiers
        if (control.action == "key_press" && activeModifiers.isNotEmpty()) {
            if (state == "down") {
                // Send sequence: modifiers + this key
                val payloadObj = gson.fromJson(control.payload, KeyPayload::class.java)
                val sequence = activeModifiers.mapNotNull {
                    gson.fromJson(it.payload, KeyPayload::class.java).Key
                } + (payloadObj.Key ?: "")

                val newPayload = KeyPayload(Keys = sequence, State = "press")
                val tempControl = control.copy(action = "key_combo", payload = gson.toJson(newPayload))
                onControlClick(tempControl)
            }
        } else {
            // Normal handling with states
            val newPayload = if (control.action == "key_press") {
                val p = gson.fromJson(control.payload, KeyPayload::class.java)
                gson.toJson(p.copy(State = state))
            } else if (control.action == "mouse_button") {
                val p = gson.fromJson(control.payload, MousePayload::class.java)
                gson.toJson(p.copy(State = if (state == "down") "down" else "up"))
            } else {
                control.payload
            }

            val tempControl = control.copy(payload = newPayload)
            onControlClick(tempControl)
        }
    }
}
