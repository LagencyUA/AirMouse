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

    fun getAllLayoutNames(): List<String> {
        return layoutsDir.listFiles()?.map { it.nameWithoutExtension } ?: emptyList()
    }

    fun createDefaultTestLayout() {
        val testLayout = LayoutData(
            name = "Test Layout",
            gridWidth = 12,
            gridHeight = 20,
            controls = listOf(
                ControlElement(
                    id = "btn_enter",
                    name = "Enter",
                    x = 0, y = 0, width = 4, height = 2,
                    action = "key_press",
                    payload = KeyPayload(Key = "Enter")
                ),
                ControlElement(
                    id = "btn_scroll_down",
                    name = "Scroll Down",
                    x = 4, y = 5, width = 4, height = 4,
                    action = "mouse_scroll",
                    payload = MousePayload(Scroll = -15)
                ),
                ControlElement(
                    id = "btn_rmb",
                    name = "RMB",
                    x = 8, y = 0, width = 4, height = 2,
                    action = "mouse_button",
                    payload = MousePayload(Button = "right", State = "down")
                )
            )
        )
        saveLayout(testLayout)
    }
}
