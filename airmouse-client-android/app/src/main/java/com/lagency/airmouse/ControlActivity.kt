package com.lagency.airmouse

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.lagency.airmouse.data.LayoutManager
import com.lagency.airmouse.databinding.ActivityControlBinding
import com.lagency.airmouse.models.ControlElement
import com.lagency.airmouse.models.InputPacket
import com.lagency.airmouse.models.LayoutData
import com.lagency.airmouse.models.SystemPacket
import com.lagency.airmouse.network.ConnectionHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ControlActivity : AppCompatActivity() {
    private lateinit var binding: ActivityControlBinding
    private lateinit var layoutManager: LayoutManager
    private val gson = Gson()
    private var listenJob: Job? = null
    
    private var currentLayoutName: String? = null
    private var isEditMode = false
    private var activeDialog: AlertDialog? = null

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
        setupEditPanel()
        setupLayoutWorkingArea()
        startListening()
        
        if (layoutManager.getAllLayoutNames().isEmpty()) {
            layoutManager.createDefaultTestLayout()
        }
        refreshLayoutTabs()
        
        layoutManager.getAllLayoutNames().firstOrNull()?.let { switchLayout(it) }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isEditMode) showCancelEditDialog() else disconnectAndExit()
            }
        })
    }

    private fun setupLayoutWorkingArea() {
        binding.layoutWorkingArea.onSelectionChanged = { control ->
            val hasSelection = control != null
            binding.btnEditElement.isEnabled = hasSelection
            binding.btnEditElement.alpha = if (hasSelection) 1.0f else 0.5f
            if (!hasSelection) binding.buttonEditPanel.visibility = View.GONE
        }
    }

    private fun setupHeader() {
        binding.btnBack.setOnClickListener { disconnectAndExit() }
        binding.btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        binding.btnEditLayout.setOnClickListener { setEditMode(!isEditMode) }
        binding.btnAddLayout.setOnClickListener { showAddLayoutDialog() }

        binding.btnRenameLayout.setOnClickListener { showRenameLayoutDialog() }
        binding.btnAddElement.setOnClickListener { Toast.makeText(this, "Add block coming soon", Toast.LENGTH_SHORT).show() }
        binding.btnEditElement.setOnClickListener {
            binding.layoutWorkingArea.getSelectedControl()?.let { showButtonEditPanel(it) }
        }
        binding.btnDeleteLayout.setOnClickListener { showDeleteLayoutDialog() }
        binding.btnCancelEdit.setOnClickListener { showCancelEditDialog() }
        binding.btnSaveEdit.setOnClickListener { showSaveEditDialog() }
    }

    private fun setupEditPanel() {
        binding.btnCloseEditPanel.setOnClickListener {
            binding.buttonEditPanel.visibility = View.GONE
        }
    }

    private fun setEditMode(enabled: Boolean) {
        isEditMode = enabled
        binding.layoutWorkingArea.isEditMode = enabled
        if (!enabled) binding.buttonEditPanel.visibility = View.GONE
        
        binding.btnBack.visibility = if (enabled) View.GONE else View.VISIBLE
        binding.layoutsScrollView.visibility = if (enabled) View.GONE else View.VISIBLE
        binding.btnAddLayout.visibility = if (enabled) View.GONE else View.VISIBLE
        binding.btnSettings.visibility = if (enabled) View.GONE else View.VISIBLE
        binding.editToolsContainer.visibility = if (enabled) View.VISIBLE else View.GONE
        
        if (enabled) {
            binding.btnEditLayout.setColorFilter(getColor(android.R.color.holo_blue_light))
        } else {
            binding.btnEditLayout.clearColorFilter()
        }
    }

    private fun refreshLayoutTabs() {
        binding.layoutsContainer.removeAllViews()
        layoutManager.getAllLayoutNames().forEach { name ->
            val btn = Button(this, null, androidx.appcompat.R.attr.buttonStyleSmall).apply {
                text = name
                isAllCaps = false
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(8, 0, 8, 0) }
                
                if (name == currentLayoutName) {
                    alpha = 1.0f
                    setBackgroundColor(getColor(android.R.color.holo_blue_light))
                } else {
                    alpha = 0.7f
                }
                setOnClickListener { switchLayout(name) }
            }
            binding.layoutsContainer.addView(btn)
        }
    }

    private fun switchLayout(name: String) {
        currentLayoutName = name
        layoutManager.loadLayout(name)?.let { layout ->
            binding.layoutWorkingArea.setLayout(
                layout,
                onControlClick = { handleControlClick(it) }
            )
        }
        refreshLayoutTabs()
    }

    private fun showButtonEditPanel(control: ControlElement) {
        binding.buttonEditPanel.visibility = View.VISIBLE
        // Future: populate panel with control data
    }

    private fun handleControlClick(control: ControlElement) {
        sendInput(control.action, control.payload)
        
        if (control.action == "mouse_button" && control.payload.contains("\"State\":\"down\"")) {
            lifecycleScope.launch {
                delay(100)
                val upPayload = control.payload.replace("\"State\":\"down\"", "\"State\":\"up\"")
                sendInput(control.action, upPayload)
            }
        }
    }

    private fun sendInput(action: String, payloadJson: String) {
        val tcpClient = ConnectionHolder.tcpClientManager ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            val packet = InputPacket(Action = action, Payload = payloadJson)
            val sent = tcpClient.send(packet)
            if (!sent) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ControlActivity, "Failed to send $action", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startListening() {
        val tcpClient = ConnectionHolder.tcpClientManager ?: return
        listenJob = lifecycleScope.launch(Dispatchers.IO) {
            while (tcpClient.isConnected()) {
                val line = try { tcpClient.read() } catch (e: Exception) { null }

                if (line == null) {
                    withContext(Dispatchers.Main) {
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
                        withContext(Dispatchers.Main) {
                            val toastMsg = if (message == "host_shutdown") "Server closed connection" else "Disconnected"
                            Toast.makeText(this@ControlActivity, toastMsg, Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        break
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    private fun showRenameLayoutDialog() {
        val oldName = currentLayoutName ?: return
        val input = EditText(this).apply {
            setText(oldName)
            selectAll()
        }
        activeDialog = AlertDialog.Builder(this)
            .setTitle("Rename Layout")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty() && newName != oldName) {
                    if (layoutManager.renameLayout(oldName, newName)) {
                        currentLayoutName = newName
                        refreshLayoutTabs()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteLayoutDialog() {
        val name = currentLayoutName ?: return
        activeDialog = AlertDialog.Builder(this)
            .setTitle("Delete Layout")
            .setMessage("Are you sure you want to delete '$name'?")
            .setPositiveButton("Delete") { _, _ ->
                layoutManager.deleteLayout(name)
                currentLayoutName = layoutManager.getAllLayoutNames().firstOrNull()
                if (currentLayoutName != null) switchLayout(currentLayoutName!!)
                else {
                    layoutManager.createDefaultTestLayout()
                    switchLayout("Default Layout")
                }
                setEditMode(false)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCancelEditDialog() {
        activeDialog = AlertDialog.Builder(this)
            .setTitle("Cancel Changes")
            .setMessage("Discard all changes?")
            .setPositiveButton("Yes") { _, _ ->
                currentLayoutName?.let { switchLayout(it) }
                setEditMode(false)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showSaveEditDialog() {
        activeDialog = AlertDialog.Builder(this)
            .setTitle("Save Changes")
            .setMessage("Save layout changes?")
            .setPositiveButton("Yes") { _, _ ->
                binding.layoutWorkingArea.getLayoutData()?.let { layoutManager.saveLayout(it) }
                setEditMode(false)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showAddLayoutDialog() {
        val input = EditText(this).apply { hint = "New Layout Name" }
        activeDialog = AlertDialog.Builder(this)
            .setTitle("New Layout")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    layoutManager.saveLayout(LayoutData(name = name))
                    switchLayout(name)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun disconnectAndExit() {
        val tcpClient = ConnectionHolder.tcpClientManager
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(NonCancellable) {
                if (tcpClient != null && tcpClient.isConnected()) {
                    tcpClient.send(SystemPacket(Action = "disconnect"))
                    tcpClient.disconnect()
                }
                withContext(Dispatchers.Main) { finish() }
            }
        }
    }

    override fun onDestroy() {
        listenJob?.cancel()
        activeDialog?.dismiss()
        val tcpClient = ConnectionHolder.tcpClientManager
        if (tcpClient != null && tcpClient.isConnected()) {
            lifecycleScope.launch(Dispatchers.IO) {
                withContext(NonCancellable) { tcpClient.disconnect() }
            }
        }
        super.onDestroy()
    }
}
