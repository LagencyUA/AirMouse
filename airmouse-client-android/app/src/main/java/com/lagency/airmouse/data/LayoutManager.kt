package com.lagency.airmouse.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lagency.airmouse.models.ControlElement
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
        createDefaultTestLayout()
    }

    fun createDefaultTestLayout() {
        if (allLayouts.any { it.name == "Default Layout" }) return
        
        val testLayout = LayoutData(
            name = "Default Layout",
            gridWidth = 12,
            gridHeight = 20,
            controls = listOf(
                ControlElement(
                    id = "btn_enter",
                    name = "Enter",
                    x = 0, y = 0, width = 4, height = 2,
                    action = "key_press",
                    payload = gson.toJson(KeyPayload(Key = "Enter")),
                    zIndex = 0
                ),
                ControlElement(
                    id = "btn_scroll_down",
                    name = "Scroll Down",
                    x = 4, y = 5, width = 4, height = 4,
                    action = "mouse_scroll",
                    payload = gson.toJson(MousePayload(Scroll = -15)),
                    zIndex = 1
                ),
                ControlElement(
                    id = "btn_rmb",
                    name = "RMB",
                    x = 8, y = 0, width = 4, height = 2,
                    action = "mouse_button",
                    payload = gson.toJson(MousePayload(Button = "right", State = "down")),
                    zIndex = 2
                )
            )
        )
        allLayouts.add(testLayout)
        saveToStorage()
    }
}
