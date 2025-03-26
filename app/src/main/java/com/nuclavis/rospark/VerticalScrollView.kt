package com.nuclavis.rospark

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView
import android.view.MotionEvent

class VerticalScrollView(context: Context?, attrs: AttributeSet?) : ScrollView(context, attrs) {
    private var xDistance = 0f
    private var yDistance = 0f
    private var lastX = 0f
    private var lastY = 0f
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                run {
                    yDistance = 0f
                    xDistance = yDistance
                }
                lastX = ev.x
                lastY = ev.y
            }
            MotionEvent.ACTION_MOVE -> {
                val curX = ev.x
                val curY = ev.y
                xDistance += Math.abs(curX - lastX)
                yDistance += Math.abs(curY - lastY)
                lastX = curX
                lastY = curY
                if (xDistance > yDistance) return false
            }
        }
        return super.onInterceptTouchEvent(ev)
    }
}