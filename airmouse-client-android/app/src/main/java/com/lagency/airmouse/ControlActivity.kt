package com.lagency.airmouse

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lagency.airmouse.databinding.ActivityControlBinding
import com.lagency.airmouse.models.CommandPacket
import com.lagency.airmouse.network.ConnectionHolder
import kotlinx.coroutines.launch

class ControlActivity : AppCompatActivity() {
    private lateinit var binding: ActivityControlBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityControlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val tcpClient = ConnectionHolder.tcpClientManager

        if (tcpClient == null || !tcpClient.isConnected()) {
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnSendTest.setOnClickListener {
            lifecycleScope.launch {
                val packet = CommandPacket(Button = "test")
                val sent = tcpClient.send(packet)
                if (sent) {
                    Toast.makeText(this@ControlActivity, "Test packet sent", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ControlActivity, "Failed to send", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Якщо це остання Activity, можна закрити з'єднання
        // Але зазвичай краще мати кнопку "Disconnect"
    }
}
