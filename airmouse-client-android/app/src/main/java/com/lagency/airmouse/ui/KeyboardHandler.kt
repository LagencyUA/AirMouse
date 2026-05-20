package com.lagency.airmouse.ui

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.google.gson.Gson
import com.lagency.airmouse.models.ControlElement
import com.lagency.airmouse.models.KeyPayload
import com.lagency.airmouse.models.KeyboardTextPacket

class KeyboardHandler(
    private val context: Context,
    private val getActiveModifiers: () -> List<String>,
    private val onSendPacket: (String, String) -> Unit
) {
    private val gson = Gson()
    private var hiddenEditText: EditText? = null
    private var lastSentText = " "
    private var isInternalChange = false

    fun attachToView(parent: View) {
        val viewGroup = parent as? android.view.ViewGroup ?: return
        
        if (hiddenEditText == null) {
            hiddenEditText = object : androidx.appcompat.widget.AppCompatEditText(context) {
                override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
                    val conn = super.onCreateInputConnection(outAttrs)
                    return if (conn != null) {
                        object : InputConnectionWrapper(conn, true) {
                            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                                // If the text is just our base character, we need to send backspace manually
                                // because onTextChanged won't trigger or will trigger for empty string.
                                if (text.toString() == " " && beforeLength > 0) {
                                    handleSpecialKey("BACKSPACE")
                                }
                                return super.deleteSurroundingText(beforeLength, afterLength)
                            }

                            override fun sendKeyEvent(event: KeyEvent): Boolean {
                                if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DEL) {
                                    if (text.toString() == " ") {
                                        handleSpecialKey("BACKSPACE")
                                        return true
                                    }
                                }
                                return super.sendKeyEvent(event)
                            }
                        }
                    } else null
                }
            }.apply {
                layoutParams = android.view.ViewGroup.LayoutParams(1, 1)
                alpha = 0.01f
                isFocusable = true
                isFocusableInTouchMode = true
                // Enable suggestions and standard text input
                inputType = android.text.InputType.TYPE_CLASS_TEXT
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                imeOptions = EditorInfo.IME_ACTION_DONE
                
                // Use a stable character for the base so we can detect deletions
                setText(" ")
                setSelection(1)

                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        if (isInternalChange) return
                        
                        val currentText = s?.toString() ?: ""
                        
                        if (currentText.isEmpty()) {
                            // Base character deleted
                            handleSpecialKey("BACKSPACE")
                            resetBuffer()
                            return
                        }

                        syncDelta(currentText)
                    }
                    override fun afterTextChanged(s: Editable?) {}
                })

                setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                        handleSpecialKey("ENTER")
                        resetBuffer()
                        return@setOnKeyListener true
                    }
                    false
                }
            }
        }
        
        // Ensure it's attached to the current parent
        if (hiddenEditText?.parent == null) {
            viewGroup.addView(hiddenEditText)
        } else if (hiddenEditText?.parent != viewGroup) {
            (hiddenEditText?.parent as? android.view.ViewGroup)?.removeView(hiddenEditText)
            viewGroup.addView(hiddenEditText)
        }
    }

    private fun syncDelta(current: String) {
        val modifiers = getActiveModifiers()
        
        if (modifiers.isNotEmpty()) {
            // In combo mode, we process character by character and reset frequently
            if (current.length > lastSentText.length) {
                val added = current.substring(lastSentText.length)
                handleInput(added)
            }
            lastSentText = current
            // Reset buffer almost immediately for combos to keep state clean
            if (current.length > 2) resetBuffer()
            return
        }

        // 1. Find common prefix
        var commonLen = 0
        while (commonLen < lastSentText.length && commonLen < current.length && 
               lastSentText[commonLen] == current[commonLen]) {
            commonLen++
        }
        
        // 2. Backspace for removed part
        val toDelete = lastSentText.length - commonLen
        if (toDelete > 0) {
            repeat(toDelete) { handleSpecialKey("BACKSPACE") }
        }
        
        // 3. Send new part
        val toAdd = current.substring(commonLen)
        if (toAdd.isNotEmpty()) {
            handleInput(toAdd)
        }
        
        lastSentText = current
        
        // 4. Periodically reset to keep buffer small, but not too often to disrupt IME
        if (current.length > 40 || (current.endsWith(" ") && current.length > 10)) {
            resetBuffer()
        }
    }

    private fun resetBuffer() {
        isInternalChange = true
        hiddenEditText?.post {
            hiddenEditText?.setText(" ")
            hiddenEditText?.setSelection(1)
            lastSentText = " "
            isInternalChange = false
        }
    }

    fun showKeyboard() {
        hiddenEditText?.let { et ->
            et.requestFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            et.postDelayed({
                et.requestFocus()
                @Suppress("DEPRECATION")
                if (!imm.showSoftInput(et, InputMethodManager.SHOW_FORCED)) {
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
                }
            }, 100)
        }
    }

    private fun handleInput(text: String) {
        val modifiers = getActiveModifiers()
        if (modifiers.isEmpty()) {
            val packet = KeyboardTextPacket(Text = text)
            onSendPacket("keyboard_text", gson.toJson(packet))
        } else {
            // Process character by character if multiple were entered (unlikely but possible)
            for (char in text) {
                val s = char.toString()
                if (KeyHandler.isModifierAllowedForKey(s)) {
                    val combo = KeyPayload(Keys = modifiers + s, State = "press")
                    onSendPacket("key_combo", gson.toJson(combo))
                }
                // If not allowed, we send nothing as per instructions
            }
        }
    }

    private fun handleSpecialKey(key: String) {
        val modifiers = getActiveModifiers()
        if (modifiers.isEmpty()) {
            val payload = KeyPayload(Key = key, State = "press")
            onSendPacket("key_press", gson.toJson(payload))
        } else {
            if (KeyHandler.isModifierAllowedForKey(key)) {
                val combo = KeyPayload(Keys = modifiers + key, State = "press")
                onSendPacket("key_combo", gson.toJson(combo))
            }
        }
    }
}
