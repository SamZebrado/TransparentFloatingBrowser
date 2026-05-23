package com.samzebrado.transparentfloatingbrowser

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
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

    fun createView(): View {
        val container = FrameLayout(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }

        buttonText = Button(context).apply {
            text = "EDIT"
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.parseColor("#FF5722"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                onToggleMode()
            }
            setOnLongClickListener {
                onClose()
                true
            }
        }

        container.addView(
            buttonText,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )

        bubbleView = container
        return container
    }

    fun getParams(): WindowManager.LayoutParams {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 20
        params.y = 300
        bubbleParams = params
        return params
    }

    fun setMode(mode: OverlayMode) {
        buttonText?.text = when (mode) {
            OverlayMode.EDIT -> "EDIT"
            OverlayMode.DISPLAY -> "VIEW"
        }
    }

    fun getView(): View? = bubbleView
}
