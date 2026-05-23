package com.samzebrado.transparentfloatingbrowser

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout

class OverlayControlBubble(
    private val context: Context,
    private val onToggleMode: () -> Unit,
    private val onClose: () -> Unit
) {

    private var bubbleView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var buttonText: Button? = null
    private var windowManager: WindowManager? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private var onPositionChanged: ((Int, Int) -> Unit)? = null

    private val touchSlopPx: Float by lazy {
        8f * context.resources.displayMetrics.density
    }

    fun setOnPositionChangedListener(listener: (Int, Int) -> Unit) {
        onPositionChanged = listener
    }

    fun createView(sizePx: Int): View {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val container = FrameLayout(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }

        buttonText = Button(context).apply {
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
            text = context.getString(R.string.mode_edit)
            setPadding(8, 8, 8, 8)
            setBackgroundColor(Color.parseColor("#FF5722"))
            setTextColor(Color.WHITE)
            textSize = 12f

            var isDragging = false
            var isLongPress = false
            var longPressRunnable: Runnable? = null

            setOnTouchListener(object : View.OnTouchListener {
                override fun onTouch(view: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = bubbleParams?.x ?: 0
                            initialY = bubbleParams?.y ?: 0
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            isDragging = false
                            isLongPress = false

                            longPressRunnable = Runnable {
                                isLongPress = true
                                onClose()
                            }
                            view.postDelayed(longPressRunnable, 500)
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val deltaX = event.rawX - initialTouchX
                            val deltaY = event.rawY - initialTouchY
                            if (!isDragging && (kotlin.math.abs(deltaX) > touchSlopPx || kotlin.math.abs(deltaY) > touchSlopPx)) {
                                isDragging = true
                                longPressRunnable?.let { view.removeCallbacks(it) }
                            }
                            if (isDragging) {
                                val newX = initialX + deltaX.toInt()
                                val newY = initialY + deltaY.toInt()
                                bubbleParams?.x = newX
                                bubbleParams?.y = newY

                                val viewToUpdate = bubbleView
                                val paramsToUpdate = bubbleParams
                                if (viewToUpdate != null && paramsToUpdate != null) {
                                    try {
                                        windowManager?.updateViewLayout(viewToUpdate, paramsToUpdate)
                                    } catch (e: Exception) {
                                        Log.w("OverlayControlBubble", "Failed to update bubble position", e)
                                    }
                                }

                                onPositionChanged?.invoke(newX, newY)
                            }
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            longPressRunnable?.let { view.removeCallbacks(it) }
                            if (!isDragging && !isLongPress) {
                                onToggleMode()
                            }
                            return true
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            longPressRunnable?.let { view.removeCallbacks(it) }
                            return true
                        }
                    }
                    return false
                }
            })
        }

        container.addView(buttonText)
        bubbleView = container
        return container
    }

    fun getParams(x: Int, y: Int): WindowManager.LayoutParams {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = x
        params.y = y
        bubbleParams = params
        return params
    }

    fun setMode(mode: OverlayMode) {
        val editMode = context.getString(R.string.mode_edit)
        val displayMode = context.getString(R.string.mode_display)
        buttonText?.text = when (mode) {
            OverlayMode.EDIT -> editMode
            OverlayMode.DISPLAY -> displayMode
        }
    }

    fun updateButtonSize(sizePx: Int) {
        buttonText?.layoutParams = buttonText?.layoutParams?.apply {
            width = sizePx
            height = sizePx
        }
        buttonText?.requestLayout()
        bubbleView?.requestLayout()
    }

    fun getView(): View? = bubbleView
}
