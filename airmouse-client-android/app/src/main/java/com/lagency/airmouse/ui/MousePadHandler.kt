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
    private val dragHandler = Handler(Looper.getMainLooper())
    
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
                
                movedEnough = false
                primaryWasMoving = false
                secondFingerAssist = false
                
                touchMode = TouchMode.MOVE
                dragHandler.postDelayed(dragRunnable, 250)
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                dragHandler.removeCallbacks(dragRunnable)
                if (event.pointerCount == 2) {
                    secondFingerTapTime = System.currentTimeMillis()
                    if (primaryWasMoving) {
                        secondFingerAssist = true
                    } else {
                        lastScrollY = (event.getY(0) + event.getY(1)) / 2f
                        touchMode = TouchMode.SCROLL
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

                            if (kotlin.math.abs(dx) > moveThreshold || kotlin.math.abs(dy) > moveThreshold) {
                                movedEnough = true
                                primaryWasMoving = true
                                if (!dragPressed) {
                                    dragHandler.removeCallbacks(dragRunnable)
                                }
                                sendMouseMove(dx.toInt(), dy.toInt())
                                lastX = x
                                lastY = y
                            }
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
                        touchMode = TouchMode.NONE
                    }
                    
                    val remainIndex = if (event.actionIndex == 0) 1 else 0
                    lastX = event.getX(remainIndex)
                    lastY = event.getY(remainIndex)
                }
            }

            MotionEvent.ACTION_UP -> {
                dragHandler.removeCallbacks(dragRunnable)
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
                dragHandler.removeCallbacks(dragRunnable)
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
