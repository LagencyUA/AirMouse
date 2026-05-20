package com.lagency.airmouse.ui

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import com.google.gson.Gson
import com.lagency.airmouse.models.ControlElement
import com.lagency.airmouse.models.MousePayload

class MousePadHandler(
    private val onControlClick: (ControlElement) -> Unit
) {
    private val gson = Gson()
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var touchMode = TouchMode.NONE
    private var lastX = 0f
    private var lastY = 0f
    private var lastScrollY = 0f
    private var dragPressed = false
    private var tapDownTime = 0L
    private var secondFingerTapTime = 0L
    private var baseScrollFactor = 1.5f

    private var movedEnough = false
    private var primaryWasMoving = false
    private var secondFingerAssist = false

    private val moveThreshold = 8f
    private val scrollThreshold = 6f
    private val sendIntervalMs = 8L

    private var pendingDX = 0f
    private var pendingDY = 0f

    private enum class TouchMode {
        NONE, MOVE, DRAG, SCROLL
    }

    private val dragRunnable = Runnable {
        if (touchMode == TouchMode.MOVE && !movedEnough) {
            sendMouseButton("left", "down")
            dragPressed = true
            touchMode = TouchMode.DRAG
        }
    }

    private val sendMovementRunnable = object : Runnable {
        override fun run() {
            if (touchMode == TouchMode.MOVE || touchMode == TouchMode.DRAG) {
                val dx = pendingDX.toInt()
                val dy = pendingDY.toInt()
                
                if (dx != 0 || dy != 0) {
                    sendMouseMove(dx, dy)
                    pendingDX -= dx
                    pendingDY -= dy
                }
                mainHandler.postDelayed(this, sendIntervalMs)
            }
        }
    }

    private var currentControl: ControlElement? = null

    fun handleTouch(control: ControlElement, event: MotionEvent) {
        currentControl = control
        val sensitivity = control.sensitivity
        val scrollSensitivity = control.scrollSensitivity * baseScrollFactor

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                tapDownTime = System.currentTimeMillis()
                lastX = event.x
                lastY = event.y
                pendingDX = 0f
                pendingDY = 0f
                
                movedEnough = false
                primaryWasMoving = false
                secondFingerAssist = false
                
                touchMode = TouchMode.MOVE
                mainHandler.postDelayed(dragRunnable, 250)
                mainHandler.postDelayed(sendMovementRunnable, sendIntervalMs)
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                mainHandler.removeCallbacks(dragRunnable)
                if (event.pointerCount == 2) {
                    secondFingerTapTime = System.currentTimeMillis()
                    if (primaryWasMoving) {
                        secondFingerAssist = true
                    } else {
                        lastScrollY = (event.getY(0) + event.getY(1)) / 2f
                        touchMode = TouchMode.SCROLL
                        mainHandler.removeCallbacks(sendMovementRunnable)
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                when (touchMode) {
                    TouchMode.MOVE, TouchMode.DRAG -> {
                        if (event.pointerCount >= 1) {
                            val x = event.x
                            val y = event.y
                            val dx = (x - lastX) * sensitivity
                            val dy = (y - lastY) * sensitivity
                            
                            pendingDX += dx
                            pendingDY += dy

                            if (kotlin.math.abs(pendingDX) > moveThreshold || kotlin.math.abs(pendingDY) > moveThreshold) {
                                movedEnough = true
                                primaryWasMoving = true
                                if (!dragPressed) {
                                    mainHandler.removeCallbacks(dragRunnable)
                                }
                            }
                            lastX = x
                            lastY = y
                        }
                    }
                    TouchMode.SCROLL -> {
                        if (event.pointerCount >= 2) {
                            val centerY = (event.getY(0) + event.getY(1)) / 2f
                            val delta = (centerY - lastScrollY) * scrollSensitivity
                            
                            if (kotlin.math.abs(delta) > scrollThreshold) {
                                sendScroll(delta.toInt())
                                lastScrollY = centerY
                            }
                        }
                    }
                    else -> {}
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount == 2) {
                    val duration = System.currentTimeMillis() - secondFingerTapTime
                    if (secondFingerAssist) {
                        if (duration < 180) {
                            sendMouseButton("left", "click")
                        }
                        secondFingerAssist = false
                    } else if (touchMode == TouchMode.SCROLL) {
                        if (!movedEnough && duration < 180) {
                            sendMouseButton("right", "click")
                        }
                        touchMode = TouchMode.MOVE // Return to move mode
                        lastX = event.getX(if (event.actionIndex == 0) 1 else 0)
                        lastY = event.getY(if (event.actionIndex == 0) 1 else 0)
                        mainHandler.postDelayed(sendMovementRunnable, sendIntervalMs)
                    }
                    
                    val remainIndex = if (event.actionIndex == 0) 1 else 0
                    lastX = event.getX(remainIndex)
                    lastY = event.getY(remainIndex)
                }
            }

            MotionEvent.ACTION_UP -> {
                mainHandler.removeCallbacks(dragRunnable)
                mainHandler.removeCallbacks(sendMovementRunnable)
                val duration = System.currentTimeMillis() - tapDownTime

                if (!movedEnough && touchMode == TouchMode.MOVE && duration < 180) {
                    sendMouseButton("left", "click")
                }

                if (dragPressed) {
                    sendMouseButton("left", "up")
                    dragPressed = false
                }

                touchMode = TouchMode.NONE
            }
            
            MotionEvent.ACTION_CANCEL -> {
                mainHandler.removeCallbacks(dragRunnable)
                mainHandler.removeCallbacks(sendMovementRunnable)
                if (dragPressed) {
                    sendMouseButton("left", "up")
                    dragPressed = false
                }
                touchMode = TouchMode.NONE
            }
        }
    }

    private fun sendMouseMove(dx: Int, dy: Int) {
        val control = currentControl ?: return
        val payload = gson.toJson(MousePayload(DX = dx, DY = dy))
        onControlClick(control.copy(action = "mouse_move", payload = payload))
    }

    private fun sendMouseButton(button: String, state: String) {
        val control = currentControl ?: return
        val payload = gson.toJson(MousePayload(Button = button, State = state))
        onControlClick(control.copy(action = "mouse_button", payload = payload))
    }

    private fun sendScroll(delta: Int) {
        val control = currentControl ?: return
        val payload = gson.toJson(MousePayload(Scroll = delta))
        onControlClick(control.copy(action = "mouse_scroll", payload = payload))
    }
}
