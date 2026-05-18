package com.lagency.airmouse.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.lagency.airmouse.models.ControlElement
import com.lagency.airmouse.models.LayoutData

class GridSnapLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private var layoutData: LayoutData? = null
    private var cellWidth: Float = 0f
    private var cellHeight: Float = 0f

    fun setLayout(data: LayoutData, onControlClick: (ControlElement) -> Unit) {
        this.layoutData = data
        removeAllViews()
        
        data.controls.forEach { control ->
            val button = Button(context).apply {
                text = control.name
                setOnClickListener { onControlClick(control) }
                // Store control data in tag for layout pass
                tag = control
            }
            addView(button)
        }
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        
        val paddingHorizontal = paddingLeft + paddingRight
        val paddingVertical = paddingTop + paddingBottom
        
        val availableWidth = width - paddingHorizontal
        val availableHeight = height - paddingVertical

        val gridW = layoutData?.gridWidth ?: 1
        val gridH = layoutData?.gridHeight ?: 1
        
        cellWidth = availableWidth.toFloat() / gridW
        cellHeight = availableHeight.toFloat() / gridH

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val control = child.tag as? ControlElement ?: continue
            
            val childWidth = (control.width * cellWidth).toInt()
            val childHeight = (control.height * cellHeight).toInt()
            
            child.measure(
                MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY)
            )
        }
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val paddingL = paddingLeft
        val paddingT = paddingTop

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val control = child.tag as? ControlElement ?: continue
            
            val left = paddingL + (control.x * cellWidth).toInt()
            val top = paddingT + (control.y * cellHeight).toInt()
            val right = left + child.measuredWidth
            val bottom = top + child.measuredHeight
            
            child.layout(left, top, right, bottom)
        }
    }
}
