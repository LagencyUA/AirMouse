package com.lagency.airmouse.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.gson.Gson
import com.lagency.airmouse.models.ControlElement
import com.lagency.airmouse.models.MousePayload

class GyroMouseHandler(
    private val context: Context,
    private val onActivationChanged: (Boolean) -> Unit,
    private val onSendPacket: (String, String) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val gson = Gson()
    
    private var isEnabled = false
    private var sensitivity = 1.0f
    
    private var accumDX = 0f
    private var accumDY = 0f
    private val sendHandler = Handler(Looper.getMainLooper())
    private val sendInterval = 8L
    
    private var lastTimestamp = 0L

    private val sendRunnable = object : Runnable {
        override fun run() {
            if (accumDX != 0f || accumDY != 0f) {
                val dx = (accumDX * sensitivity * 50).toInt()
                val dy = (accumDY * sensitivity * 50).toInt()
                if (dx != 0 || dy != 0) {
                    val payload = MousePayload(DX = dx, DY = dy)
                    onSendPacket("mouse_move", gson.toJson(payload))
                }
                accumDX = 0f
                accumDY = 0f
            }
            if (isEnabled) {
                sendHandler.postDelayed(this, sendInterval)
            }
        }
    }

    fun handleTouch(control: ControlElement, state: String) {
        sensitivity = control.sensitivity
        
        if (state == "down") {
            // Check for long press for reset is handled by the caller or we can do it here
        } else if (state == "up") {
            toggle()
        }
    }

    fun toggle() {
        if (isEnabled) {
            stop()
        } else {
            start()
        }
    }

    fun reset() {
        Toast.makeText(context, "Gyro Position Reset", Toast.LENGTH_SHORT).show()
        accumDX = 0f
        accumDY = 0f
    }

    private fun start() {
        if (isEnabled) return
        isEnabled = true
        onActivationChanged(true)
        lastTimestamp = 0L
        sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME)
        sendHandler.post(sendRunnable)
    }

    private fun stop() {
        if (!isEnabled) return
        isEnabled = false
        onActivationChanged(false)
        sensorManager.unregisterListener(this)
        sendHandler.removeCallbacks(sendRunnable)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isEnabled) return
        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            if (lastTimestamp != 0L) {
                // event.values[0] -> Rotation around X (Tilt screen Up/Down)
                // event.values[1] -> Rotation around Y (Tilt towards long sides/volume buttons)
                // event.values[2] -> Rotation around Z (Steering wheel motion)

                // Vertical: Tilt screen up/down (X axis)
                val dy = -event.values[0] 
                
                // Horizontal: "Steering wheel" motion (Z axis)
                // Rotating the phone like a steering wheel moves the mouse horizontally
                val dx = -event.values[2]
                
                accumDX += dx
                accumDY += dy
            }
            lastTimestamp = event.timestamp
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
