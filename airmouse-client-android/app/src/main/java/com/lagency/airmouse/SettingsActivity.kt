package com.lagency.airmouse

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.lagency.airmouse.data.LayoutManager
import com.lagency.airmouse.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var layoutManager: LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        layoutManager = LayoutManager(this)

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
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
