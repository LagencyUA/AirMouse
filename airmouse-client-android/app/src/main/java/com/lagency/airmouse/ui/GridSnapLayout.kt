package com.lagency.airmouse.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.Button
import com.google.gson.Gson
import com.lagency.airmouse.models.ControlElement
import com.lagency.airmouse.models.ControlType
import com.lagency.airmouse.models.LayoutData
import kotlin.math.roundToInt

class GridSnapLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private val gson = Gson()
    private var layoutData: LayoutData? = null
    private var cellWidth: Float = 0f
    private var cellHeight: Float = 0f
    
    var isEditMode: Boolean = false
        set(value) {
            field = value
            if (!value) selectedChild = null
            invalidate()
        }

    private val gridPaint = Paint().apply {
        color = Color.GRAY
        alpha = 40
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    
    private val dotPaint = Paint().apply {
        color = Color.GRAY
        alpha = 80
        style = Paint.Style.FILL
    }

    private val handlePaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val handleSize = 12f * resources.displayMetrics.density
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var selectedChild: View? = null
    private var dragHandle: DragHandle = DragHandle.NONE
    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f
    private var startTouchX: Float = 0f
    private var startTouchY: Float = 0f
    
    // For smooth dragging
    private var tempX: Float = 0f
    private var tempY: Float = 0f
    private var tempW: Float = 0f
    private var tempH: Float = 0f

    private var gyroDownTime = 0L
    private val gyroLongPressTimeout = 200L

    private val mousePadHandler by lazy { MousePadHandler { onControlClick -> onControlClickWrapper?.invoke(onControlClick) } }
    private val gyroMouseHandler by lazy { 
        GyroMouseHandler(
            context = context,
            onActivationChanged = { activated ->
                // Find and update the gyro button visual state
                for (i in 0 until childCount) {
                    val child = getChildAt(i)
                    val control = child.tag as? ControlElement
                    if (control?.type == ControlType.GYRO_MOUSE) {
                        child.isActivated = activated
                    }
                }
            },
            onSendPacket = { action, payload ->
                onControlClickWrapper?.invoke(ControlElement(
                    id = "gyro_temp", name = "", x = 0, y = 0, width = 0, height = 0,
                    action = action, payload = payload
                ))
            }
        )
    }
    private val keyHandler by lazy {
        KeyHandler(
            onControlClick = { control -> onControlClickWrapper?.invoke(control) },
            onActivationChanged = { control, activated ->
                findViewWithTag<View>(control)?.isActivated = activated
            }
        )
    }

    private val keyboardHandler by lazy {
        KeyboardHandler(
            context = context,
            getActiveModifiers = { keyHandler.getActiveModifierKeys() },
            onSendPacket = { action, payload ->
                onControlClickWrapper?.invoke(ControlElement(
                    id = "kb_temp", name = "", x = 0, y = 0, width = 0, height = 0,
                    action = action, payload = payload
                ))
            }
        )
    }

    private var onControlClickWrapper: ((ControlElement) -> Unit)? = null

    var onSelectionChanged: ((ControlElement?) -> Unit)? = null

    enum class DragHandle { 
        NONE, TOP_LEFT, TOP_CENTER, TOP_RIGHT, 
        MIDDLE_LEFT, MIDDLE_RIGHT, 
        BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT, 
        BODY 
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        keyboardHandler.attachToView(this)
    }

    fun getSelectedControl(): ControlElement? = selectedChild?.tag as? ControlElement

    fun setLayout(data: LayoutData, onControlClick: (ControlElement) -> Unit) {
        keyboardHandler.hideKeyboard()
        val prevSelectedId = (selectedChild?.tag as? ControlElement)?.id
        this.layoutData = data
        this.onControlClickWrapper = onControlClick
        refreshViews(onControlClick)
        
        if (prevSelectedId != null) {
            reselectById(prevSelectedId)
        }
    }

    fun clearSelection() {
        selectedChild = null
        dragHandle = DragHandle.NONE
        invalidate()
        onSelectionChanged?.invoke(null)
    }

    fun reselectById(id: String) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val control = child.tag as? ControlElement
            if (control?.id == id) {
                selectedChild = child
                initTempVars(child)
                invalidate()
                break
            }
        }
    }

    fun resetModifiers() {
        keyHandler.resetModifiers()
    }

    private fun refreshViews(onControlClick: (ControlElement) -> Unit) {
        val data = layoutData ?: return
        removeAllViews()
        selectedChild = null
        
        // Re-attach keyboard handler's hidden field after clearing
        keyboardHandler.attachToView(this)
        
        data.controls.sortedBy { it.zIndex }.forEach { control ->
            val button = Button(context).apply {
                text = control.getDisplayName(gson)
                isAllCaps = false
                alpha = 0.9f
                elevation = (control.zIndex * 4f + 8f) * resources.displayMetrics.density
                
                // Set common background for all buttons
                setBackgroundResource(com.lagency.airmouse.R.drawable.btn_modifier_selector)
                
                setOnClickListener { 
                    if (!isEditMode) {
                        if (control.type == ControlType.KEYBOARD) {
                            keyboardHandler.showKeyboard()
                        } else {
                            onControlClick(control)
                        }
                    }
                }
                
                setOnTouchListener { v, event ->
                    if (isEditMode) return@setOnTouchListener false
                    
                    if (control.type == ControlType.MOUSE_PAD) {
                        mousePadHandler.handleTouch(control, event)
                        return@setOnTouchListener true
                    }

                    if (control.type == ControlType.GYRO_MOUSE) {
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                v.isPressed = true
                                gyroDownTime = System.currentTimeMillis()
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                v.isPressed = false
                                if (System.currentTimeMillis() - gyroDownTime >= gyroLongPressTimeout) {
                                    gyroMouseHandler.reset()
                                } else {
                                    gyroMouseHandler.toggle()
                                }
                                gyroDownTime = 0
                            }
                        }
                        return@setOnTouchListener true
                    }

                    if (control.type == ControlType.KEYBOARD) {
                        return@setOnTouchListener false
                    }
                    
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            v.isPressed = true
                            keyHandler.handleTouch(control, "down")
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.isPressed = false
                            keyHandler.handleTouch(control, "up")
                        }
                    }
                    true
                }

                tag = control
            }
            addView(button)
        }
        requestLayout()
        invalidate()
    }

    fun getLayoutData(): LayoutData? = layoutData

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!isEditMode) return super.onInterceptTouchEvent(ev)
        // Intercept everything in edit mode to handle selection/drag/resize
        return true
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!isEditMode) return super.onTouchEvent(ev)

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                startTouchX = ev.x
                startTouchY = ev.y
                lastTouchX = ev.x
                lastTouchY = ev.y
                
                // 1. Check if hit a handle of the already selected child
                selectedChild?.let { child ->
                    val handle = getHandleAt(child, ev.x, ev.y)
                    if (handle != DragHandle.NONE) {
                        dragHandle = handle
                        initTempVars(child)
                        return true
                    }
                }

                // 2. If not a handle, check if hit any child (for move or selection)
                val child = findChildAt(ev.x, ev.y)
                if (child != null && child.tag is ControlElement) {
                    selectedChild = child
                    dragHandle = DragHandle.BODY
                    initTempVars(child)
                    onSelectionChanged?.invoke(child.tag as ControlElement)
                } else {
                    selectedChild = null
                    dragHandle = DragHandle.NONE
                    onSelectionChanged?.invoke(null)
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (selectedChild == null || dragHandle == DragHandle.NONE) return false
                
                val dx = (ev.x - lastTouchX) / cellWidth
                val dy = (ev.y - lastTouchY) / cellHeight
                val control = selectedChild!!.tag as ControlElement

                when (dragHandle) {
                    DragHandle.BODY -> {
                        tempX += dx
                        tempY += dy
                        control.x = tempX.roundToInt().coerceIn(0, (layoutData?.gridWidth ?: 1) - control.width)
                        control.y = tempY.roundToInt().coerceIn(0, (layoutData?.gridHeight ?: 1) - control.height)
                    }
                    DragHandle.TOP_LEFT -> {
                        tempX += dx; tempY += dy; tempW -= dx; tempH -= dy
                        applyResize(control)
                    }
                    DragHandle.TOP_CENTER -> {
                        tempY += dy; tempH -= dy
                        applyResize(control)
                    }
                    DragHandle.TOP_RIGHT -> {
                        tempY += dy; tempW += dx; tempH -= dy
                        applyResize(control)
                    }
                    DragHandle.MIDDLE_LEFT -> {
                        tempX += dx; tempW -= dx
                        applyResize(control)
                    }
                    DragHandle.MIDDLE_RIGHT -> {
                        tempW += dx
                        applyResize(control)
                    }
                    DragHandle.BOTTOM_LEFT -> {
                        tempX += dx; tempW -= dx; tempH += dy
                        applyResize(control)
                    }
                    DragHandle.BOTTOM_CENTER -> {
                        tempH += dy
                        applyResize(control)
                    }
                    DragHandle.BOTTOM_RIGHT -> {
                        tempW += dx; tempH += dy
                        applyResize(control)
                    }
                    else -> {}
                }
                
                lastTouchX = ev.x
                lastTouchY = ev.y
                requestLayout()
                invalidate() // Force redraw of handles
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragHandle = DragHandle.NONE
            }
        }
        return true
    }

    private fun initTempVars(child: View) {
        val control = child.tag as ControlElement
        tempX = control.x.toFloat()
        tempY = control.y.toFloat()
        tempW = control.width.toFloat()
        tempH = control.height.toFloat()
    }

    private fun applyResize(control: ControlElement) {
        val newX = tempX.roundToInt().coerceAtLeast(0)
        val newY = tempY.roundToInt().coerceAtLeast(0)
        val newW = tempW.roundToInt().coerceAtLeast(1)
        val newH = tempH.roundToInt().coerceAtLeast(1)
        
        if (newW >= 1) {
            control.x = newX
            control.width = newW
        }
        if (newH >= 1) {
            control.y = newY
            control.height = newH
        }
    }

    private fun getHandleAt(child: View, x: Float, y: Float): DragHandle {
        val l = child.left.toFloat()
        val t = child.top.toFloat()
        val r = child.right.toFloat()
        val b = child.bottom.toFloat()
        val mw = (l + r) / 2
        val mh = (t + b) / 2
        val h = handleSize
        
        return when {
            isNear(x, y, l, t, h) -> DragHandle.TOP_LEFT
            isNear(x, y, mw, t, h) -> DragHandle.TOP_CENTER
            isNear(x, y, r, t, h) -> DragHandle.TOP_RIGHT
            isNear(x, y, l, mh, h) -> DragHandle.MIDDLE_LEFT
            isNear(x, y, r, mh, h) -> DragHandle.MIDDLE_RIGHT
            isNear(x, y, l, b, h) -> DragHandle.BOTTOM_LEFT
            isNear(x, y, mw, b, h) -> DragHandle.BOTTOM_CENTER
            isNear(x, y, r, b, h) -> DragHandle.BOTTOM_RIGHT
            else -> DragHandle.NONE
        }
    }

    private fun isNear(x: Float, y: Float, tx: Float, ty: Float, radius: Float): Boolean {
        return x >= tx - radius && x <= tx + radius && y >= ty - radius && y <= ty + radius
    }

    private fun findChildAt(x: Float, y: Float): View? {
        for (i in childCount - 1 downTo 0) {
            val child = getChildAt(i)
            if (child.tag !is ControlElement) continue
            if (x >= child.left && x <= child.right && y >= child.top && y <= child.bottom) {
                return child
            }
        }
        return null
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (isEditMode) drawGrid(canvas)
        super.dispatchDraw(canvas)
        if (isEditMode) {
            selectedChild?.let { drawHandles(canvas, it) }
        }
    }

    private fun drawHandles(canvas: Canvas, child: View) {
        val l = child.left.toFloat()
        val t = child.top.toFloat()
        val r = child.right.toFloat()
        val b = child.bottom.toFloat()
        val mw = (l + r) / 2
        val mh = (t + b) / 2
        val radius = handleSize / 2
        
        canvas.drawCircle(l, t, radius, handlePaint)
        canvas.drawCircle(mw, t, radius, handlePaint)
        canvas.drawCircle(r, t, radius, handlePaint)
        canvas.drawCircle(l, mh, radius, handlePaint)
        canvas.drawCircle(r, mh, radius, handlePaint)
        canvas.drawCircle(l, b, radius, handlePaint)
        canvas.drawCircle(mw, b, radius, handlePaint)
        canvas.drawCircle(r, b, radius, handlePaint)
    }

    private fun drawGrid(canvas: Canvas) {
        val gridW = layoutData?.gridWidth ?: 1
        val gridH = layoutData?.gridHeight ?: 1
        for (i in 0..gridW) {
            val x = i * cellWidth
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
        }
        for (i in 0..gridH) {
            val y = i * cellHeight
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
        }
        for (i in 0..gridW) {
            for (j in 0..gridH) {
                canvas.drawCircle(i * cellWidth, j * cellHeight, 3f, dotPaint)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val availableWidth = width - (paddingLeft + paddingRight)
        val availableHeight = height - (paddingTop + paddingBottom)
        cellWidth = availableWidth.toFloat() / (layoutData?.gridWidth ?: 1)
        cellHeight = availableHeight.toFloat() / (layoutData?.gridHeight ?: 1)

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val control = child.tag as? ControlElement
            if (control != null) {
                child.measure(
                    MeasureSpec.makeMeasureSpec((control.width * cellWidth).toInt(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec((control.height * cellHeight).toInt(), MeasureSpec.EXACTLY)
                )
            } else {
                // Measure other views (like the hidden EditText)
                child.measure(MeasureSpec.makeMeasureSpec(1, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(1, MeasureSpec.EXACTLY))
            }
        }
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val control = child.tag as? ControlElement
            if (control != null) {
                val left = paddingLeft + (control.x * cellWidth).toInt()
                val top = paddingTop + (control.y * cellHeight).toInt()
                child.layout(left, top, left + child.measuredWidth, top + child.measuredHeight)
            } else {
                // Layout other views at (0,0) with 1x1 size
                child.layout(0, 0, child.measuredWidth, child.measuredHeight)
            }
        }
    }
}
