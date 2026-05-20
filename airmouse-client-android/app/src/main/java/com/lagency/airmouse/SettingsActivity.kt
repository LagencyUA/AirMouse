package com.lagency.airmouse

import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.lagency.airmouse.data.LayoutManager
import com.lagency.airmouse.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var layoutManager: LayoutManager

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            try {
                contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(layoutManager.getExportJson().toByteArray())
                }
                Toast.makeText(this, "Layouts exported", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try {
                contentResolver.openInputStream(it)?.use { stream ->
                    val json = stream.bufferedReader().readText()
                    if (layoutManager.importFromJson(json)) {
                        Toast.makeText(this, "Layouts imported", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Invalid file format", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        layoutManager = LayoutManager.getInstance(this)

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnExportLayouts.setOnClickListener {
            exportLauncher.launch("airmouse_layouts.json")
        }

        binding.btnImportLayouts.setOnClickListener {
            importLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
        }

        binding.btnRestoreDefaults.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Restore Defaults")
                .setMessage("This will delete all your layouts and restore the defaults. Are you sure?")
                .setPositiveButton("Yes") { _, _ ->
                    layoutManager.restoreDefaults()
                    Toast.makeText(this, "Defaults restored", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("No", null)
                .show()
        }
    }
}
