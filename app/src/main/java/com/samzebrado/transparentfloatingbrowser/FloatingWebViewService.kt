package com.samzebrado.transparentfloatingbrowser

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast

enum class OverlayMode { EDIT, DISPLAY }

class FloatingWebViewService : Service() {

    companion object {
        private const val TAG = "FloatingWebViewService"
        private const val OVERLAY_WIDTH_DP = 320
        private const val OVERLAY_HEIGHT_DP = 220
        private const val EXTRA_URL = "url"
        private const val MIN_WIDTH_DP = 180
        private const val MIN_HEIGHT_DP = 120
        private const val PREFS_NAME = "FloatingWebViewPrefs"
        private const val KEY_OVERLAY_X = "overlay_x"
        private const val KEY_OVERLAY_Y = "overlay_y"
        private const val KEY_OVERLAY_WIDTH = "overlay_width"
        private const val KEY_OVERLAY_HEIGHT = "overlay_height"

        fun start(context: Context, url: String) {
            val intent = Intent(context, FloatingWebViewService::class.java).apply {
                putExtra(EXTRA_URL, url)
            }
            context.startService(intent)
        }
    }

    private var windowManager: WindowManager? = null
    private var webViewController: FloatingWebViewController? = null
    private var overlayView: View? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var controlBubble: OverlayControlBubble? = null
    private var controlBubbleView: View? = null
    private var controlBubbleParams: WindowManager.LayoutParams? = null
    private var currentMode: OverlayMode = OverlayMode.EDIT

    private var dragInitialX = 0
    private var dragInitialY = 0
    private var dragInitialTouchX = 0f
    private var dragInitialTouchY = 0f

