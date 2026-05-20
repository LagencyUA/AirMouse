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
import com.lagency.airmouse.models.LayoutData
import com.lagency.airmouse.network.ConnectionHolder
import com.lagency.airmouse.ui.ControlEditManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ControlActivity : AppCompatActivity() {
    private lateinit var binding: ActivityControlBinding
    private lateinit var layoutManager: LayoutManager
    private lateinit var editManager: ControlEditManager
    private val networkService = ConnectionHolder.networkService
    private val gson = Gson()
    private var listenJob: Job? = null
    
    private var currentLayoutName: String? = null
    private var isEditMode = false
    private var activeDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityControlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        layoutManager = LayoutManager.getInstance(this)
        editManager = ControlEditManager(
            binding = binding,
            onDelete = { control ->
                binding.layoutWorkingArea.getLayoutData()?.let { layout ->
                    binding.layoutWorkingArea.clearSelection()
                    layout.controls.remove(control)
                    // Just refresh the view, don't save to LayoutManager yet
                    binding.layoutWorkingArea.setLayout(layout) { handleControlClick(it) }
                }
            },
            onSave = { 
                binding.layoutWorkingArea.getLayoutData()?.let { layout ->
                    layout.controls.sortBy { it.zIndex }
                    // Just refresh the view, don't save to LayoutManager yet
                    binding.layoutWorkingArea.setLayout(layout) { handleControlClick(it) }
                }
            }
        )

        if (!networkService.isConnected()) {
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupHeader()
        setupLayoutWorkingArea()
        setupEditPanel()
        startListening()
        
        if (layoutManager.getAllLayoutNames().isEmpty()) {
            layoutManager.createDefaultLayouts()
        }
        refreshLayoutTabs()
        
        layoutManager.getAllLayoutNames().firstOrNull()?.let { switchLayout(it) }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isEditMode) showCancelEditDialog() else disconnectAndExit()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (!isEditMode) {
            val allNames = layoutManager.getAllLayoutNames()
            if (currentLayoutName == null || !allNames.contains(currentLayoutName)) {
                currentLayoutName = allNames.firstOrNull()
            }
            refreshLayoutTabs()
            currentLayoutName?.let { switchLayout(it) }
        }
    }

    private fun setupLayoutWorkingArea() {
        binding.layoutWorkingArea.onSelectionChanged = { control ->
            val hasSelection = control != null
            binding.btnEditElement.isEnabled = hasSelection
            binding.btnEditElement.alpha = if (hasSelection) 1.0f else 0.5f
            if (!hasSelection) editManager.hidePanel()
        }
    }

    private fun setupHeader() {
        binding.btnBack.setOnClickListener { 
            if (isEditMode) showCancelEditDialog() else disconnectAndExit()
        }
        binding.btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        binding.btnEditLayout.setOnClickListener { setEditMode(!isEditMode) }
        binding.btnAddLayout.setOnClickListener { showAddLayoutDialog() }

        binding.btnRenameLayout.setOnClickListener { showRenameLayoutDialog() }
        binding.btnAddElement.setOnClickListener { 
            binding.layoutWorkingArea.getLayoutData()?.let { layout ->
                val newControl = ControlElement(
                    id = "btn_${System.currentTimeMillis()}",
                    name = "",
                    x = 0, y = 0, width = 2, height = 2,
                    action = "key_press",
                    payload = gson.toJson(mapOf("Key" to "A"))
                )
                layout.controls.add(newControl)
                binding.layoutWorkingArea.setLayout(layout) { handleControlClick(it) }
                binding.layoutWorkingArea.reselectById(newControl.id)
            }
        }
        
        binding.btnEditElement.setOnClickListener {
            binding.layoutWorkingArea.getSelectedControl()?.let { editManager.showPanel(it) }
        }

        binding.btnDeleteLayout.setOnClickListener { showDeleteLayoutDialog() }
        binding.btnCancelEdit.setOnClickListener { showCancelEditDialog() }
        binding.btnSaveEdit.setOnClickListener { showSaveEditDialog() }
    }

    private fun setupEditPanel() {
        binding.btnCloseEditPanel.setOnClickListener { editManager.hidePanel() }
    }

    private fun setEditMode(enabled: Boolean) {
        isEditMode = enabled
        binding.layoutWorkingArea.isEditMode = enabled
        
        // Ensure we work on a fresh deep copy when entering edit mode,
        // and discard any unsaved visual changes when exiting.
        currentLayoutName?.let { switchLayout(it) }

        if (!enabled) editManager.hidePanel()
        
        binding.btnBack.visibility = if (enabled) View.GONE else View.VISIBLE
        binding.layoutsScrollView.visibility = if (enabled) View.GONE else View.VISIBLE
        binding.btnAddLayout.visibility = if (enabled) View.GONE else View.VISIBLE
        binding.btnSettings.visibility = if (enabled) View.GONE else View.VISIBLE
        binding.editToolsContainer.visibility = if (enabled) View.VISIBLE else View.GONE
        
        binding.btnEditLayout.isEnabled = !enabled
        binding.btnEditLayout.alpha = if (enabled) 0.5f else 1.0f
        binding.btnEditLayout.setColorFilter(if (enabled) getColor(android.R.color.holo_blue_light) else 0)
        if (!enabled) binding.btnEditLayout.clearColorFilter()
    }

    private fun refreshLayoutTabs() {
        binding.layoutsContainer.removeAllViews()
        layoutManager.getAllLayoutNames().forEach { name ->
            val btn = Button(this, null, androidx.appcompat.R.attr.buttonStyleSmall).apply {
                text = name
                isAllCaps = false
                layoutParams = LinearLayout.LayoutParams(-2, -2).apply { setMargins(8, 0, 8, 0) }
                alpha = if (name == currentLayoutName) 1.0f else 0.7f
                if (name == currentLayoutName) setBackgroundColor(getColor(android.R.color.holo_blue_light))
                setOnClickListener { switchLayout(name) }
            }
            binding.layoutsContainer.addView(btn)
        }
    }

    private fun switchLayout(name: String) {
        currentLayoutName = name
        layoutManager.loadLayout(name)?.let { layout ->
            binding.layoutWorkingArea.resetModifiers()
            binding.layoutWorkingArea.setLayout(layout) { control ->
                handleControlClick(control)
            }
        }
        refreshLayoutTabs()
    }

    private fun handleControlClick(control: ControlElement) {
        lifecycleScope.launch {
            networkService.sendInput(control.action, control.payload)
        }
    }

    private fun startListening() {
        listenJob = lifecycleScope.launch {
            networkService.messages.collect { line ->
                try {
                    val basePacket = gson.fromJson(line, Map::class.java)
                    if (basePacket["Type"] == "system" && basePacket["Action"] == "disconnect") {
                        val message = basePacket["Message"] as? String
                        val toastMsg = if (message == "host_shutdown") "Server closed connection" else "Disconnected"
                        Toast.makeText(this@ControlActivity, toastMsg, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } catch (e: Exception) { }
            }
        }
        lifecycleScope.launch { networkService.startListening() }
    }

    private fun showRenameLayoutDialog() {
        val oldName = currentLayoutName ?: return
        val input = EditText(this).apply { setText(oldName); selectAll() }
        activeDialog = AlertDialog.Builder(this)
            .setTitle("Rename Layout")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty() && layoutManager.renameLayout(oldName, newName)) {
                    currentLayoutName = newName
                    refreshLayoutTabs()
                }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showDeleteLayoutDialog() {
        val name = currentLayoutName ?: return
        activeDialog = AlertDialog.Builder(this)
            .setTitle("Delete Layout")
            .setMessage("Delete '$name'?")
            .setPositiveButton("Delete") { _, _ ->
                layoutManager.deleteLayout(name)
                val first = layoutManager.getAllLayoutNames().firstOrNull()
                if (first != null) switchLayout(first) else {
                    layoutManager.createDefaultLayouts()
                    switchLayout("Default Layout")
                }
                setEditMode(false)
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showCancelEditDialog() {
        activeDialog = AlertDialog.Builder(this)
            .setTitle("Cancel Changes")
            .setMessage("Discard all changes?")
            .setPositiveButton("Yes") { _, _ ->
                currentLayoutName?.let { switchLayout(it) }
                setEditMode(false)
            }
            .setNegativeButton("No", null).show()
    }

    private fun showSaveEditDialog() {
        activeDialog = AlertDialog.Builder(this)
            .setTitle("Save Changes")
            .setMessage("Save layout changes?")
            .setPositiveButton("Yes") { _, _ ->
                binding.layoutWorkingArea.getLayoutData()?.let { layoutManager.saveLayout(it) }
                setEditMode(false)
            }
            .setNegativeButton("No", null).show()
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
            .setNegativeButton("Cancel", null).show()
    }

    private fun disconnectAndExit() {
        lifecycleScope.launch {
            withContext(NonCancellable) {
                networkService.sendSystemAction("disconnect")
                networkService.disconnect()
                finish()
            }
        }
    }

    override fun onDestroy() {
        listenJob?.cancel()
        activeDialog?.dismiss()
        super.onDestroy()
    }
}
