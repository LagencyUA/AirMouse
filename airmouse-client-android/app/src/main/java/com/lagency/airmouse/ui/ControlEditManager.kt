package com.lagency.airmouse.ui

import android.view.View
import android.widget.ArrayAdapter
import com.google.gson.Gson
import com.lagency.airmouse.databinding.ActivityControlBinding
import com.lagency.airmouse.models.ControlElement
import com.lagency.airmouse.models.ControlType
import com.lagency.airmouse.models.KeyPayload
import com.lagency.airmouse.models.MousePayload

class ControlEditManager(
    private val binding: ActivityControlBinding,
    private val onDelete: (ControlElement) -> Unit,
    private val onSave: (ControlElement) -> Unit
) {
    private var currentControl: ControlElement? = null
    private val gson = Gson()
    private var isUpdatingPanel = false
    
    private val predefinedKeys = listOf(
        "--- COMMON ---",
        "ENTER", "ESC", "SPACE", "TAB", "BACKSPACE",
        "--- MOUSE ---",
        "MOUSE_LEFT", "MOUSE_RIGHT", "MOUSE_MIDDLE", "MOUSE_X1", "MOUSE_X2",
        "--- MODIFIERS ---",
        "CTRL", "RCTRL", "SHIFT", "RSHIFT", "ALT", "RALT", "WIN", "CAPSLOCK", "NUMLOCK",
        "--- FUNCTION ---",
        "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12",
        "--- ARROWS ---",
        "LEFT", "UP", "RIGHT", "DOWN",
        "--- SPECIAL ---",
        "PRINTSCREEN", "SCROLLLOCK", "PAUSE", "INSERT", "DELETE", "HOME", "END", "PAGEUP", "PAGEDOWN",
        "--- MEDIA ---",
        "VOLUME_MUTE", "VOLUME_DOWN", "VOLUME_UP", "MEDIA_NEXT_TRACK", "MEDIA_PREV_TRACK", "MEDIA_STOP", "MEDIA_PLAY_PAUSE"
    )
    private val controlTypes = ControlType.values().map { it.name }
    private val modifierKeys = setOf("CTRL", "RCTRL", "SHIFT", "RSHIFT", "ALT", "RALT", "WIN", "CAPSLOCK", "NUMLOCK")

    init {
        setupListeners()
    }

    private fun setupListeners() {
        // Control Type Spinner
        val typeAdapter = ArrayAdapter(binding.root.context, android.R.layout.simple_spinner_item, controlTypes)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerControlType.adapter = typeAdapter

        // Predefined Keys Spinner with Categories
        val keyAdapter = object : ArrayAdapter<String>(binding.root.context, android.R.layout.simple_spinner_item, predefinedKeys) {
            private var defaultTextColor: android.content.res.ColorStateList? = null

            override fun isEnabled(position: Int): Boolean {
                return !getItem(position)!!.startsWith("---")
            }

            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                return super.getView(position, convertView, parent)
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val isHeader = !isEnabled(position)
                view.isEnabled = !isHeader
                
                if (view is android.widget.TextView) {
                    if (defaultTextColor == null) {
                        defaultTextColor = view.textColors
                    }

                    if (isHeader) {
                        view.setTextColor(android.graphics.Color.GRAY)
                        view.setTypeface(null, android.graphics.Typeface.BOLD)
                    } else {
                        view.setTextColor(defaultTextColor)
                        view.setTypeface(null, android.graphics.Typeface.NORMAL)
                    }
                }
                return view
            }
        }
        keyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPredefinedKeys.adapter = keyAdapter

        binding.spinnerPredefinedKeys.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateModifierCheckboxState()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Custom Payload Checkbox
        binding.checkCustomPayload.setOnCheckedChangeListener { _, isChecked ->
            binding.editCustomPayload.isEnabled = isChecked
            binding.spinnerPredefinedKeys.isEnabled = !isChecked
            updateModifierCheckboxState()
        }

        binding.spinnerControlType.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val typeStr = binding.spinnerControlType.selectedItem as? String ?: return
                val type = ControlType.valueOf(typeStr)
                binding.containerModifierOption.visibility = if (type == ControlType.BUTTON) View.VISIBLE else View.GONE
                updateModifierCheckboxState()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Z-Index Controls
        binding.btnZIndexMinus.setOnClickListener {
            val current = binding.editZIndex.text.toString().toIntOrNull() ?: 0
            if (current > 0) binding.editZIndex.setText((current - 1).toString())
        }
        binding.btnZIndexPlus.setOnClickListener {
            val current = binding.editZIndex.text.toString().toIntOrNull() ?: 0
            if (current < 100) binding.editZIndex.setText((current + 1).toString())
        }

        // Save Button
        binding.btnSaveControl.setOnClickListener {
            val control = currentControl ?: return@setOnClickListener
            
            control.type = ControlType.valueOf(binding.spinnerControlType.selectedItem as String)
            control.name = binding.editControlName.text.toString().trim()
            control.zIndex = binding.editZIndex.text.toString().toIntOrNull()?.coerceIn(0, 100) ?: 0
            control.isModifier = binding.checkIsModifier.isChecked
            
            if (control.type == ControlType.BUTTON) {
                if (binding.checkCustomPayload.isChecked) {
                    val customChar = binding.editCustomPayload.text.toString()
                    if (customChar.isNotEmpty()) {
                        control.action = "key_press"
                        control.payload = gson.toJson(KeyPayload(Key = customChar))
                    }
                } else {
                    val selectedKey = binding.spinnerPredefinedKeys.selectedItem as String
                    if (selectedKey.startsWith("MOUSE_")) {
                        control.action = "mouse_button"
                        val buttonName = selectedKey.removePrefix("MOUSE_").lowercase()
                        control.payload = gson.toJson(MousePayload(Button = buttonName))
                    } else {
                        control.action = "key_press"
                        control.payload = gson.toJson(KeyPayload(Key = selectedKey))
                    }
                }
            } else {
                // Future: handle other types (MOUSE_PAD, etc.)
                control.action = when(control.type) {
                    ControlType.MOUSE_PAD -> "mouse_move"
                    ControlType.SCROLL_BAR -> "mouse_scroll"
                    ControlType.KEYBOARD -> "system_keyboard"
                    ControlType.ACCELEROMETER -> "system_accel"
                    else -> ""
                }
            }
            
            onSave(control)
            hidePanel()
        }

        // Delete Button
        binding.btnDeleteControl.setOnClickListener {
            val control = currentControl ?: return@setOnClickListener
            androidx.appcompat.app.AlertDialog.Builder(binding.root.context)
                .setTitle("Delete Control")
                .setMessage("Are you sure you want to delete '${control.name}'?")
                .setPositiveButton("Delete") { _, _ ->
                    onDelete(control)
                    hidePanel()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    fun showPanel(control: ControlElement) {
        isUpdatingPanel = true
        currentControl = control
        binding.buttonEditPanel.visibility = View.VISIBLE
        
        binding.spinnerControlType.setSelection(controlTypes.indexOf(control.type.name))
        binding.editControlName.setText(control.name)
        binding.editZIndex.setText(control.zIndex.toString())
        binding.containerModifierOption.visibility = if (control.type == ControlType.BUTTON) View.VISIBLE else View.GONE
        
        if (control.type == ControlType.BUTTON) {
            try {
                if (control.action == "mouse_button") {
                    val payload = gson.fromJson(control.payload, MousePayload::class.java)
                    val key = "MOUSE_${payload.Button?.uppercase()}"
                    binding.checkCustomPayload.isChecked = false
                    binding.spinnerPredefinedKeys.setSelection(predefinedKeys.indexOf(key))
                } else {
                    val payload = gson.fromJson(control.payload, KeyPayload::class.java)
                    val key = payload.Key ?: ""
                    
                    if (predefinedKeys.contains(key)) {
                        binding.checkCustomPayload.isChecked = false
                        binding.spinnerPredefinedKeys.setSelection(predefinedKeys.indexOf(key))
                    } else {
                        binding.checkCustomPayload.isChecked = true
                        binding.editCustomPayload.setText(key)
                    }
                }
            } catch (e: Exception) {
                binding.checkCustomPayload.isChecked = false
                binding.spinnerPredefinedKeys.setSelection(0)
            }
        }
        
        updateModifierCheckboxState()
        binding.checkIsModifier.isChecked = control.isModifier
        isUpdatingPanel = false
    }

    fun hidePanel() {
        binding.buttonEditPanel.visibility = View.GONE
        currentControl = null
    }

    private fun updateModifierCheckboxState() {
        if (isUpdatingPanel) return
        
        val typeStr = binding.spinnerControlType.selectedItem as? String ?: return
        val type = ControlType.valueOf(typeStr)
        val isCustom = binding.checkCustomPayload.isChecked
        val selectedKey = binding.spinnerPredefinedKeys.selectedItem as? String ?: ""
        
        val canBeModifier = type == ControlType.BUTTON && !isCustom && modifierKeys.contains(selectedKey)
        
        binding.checkIsModifier.isEnabled = canBeModifier
        if (!canBeModifier) {
            binding.checkIsModifier.isChecked = false
        }
    }
}