    private var resizeInitialWidth = 0
    private var resizeInitialHeight = 0
    private var resizeInitialTouchX = 0f
    private var resizeInitialTouchY = 0f

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (overlayView == null) {
            val url = intent?.getStringExtra(EXTRA_URL) ?: "https://SamZebrado.github.io/NewMarsHeartBeat/?transparent=1"
            addOverlayView(url)
        }
        return START_NOT_STICKY
    }

    private fun addOverlayView(url: String) {
        try {
            if (windowManager == null) {
                windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            }

            webViewController = FloatingWebViewController(this)
            overlayView = webViewController?.createView()
            webViewController?.setEditHandlesVisible(currentMode == OverlayMode.EDIT)
            setupOverlayParams()
            setupTouchListeners()

            windowManager?.addView(overlayView, overlayParams)
            webViewController?.loadUrl(url)

            addControlBubble()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay", e)
            Toast.makeText(this, "Failed to show overlay", Toast.LENGTH_SHORT).show()
            cleanupOnFailure()
        }
    }

    private fun setupOverlayParams() {
        val density = resources.displayMetrics.density
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val defaultWidth = (OVERLAY_WIDTH_DP * density).toInt()
        val defaultHeight = (OVERLAY_HEIGHT_DP * density).toInt()
        val defaultX = (100 * density).toInt()
        val defaultY = (100 * density).toInt()

        val width = prefs.getInt(KEY_OVERLAY_WIDTH, defaultWidth)
        val height = prefs.getInt(KEY_OVERLAY_HEIGHT, defaultHeight)
        val x = prefs.getInt(KEY_OVERLAY_X, defaultX)
        val y = prefs.getInt(KEY_OVERLAY_Y, defaultY)

        overlayParams = WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        overlayParams?.gravity = Gravity.TOP or Gravity.START
        overlayParams?.x = x
        overlayParams?.y = y
    }

    private fun setupTouchListeners() {
        val dragHandle = webViewController?.getDragHandleView()
        val resizeHandle = webViewController?.getResizeHandleView()

        dragHandle?.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(view: View, event: MotionEvent): Boolean {
                val params = overlayParams ?: return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        dragInitialX = params.x
                        dragInitialY = params.y
                        dragInitialTouchX = event.rawX
                        dragInitialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = dragInitialX + (event.rawX - dragInitialTouchX).toInt()
                        params.y = dragInitialY + (event.rawY - dragInitialTouchY).toInt()
                        windowManager?.updateViewLayout(overlayView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        savePosition()
                        return true
                    }
                }
                return false
            }
        })

        resizeHandle?.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(view: View, event: MotionEvent): Boolean {
                val params = overlayParams ?: return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        resizeInitialWidth = params.width
                        resizeInitialHeight = params.height
                        resizeInitialTouchX = event.rawX
                        resizeInitialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val density = resources.displayMetrics.density
                        val minWidth = (MIN_WIDTH_DP * density).toInt()
                        val minHeight = (MIN_HEIGHT_DP * density).toInt()

                        val deltaX = (event.rawX - resizeInitialTouchX).toInt()
                        val deltaY = (event.rawY - resizeInitialTouchY).toInt()

                        val newWidth = (resizeInitialWidth + deltaX).coerceAtLeast(minWidth)
                        val newHeight = (resizeInitialHeight + deltaY).coerceAtLeast(minHeight)

                        params.width = newWidth
                        params.height = newHeight
                        windowManager?.updateViewLayout(overlayView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        saveSize()
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun addControlBubble() {
        try {
            controlBubble = OverlayControlBubble(
                context = this,
                onToggleMode = { toggleMode() },
                onClose = { stopSelf() }
            )
            controlBubbleView = controlBubble?.createView()
            controlBubbleParams = controlBubble?.getParams()
            controlBubble?.setMode(currentMode)

            windowManager?.addView(controlBubbleView, controlBubbleParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add control bubble", e)
            controlBubble = null
            controlBubbleView = null
            controlBubbleParams = null
        }
    }

    private fun toggleMode() {
        currentMode = when (currentMode) {
            OverlayMode.EDIT -> OverlayMode.DISPLAY
            OverlayMode.DISPLAY -> OverlayMode.EDIT
        }
        applyModeToWebOverlay()
        controlBubble?.setMode(currentMode)
    }

    private fun applyModeToWebOverlay() {
        val params = overlayParams ?: return
        params.flags = when (currentMode) {
            OverlayMode.EDIT -> WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            OverlayMode.DISPLAY -> WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }

        webViewController?.setEditHandlesVisible(currentMode == OverlayMode.EDIT)

        try {
            windowManager?.updateViewLayout(overlayView, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update overlay mode", e)
        }
    }

    private fun savePosition() {
        val params = overlayParams ?: return
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt(KEY_OVERLAY_X, params.x)
            putInt(KEY_OVERLAY_Y, params.y)
            apply()
        }
    }

    private fun saveSize() {
        val params = overlayParams ?: return
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt(KEY_OVERLAY_WIDTH, params.width)
            putInt(KEY_OVERLAY_HEIGHT, params.height)
            apply()
        }
    }

    private fun removeOverlayView() {
        if (controlBubbleView != null) {
            try {
                windowManager?.removeView(controlBubbleView)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove control bubble", e)
            }
            controlBubbleView = null
            controlBubbleParams = null
            controlBubble = null
        }

        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove overlay", e)
            }
            overlayView = null
            overlayParams = null
        }

        webViewController?.destroy()
        webViewController = null
    }

    private fun cleanupOnFailure() {
        if (controlBubbleView != null) {
            try {
                windowManager?.removeView(controlBubbleView)
            } catch (e: Exception) {
                Log.w(TAG, "Control bubble not attached or already removed during cleanup", e)
            }
            controlBubbleView = null
            controlBubbleParams = null
            controlBubble = null
        }

        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                Log.w(TAG, "Overlay not attached or already removed during cleanup", e)
            }
            overlayView = null
            overlayParams = null
        }

        webViewController?.destroy()
        webViewController = null
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlayView()
        windowManager = null
    }
}
