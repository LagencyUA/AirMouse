package com.lagency.airmouse.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lagency.airmouse.models.ControlElement
import com.lagency.airmouse.models.ControlType
import com.lagency.airmouse.models.KeyPayload
import com.lagency.airmouse.models.LayoutData
import com.lagency.airmouse.models.MousePayload
import java.io.File

class LayoutManager(private val context: Context) {
    private val gson = Gson()
    private val storageFile = File(context.filesDir, "layouts_v2.json")
    private var allLayouts: MutableList<LayoutData> = mutableListOf()

    init {
        loadFromStorage()
    }

    private fun loadFromStorage() {
        if (storageFile.exists()) {
            try {
                val json = storageFile.readText()
                val type = object : TypeToken<List<LayoutData>>() {}.type
                val loaded: List<LayoutData>? = gson.fromJson(json, type)
                if (loaded != null) {
                    allLayouts = loaded.toMutableList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveToStorage() {
        try {
            val json = gson.toJson(allLayouts)
            storageFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveLayout(layout: LayoutData) {
        val index = allLayouts.indexOfFirst { it.name == layout.name }
        if (index != -1) {
            allLayouts[index] = layout
        } else {
            allLayouts.add(layout)
        }
        saveToStorage()
    }

    fun loadLayout(name: String): LayoutData? {
        return allLayouts.find { it.name == name }
    }

    fun deleteLayout(name: String) {
        allLayouts.removeAll { it.name == name }
        saveToStorage()
    }

    fun renameLayout(oldName: String, newName: String): Boolean {
        if (allLayouts.any { it.name == newName }) return false
        val index = allLayouts.indexOfFirst { it.name == oldName }
        if (index != -1) {
            val oldLayout = allLayouts[index]
            allLayouts[index] = oldLayout.copy(name = newName)
            saveToStorage()
            return true
        }
        return false
    }

    fun getAllLayoutNames(): List<String> {
        return allLayouts.map { it.name }.sorted()
    }

    fun restoreDefaults() {
        allLayouts.clear()
        createDefaultLayouts()
    }

    fun createDefaultLayouts() {
        if (allLayouts.any { it.name == "Mouse" }) return
        
        val mouseLayout = LayoutData(
            name = "Mouse",
            gridWidth = 12,
            gridHeight = 20,
            controls = mutableListOf(
                ControlElement(
                    id = "mouse_pad",
                    name = "Mouse Pad",
                    x = 0, y = 0, width = 12, height = 16,
                    type = ControlType.MOUSE_PAD
                ),
                ControlElement(
                    id = "btn_mouse_x1",
                    name = "X1",
                    x = 0, y = 16, width = 2, height = 2,
                    type = ControlType.BUTTON,
                    action = "mouse_button",
                    payload = gson.toJson(MousePayload(Button = "x1"))
                ),
                ControlElement(
                    id = "btn_mouse_x2",
                    name = "X2",
                    x = 0, y = 18, width = 2, height = 2,
                    type = ControlType.BUTTON,
                    action = "mouse_button",
                    payload = gson.toJson(MousePayload(Button = "x2"))
                ),
                ControlElement(
                    id = "btn_lmb",
                    name = "LMB",
                    x = 2, y = 16, width = 4, height = 4,
                    type = ControlType.BUTTON,
                    action = "mouse_button",
                    payload = gson.toJson(MousePayload(Button = "left"))
                ),
                ControlElement(
                    id = "btn_mmb",
                    name = "MMB",
                    x = 6, y = 16, width = 2, height = 4,
                    type = ControlType.BUTTON,
                    action = "mouse_button",
                    payload = gson.toJson(MousePayload(Button = "middle"))
                ),
                ControlElement(
                    id = "btn_rmb",
                    name = "RMB",
                    x = 8, y = 16, width = 4, height = 4,
                    type = ControlType.BUTTON,
                    action = "mouse_button",
                    payload = gson.toJson(MousePayload(Button = "right"))
                )
            )
        )
        allLayouts.add(mouseLayout)
        saveToStorage()
    }
}
