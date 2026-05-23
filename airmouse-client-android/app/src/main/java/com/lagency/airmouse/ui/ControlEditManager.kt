package com.lagency.airmouse.ui

import android.view.View
import android.view.ViewGroup
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
        val typeAdapter = ArrayAdapter(binding.root.context, android.R.layout.simple_spinner_item, controlTypes)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerControlType.adapter = typeAdapter

        val keyAdapter = object : ArrayAdapter<String>(binding.root.context, android.R.layout.simple_spinner_item, predefinedKeys) {
            private var defaultTextColor: android.content.res.ColorStateList? = null

            override fun isEnabled(position: Int): Boolean {
                return !getItem(position)!!.startsWith("---")
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                if (view is android.widget.TextView) {
                    val item = getItem(position)
                    if (item != null && item.startsWith("---")) {
                        // If somehow a header is selected, show next valid item or just neutral text
                        view.setTextColor(android.graphics.Color.GRAY)
                    }
                }
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val isHeader = !isEnabled(position)
                view.isEnabled = !isHeader
                if (view is android.widget.TextView) {
                    if (defaultTextColor == null) defaultTextColor = view.textColors
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
        // Set default selection to the first valid item (usually "ENTER")
        binding.spinnerPredefinedKeys.setSelection(1)

        binding.spinnerPredefinedKeys.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position != -1 && !keyAdapter.isEnabled(position)) {
                    // If a header is somehow selected (e.g. via keyboard or initial state), 
                    // move to the next valid item
                    binding.spinnerPredefinedKeys.setSelection(position + 1)
                    return
                }
                updateModifierCheckboxState()
                updateHintForCurrentSelection()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        binding.checkCustomPayload.setOnCheckedChangeListener { _, isChecked ->
            binding.editCustomPayload.isEnabled = isChecked
            binding.spinnerPredefinedKeys.isEnabled = !isChecked
            updateModifierCheckboxState()
            updateHintForCurrentSelection()
        }

        binding.editCustomPayload.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateHintForCurrentSelection()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        binding.spinnerControlType.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val typeStr = binding.spinnerControlType.selectedItem as? String ?: return
                val type = ControlType.valueOf(typeStr)
                updatePanelsVisibility(type)
                updateModifierCheckboxState()
                updateHintForCurrentSelection()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        binding.seekSensitivity.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val value = 0.25f + (progress * 0.25f)
                binding.txtSensitivity.text = "Movement Sensitivity: $value"
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        binding.seekScrollSensitivity.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val value = 0.25f + (progress * 0.25f)
                binding.txtScrollSensitivity.text = "Scroll Sensitivity: $value"
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        binding.btnZIndexMinus.setOnClickListener {
            val current = binding.editZIndex.text.toString().toIntOrNull() ?: 0
            if (current > 0) binding.editZIndex.setText((current - 1).toString())
        }
        binding.btnZIndexPlus.setOnClickListener {
            val current = binding.editZIndex.text.toString().toIntOrNull() ?: 0
            if (current < 100) binding.editZIndex.setText((current + 1).toString())
        }

        binding.btnSaveControl.setOnClickListener {
            val control = currentControl ?: return@setOnClickListener
            control.type = ControlType.valueOf(binding.spinnerControlType.selectedItem as String)
            control.name = binding.editControlName.text.toString().trim()
            control.zIndex = binding.editZIndex.text.toString().toIntOrNull()?.coerceIn(0, 100) ?: 0
            control.isModifier = binding.checkIsModifier.isChecked
            control.sensitivity = 0.25f + (binding.seekSensitivity.progress * 0.25f)
            control.scrollSensitivity = 0.25f + (binding.seekScrollSensitivity.progress * 0.25f)
            
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
                control.action = when(control.type) {
                    ControlType.MOUSE_PAD -> "mouse_move"
                    ControlType.KEYBOARD -> "keyboard_text"
                    ControlType.GYRO_MOUSE -> "mouse_move"
                    else -> ""
                }
            }
            onSave(control)
            hidePanel()
        }

        binding.btnDeleteControl.setOnClickListener {
            val control = currentControl ?: return@setOnClickListener
            androidx.appcompat.app.AlertDialog.Builder(binding.root.context)
                .setTitle("Delete Control")
                .setMessage("Are you sure you want to delete '${control.getDisplayName(gson)}'?")
                .setPositiveButton("Delete") { _, _ ->
                    onDelete(control)
                    hidePanel()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun updateHintForCurrentSelection() {
        if (isUpdatingPanel) return
        val typeStr = binding.spinnerControlType.selectedItem as? String ?: return
        val type = ControlType.valueOf(typeStr)
        if (type == ControlType.MOUSE_PAD || type == ControlType.KEYBOARD || type == ControlType.GYRO_MOUSE) {
            binding.editControlName.hint = when(type) {
                ControlType.MOUSE_PAD -> "Touchpad"
                ControlType.KEYBOARD -> "Keyboard"
                ControlType.GYRO_MOUSE -> "Gyro Mouse"
                else -> ""
            }
            return
        }

        val tempControl = currentControl?.copy() ?: return
        tempControl.type = type
        if (binding.checkCustomPayload.isChecked) {
            val custom = binding.editCustomPayload.text.toString()
            tempControl.action = "key_press"
            tempControl.payload = gson.toJson(KeyPayload(Key = custom))
        } else {
            val key = binding.spinnerPredefinedKeys.selectedItem as? String ?: ""
            if (key.startsWith("MOUSE_")) {
                tempControl.action = "mouse_button"
                tempControl.payload = gson.toJson(MousePayload(Button = key.removePrefix("MOUSE_").lowercase()))
            } else {
                tempControl.action = "key_press"
                tempControl.payload = gson.toJson(KeyPayload(Key = key))
            }
        }
        val originalName = tempControl.name
        tempControl.name = "" // Force auto-name for hint
        binding.editControlName.hint = tempControl.getDisplayName(gson)
        tempControl.name = originalName
    }

    fun showPanel(control: ControlElement) {
        isUpdatingPanel = true
        currentControl = control
        binding.buttonEditPanel.visibility = View.VISIBLE
        binding.spinnerControlType.setSelection(controlTypes.indexOf(control.type.name))
        binding.editControlName.setText(control.name)
        binding.editZIndex.setText(control.zIndex.toString())
        
        val sensProgress = ((control.sensitivity - 0.25f) / 0.25f).toInt().coerceIn(0, 7)
        binding.seekSensitivity.progress = sensProgress
        binding.txtSensitivity.text = "Movement Sensitivity: ${0.25f + sensProgress * 0.25f}"

        val scrollProgress = ((control.scrollSensitivity - 0.25f) / 0.25f).toInt().coerceIn(0, 7)
        binding.seekScrollSensitivity.progress = scrollProgress
        binding.txtScrollSensitivity.text = "Scroll Sensitivity: ${0.25f + scrollProgress * 0.25f}"

        updatePanelsVisibility(control.type)
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
        updateHintForCurrentSelection()
    }

    fun hidePanel() {
        binding.buttonEditPanel.visibility = View.GONE
        currentControl = null
    }

    private fun updatePanelsVisibility(type: ControlType) {
        val isButton = type == ControlType.BUTTON
        val isMousePad = type == ControlType.MOUSE_PAD
        val isKeyboard = type == ControlType.KEYBOARD
        val isGyro = type == ControlType.GYRO_MOUSE

        binding.editControlName.visibility = if (isMousePad || isKeyboard || isGyro) View.GONE else View.VISIBLE
        val nameLabel = binding.editControlName.parent.let { it as? ViewGroup }?.getChildAt(
            (binding.editControlName.parent as ViewGroup).indexOfChild(binding.editControlName) - 1
        )
        nameLabel?.visibility = if (isMousePad || isKeyboard || isGyro) View.GONE else View.VISIBLE
        
        binding.containerModifierOption.visibility = if (isButton) View.VISIBLE else View.GONE
        
        val predefinedLabel = binding.spinnerPredefinedKeys.parent.let { it as? ViewGroup }?.getChildAt(
            (binding.spinnerPredefinedKeys.parent as ViewGroup).indexOfChild(binding.spinnerPredefinedKeys) - 1
        )
        predefinedLabel?.visibility = if (isButton) View.VISIBLE else View.GONE
        binding.spinnerPredefinedKeys.visibility = if (isButton) View.VISIBLE else View.GONE
        
        binding.checkCustomPayload.parent.let { it as? View }?.visibility = if (isButton) View.VISIBLE else View.GONE
        binding.editCustomPayload.visibility = if (isButton) View.VISIBLE else View.GONE
        val customPayloadLabel = binding.editCustomPayload.parent.let { it as? ViewGroup }?.getChildAt(
            (binding.editCustomPayload.parent as ViewGroup).indexOfChild(binding.editCustomPayload) - 1
        )
        customPayloadLabel?.visibility = if (isButton) View.VISIBLE else View.GONE
        
        binding.containerMousePadSettings.visibility = if (isMousePad || isGyro) View.VISIBLE else View.GONE
        
        // Z-Index settings are now always visible for all types as requested
        val zIndexContainer = binding.editZIndex.parent as? View
        zIndexContainer?.visibility = View.VISIBLE
        val zIndexLabel = zIndexContainer?.let { (it.parent as ViewGroup).getChildAt((it.parent as ViewGroup).indexOfChild(it) - 1) }
        zIndexLabel?.visibility = View.VISIBLE
    }

    private fun updateModifierCheckboxState() {
        if (isUpdatingPanel) return
        val typeStr = binding.spinnerControlType.selectedItem as? String ?: return
        val type = ControlType.valueOf(typeStr)
        val isCustom = binding.checkCustomPayload.isChecked
        val selectedKey = binding.spinnerPredefinedKeys.selectedItem as? String ?: ""
        val canBeModifier = type == ControlType.BUTTON && !isCustom && modifierKeys.contains(selectedKey)
        binding.checkIsModifier.isEnabled = canBeModifier
        if (!canBeModifier) binding.checkIsModifier.isChecked = false
    }
}
