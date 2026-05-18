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
    private val layoutsDir = File(context.filesDir, "layouts").apply { if (!exists()) mkdirs() }

    fun saveLayout(layout: LayoutData) {
        val file = File(layoutsDir, "${layout.name}.json")
        val json = gson.toJson(layout)
        file.writeText(json)
    }

    fun loadLayout(name: String): LayoutData? {
        val file = File(layoutsDir, "$name.json")
        if (!file.exists()) return null
        
        return try {
            val json = file.readText()
            // Note: Polymorphic payload handling might be needed if payload is Any
            // For now, let's handle the specific payload types we know
            gson.fromJson(json, LayoutData::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteLayout(name: String) {
        val file = File(layoutsDir, "$name.json")
        if (file.exists()) file.delete()
    }

    fun renameLayout(oldName: String, newName: String): Boolean {
        val oldFile = File(layoutsDir, "$oldName.json")
        val newFile = File(layoutsDir, "$newName.json")
        if (oldFile.exists() && !newFile.exists()) {
            val layout = loadLayout(oldName) ?: return false
            val updatedLayout = layout.copy(name = newName)
            saveLayout(updatedLayout)
            oldFile.delete()
            return true
        }
        return false
    }

    fun getAllLayoutNames(): List<String> {
        return layoutsDir.listFiles()?.map { it.nameWithoutExtension }?.sorted() ?: emptyList()
    }

    fun restoreDefaults() {
        layoutsDir.listFiles()?.forEach { it.delete() }
        createDefaultTestLayout()
        // Add more default layouts here if needed
    }

    fun createDefaultTestLayout() {
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
                    payload = KeyPayload(Key = "Enter"),
                    zIndex = 0
                ),
                ControlElement(
                    id = "btn_scroll_down",
                    name = "Scroll Down",
                    x = 4, y = 5, width = 4, height = 4,
                    action = "mouse_scroll",
                    payload = MousePayload(Scroll = -15),
                    zIndex = 1
                ),
                ControlElement(
                    id = "btn_rmb",
                    name = "RMB",
                    x = 8, y = 0, width = 4, height = 2,
                    action = "mouse_button",
                    payload = MousePayload(Button = "right", State = "down"),
                    zIndex = 2
                )
            )
        )
        saveLayout(testLayout)
        // Clean up old "Test Layout.json" if it exists to avoid confusion
        File(layoutsDir, "Test Layout.json").delete()
    }
}
