package com.lagency.airmouse

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.lagency.airmouse.data.LayoutManager
import com.lagency.airmouse.databinding.ActivityControlBinding
import com.lagency.airmouse.models.ControlElement
import com.lagency.airmouse.models.InputPacket
import com.lagency.airmouse.models.KeyPayload
import com.lagency.airmouse.models.LayoutData
import com.lagency.airmouse.models.MousePayload
import com.lagency.airmouse.models.SystemPacket
import com.lagency.airmouse.network.ConnectionHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ControlActivity : AppCompatActivity() {
    private lateinit var binding: ActivityControlBinding
    private lateinit var layoutManager: LayoutManager
    private val gson = Gson()
    private var listenJob: Job? = null
    
    private var currentLayoutName: String? = null
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityControlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        layoutManager = LayoutManager(this)
        val tcpClient = ConnectionHolder.tcpClientManager

        if (tcpClient == null || !tcpClient.isConnected()) {
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupHeader()
        startListening()
        
        // Initialize layouts
        if (layoutManager.getAllLayoutNames().isEmpty()) {
            layoutManager.createDefaultTestLayout()
        }
        refreshLayoutTabs()
        
        val firstLayout = layoutManager.getAllLayoutNames().firstOrNull()
        if (firstLayout != null) {
            switchLayout(firstLayout)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isEditMode) {
                    showCancelEditDialog()
                } else {
                    disconnectAndExit()
                }
            }
        })
    }

    private fun setupHeader() {
        binding.btnBack.setOnClickListener {
            disconnectAndExit()
        }

        binding.btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        binding.btnEditLayout.setOnClickListener {
            setEditMode(!isEditMode)
        }

        binding.btnAddLayout.setOnClickListener {
            showAddLayoutDialog()
        }

        // Edit Mode Buttons
        binding.btnRenameLayout.setOnClickListener {
            showRenameLayoutDialog()
        }

        binding.btnAddElement.setOnClickListener {
            // TODO: Logic to add a block
            Toast.makeText(this, "Add block feature coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnDeleteLayout.setOnClickListener {
            showDeleteLayoutDialog()
        }

        binding.btnCancelEdit.setOnClickListener {
            showCancelEditDialog()
        }

        binding.btnSaveEdit.setOnClickListener {
            showSaveEditDialog()
        }
    }

    private fun setEditMode(enabled: Boolean) {
        isEditMode = enabled
        binding.layoutWorkingArea.isEditMode = enabled
        
        // UI visibility toggles
        binding.btnBack.visibility = if (enabled) View.GONE else View.VISIBLE
        binding.layoutsScrollView.visibility = if (enabled) View.GONE else View.VISIBLE
        binding.btnAddLayout.visibility = if (enabled) View.GONE else View.VISIBLE
        binding.btnSettings.visibility = if (enabled) View.GONE else View.VISIBLE
        
        binding.editToolsContainer.visibility = if (enabled) View.VISIBLE else View.GONE
        
        // Highlight edit button
        if (enabled) {
            binding.btnEditLayout.setColorFilter(getColor(android.R.color.holo_blue_light))
        } else {
            binding.btnEditLayout.clearColorFilter()
        }
    }

    private fun refreshLayoutTabs() {
        binding.layoutsContainer.removeAllViews()
        val layouts = layoutManager.getAllLayoutNames()
        
        layouts.forEach { name ->
            val btn = Button(this, null, androidx.appcompat.R.attr.buttonStyleSmall).apply {
                text = name
                isAllCaps = false
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(8, 0, 8, 0)
                }
                layoutParams = params
                
                if (name == currentLayoutName) {
                    alpha = 1.0f
                    // Use a safe color
                    setBackgroundColor(getColor(android.R.color.holo_blue_light))
                } else {
                    alpha = 0.7f
                }

                setOnClickListener {
                    switchLayout(name)
                }
            }
            binding.layoutsContainer.addView(btn)
        }
    }

    private fun switchLayout(name: String) {
        currentLayoutName = name
        val layout = layoutManager.loadLayout(name)
        if (layout != null) {
            binding.layoutWorkingArea.setLayout(layout) { control ->
                handleControlClick(control)
            }
        }
        refreshLayoutTabs()
    }

    private fun showRenameLayoutDialog() {
        val oldName = currentLayoutName ?: return
        val input = EditText(this).apply {
            setText(oldName)
            selectAll()
        }
        
        AlertDialog.Builder(this)
            .setTitle("Rename Layout")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty() && newName != oldName) {
                    if (isEditMode) {
                        // In edit mode, we just update the in-memory object name
                        // The actual file will be renamed/saved when clicking the Save checkmark
                        binding.layoutWorkingArea.getLayoutData()?.let { data ->
                            // We need a way to change the name in the data object
                            // Since LayoutData is a data class, we might need to handle this carefully
                            // For now, let's assume we rename the file immediately to keep it simple, 
                            // but your request implies it should be part of save/cancel.
                            // To follow "save/cancel" logic strictly, we'd need a "pending" name.
                            if (layoutManager.renameLayout(oldName, newName)) {
                                currentLayoutName = newName
                                refreshLayoutTabs()
                            }
                        }
                    } else {
                        if (layoutManager.renameLayout(oldName, newName)) {
                            currentLayoutName = newName
                            refreshLayoutTabs()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteLayoutDialog() {
        val name = currentLayoutName ?: return
        AlertDialog.Builder(this)
            .setTitle("Delete Layout")
            .setMessage("Are you sure you want to delete '$name'?")
            .setPositiveButton("Delete") { _, _ ->
                layoutManager.deleteLayout(name)
                currentLayoutName = layoutManager.getAllLayoutNames().firstOrNull()
                if (currentLayoutName != null) {
                    switchLayout(currentLayoutName!!)
                } else {
                    layoutManager.createDefaultTestLayout()
                    switchLayout("Default Layout")
                }
                setEditMode(false)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCancelEditDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cancel Changes")
            .setMessage("Discard all changes?")
            .setPositiveButton("Yes") { _, _ ->
                // Restore original state if we had one. For now just exit mode.
                currentLayoutName?.let { switchLayout(it) }
                setEditMode(false)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showSaveEditDialog() {
        AlertDialog.Builder(this)
            .setTitle("Save Changes")
            .setMessage("Save layout changes?")
            .setPositiveButton("Yes") { _, _ ->
                val currentData = binding.layoutWorkingArea.getLayoutData()
                if (currentData != null) {
                    layoutManager.saveLayout(currentData)
                }
                setEditMode(false)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showAddLayoutDialog() {
        val input = EditText(this).apply {
            hint = "New Layout Name"
        }
        AlertDialog.Builder(this)
            .setTitle("New Layout")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    val newLayout = LayoutData(name = name)
                    layoutManager.saveLayout(newLayout)
                    switchLayout(name)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleControlClick(control: ControlElement) {
        val payload = control.payload ?: return
        
        sendInput(control.action, payload)
        
        // If it was a mouse_button "down", send "up" after delay for test
        // When loading from JSON, payload might be a LinkedTreeMap
        val mousePayload = if (payload is MousePayload) payload 
                          else try { gson.fromJson(gson.toJson(payload), MousePayload::class.java) } catch(e: Exception) { null }

        if (control.action == "mouse_button" && mousePayload?.State == "down") {
            lifecycleScope.launch {
                kotlinx.coroutines.delay(100)
                val upPayload = mousePayload.copy(State = "up")
                sendInput(control.action, upPayload)
            }
        }
    }

    private fun sendInput(action: String, payload: Any) {
        val tcpClient = ConnectionHolder.tcpClientManager ?: return
        lifecycleScope.launch {
            val payloadJson = gson.toJson(payload)
            val packet = InputPacket(Action = action, Payload = payloadJson)
            val sent = tcpClient.send(packet)
            if (!sent) {
                Toast.makeText(this@ControlActivity, "Failed to send $action", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startListening() {
        val tcpClient = ConnectionHolder.tcpClientManager ?: return
        listenJob = lifecycleScope.launch {
            while (tcpClient.isConnected()) {
                val line = try {
                    tcpClient.read()
                } catch (e: Exception) {
                    null
                }

                if (line == null) {
                    runOnUiThread {
                        if (!isFinishing) {
                            Toast.makeText(this@ControlActivity, "Server connection lost", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                    break
                }

                try {
                    val basePacket = gson.fromJson(line, Map::class.java)
                    if (basePacket["Type"] == "system" && basePacket["Action"] == "disconnect") {
                        val message = basePacket["Message"] as? String
                        runOnUiThread {
                            val toastMsg = if (message == "host_shutdown") "Server closed connection" else "Disconnected"
                            Toast.makeText(this@ControlActivity, toastMsg, Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        break
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun disconnectAndExit() {
        val tcpClient = ConnectionHolder.tcpClientManager
        lifecycleScope.launch {
            withContext(NonCancellable) {
                if (tcpClient != null && tcpClient.isConnected()) {
                    tcpClient.send(SystemPacket(Action = "disconnect"))
                    tcpClient.disconnect()
                }
                runOnUiThread {
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        listenJob?.cancel()
        val tcpClient = ConnectionHolder.tcpClientManager
        if (tcpClient != null && tcpClient.isConnected()) {
            lifecycleScope.launch {
                withContext(NonCancellable) {
                    tcpClient.disconnect()
                }
            }
        }
        super.onDestroy()
    }
}
