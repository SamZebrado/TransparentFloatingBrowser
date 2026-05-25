package com.samzebrado.transparentfloatingbrowser

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams.MATCH_PARENT

class FloatingWebViewController(
    private val context: Context
) {
    companion object {
        private const val TAG = "FloatingWebViewController"
    }

    private var webView: WebView? = null
    private var containerView: FrameLayout? = null
    private var dragHandle: View? = null
    private var resizeHandle: View? = null

    private var onPositionChangedListener: ((Int, Int) -> Unit)? = null
    private var onSizeChangedListener: ((Int, Int) -> Unit)? = null
    private var onPositionChangeFinishedListener: (() -> Unit)? = null
    private var onSizeChangeFinishedListener: (() -> Unit)? = null
    private var currentMode: OverlayMode = OverlayMode.EDIT

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var initialWidth = 0
    private var initialHeight = 0

    private val touchSlopPx: Float by lazy {
        8f * context.resources.displayMetrics.density
    }

    private var isDragging = false
    private var isResizing = false

    fun setMode(mode: OverlayMode) {
        currentMode = mode
    }

    fun setOnPositionChangedListener(listener: (Int, Int) -> Unit) {
        onPositionChangedListener = listener
    }

    fun setOnSizeChangedListener(listener: (Int, Int) -> Unit) {
        onSizeChangedListener = listener
    }

    fun setOnPositionChangeFinishedListener(listener: () -> Unit) {
        onPositionChangeFinishedListener = listener
    }

    fun setOnSizeChangeFinishedListener(listener: () -> Unit) {
        onSizeChangeFinishedListener = listener
    }

    private fun isZoomEnabled(): Boolean {
        val prefs = context.getSharedPreferences(AppPrefs.MAIN_PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(
            AppPrefs.KEY_ENABLE_WEBVIEW_ZOOM,
            AppPrefs.DEFAULT_ENABLE_WEBVIEW_ZOOM
        )
    }

    private fun getTransparentColors(): List<String> {
        val prefs = context.getSharedPreferences(AppPrefs.MAIN_PREFS, Context.MODE_PRIVATE)
        val colorsString = prefs.getString(
            AppPrefs.KEY_TRANSPARENT_COLORS,
            AppPrefs.DEFAULT_TRANSPARENT_COLORS
        ) ?: AppPrefs.DEFAULT_TRANSPARENT_COLORS

        return colorsString.split(",", " ", "\n", ";")
            .map { it.trim() }
            .filter { it.matches(Regex("^#?[0-9a-fA-F]{6}$")) }
            .map { if (it.startsWith("#")) it.uppercase() else "#${it.uppercase()}" }
            .distinct()
            .ifEmpty { listOf(AppPrefs.DEFAULT_TRANSPARENT_COLORS) }
    }

    fun createView(): View {
        containerView = FrameLayout(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }

        val zoomEnabled = isZoomEnabled()

        webView = WebView(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false

            settings.setSupportZoom(zoomEnabled)
            settings.builtInZoomControls = zoomEnabled
            settings.displayZoomControls = false

            if (zoomEnabled) {
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
            }

            settings.setSupportMultipleWindows(false)

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "onPageFinished: $url, zoomEnabled: $zoomEnabled")
                    if (view != null) {
                        val colorKeys = getTransparentColors()
                        TransparentStyleInjector.inject(view, colorKeys) { result ->
                            Log.d(TAG, "Transparent injection result: $result")
                        }
                    }
                }
            }
        }

        dragHandle = View(context).apply {
            val density = resources.displayMetrics.density
            val prefs = context.getSharedPreferences(AppPrefs.MAIN_PREFS, Context.MODE_PRIVATE)
            val sizeDp = prefs.getInt(AppPrefs.KEY_HANDLE_SIZE_DP, AppPrefs.DEFAULT_HANDLE_SIZE_DP)
            val heightPx = (sizeDp * density).toInt()
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, heightPx).apply {
                gravity = android.view.Gravity.TOP
            }
            setBackgroundColor(Color.parseColor("#6633B5E5"))

            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        lastTouchX = event.rawX
                        lastTouchY = event.rawY
                        isDragging = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val totalDeltaX = event.rawX - initialTouchX
                        val totalDeltaY = event.rawY - initialTouchY

                        if (!isDragging && (kotlin.math.abs(totalDeltaX) > touchSlopPx || kotlin.math.abs(totalDeltaY) > touchSlopPx)) {
                            isDragging = true
                        }

                        if (isDragging) {
                            val frameDeltaX = event.rawX - lastTouchX
                            val frameDeltaY = event.rawY - lastTouchY
                            lastTouchX = event.rawX
                            lastTouchY = event.rawY
                            onPositionChangedListener?.invoke(frameDeltaX.toInt(), frameDeltaY.toInt())
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        val wasDragging = isDragging
                        isDragging = false
                        if (wasDragging) {
                            onPositionChangeFinishedListener?.invoke()
                        }
                        true
                    }
                    else -> false
                }
            }
        }

        resizeHandle = View(context).apply {
            val density = resources.displayMetrics.density
            val prefs = context.getSharedPreferences(AppPrefs.MAIN_PREFS, Context.MODE_PRIVATE)
            val sizeDp = prefs.getInt(AppPrefs.KEY_HANDLE_SIZE_DP, AppPrefs.DEFAULT_HANDLE_SIZE_DP)
            val sizePx = (sizeDp * density).toInt()
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx).apply {
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
            }
            setBackgroundColor(Color.parseColor("#66FF9800"))

            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        lastTouchX = event.rawX
                        lastTouchY = event.rawY
                        isResizing = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val totalDeltaX = event.rawX - initialTouchX
                        val totalDeltaY = event.rawY - initialTouchY

                        if (!isResizing && (kotlin.math.abs(totalDeltaX) > touchSlopPx || kotlin.math.abs(totalDeltaY) > touchSlopPx)) {
                            isResizing = true
                        }

                        if (isResizing) {
                            val frameDeltaX = event.rawX - lastTouchX
                            val frameDeltaY = event.rawY - lastTouchY
                            lastTouchX = event.rawX
                            lastTouchY = event.rawY
                            onSizeChangedListener?.invoke(frameDeltaX.toInt(), frameDeltaY.toInt())
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        val wasResizing = isResizing
                        isResizing = false
                        if (wasResizing) {
                            onSizeChangeFinishedListener?.invoke()
                        }
                        true
                    }
                    else -> false
                }
            }
        }

        containerView?.apply {
            addView(webView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(dragHandle)
            addView(resizeHandle)
        }

        return containerView as View
    }

    fun loadUrl(url: String) {
        webView?.loadUrl(url)
    }

    fun getView(): View? = containerView

    fun getDragHandleView(): View? = dragHandle

    fun getResizeHandleView(): View? = resizeHandle

    fun setEditHandlesVisible(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        dragHandle?.visibility = visibility
        resizeHandle?.visibility = visibility
    }

    fun setInitialPosition(x: Int, y: Int) {
        initialX = x
        initialY = y
    }

    fun setInitialSize(width: Int, height: Int) {
        initialWidth = width
        initialHeight = height
    }

    fun destroy() {
        webView?.let { wv ->
            wv.stopLoading()
            wv.loadUrl("about:blank")
            wv.clearHistory()
            wv.removeAllViews()
            containerView?.removeView(wv)
            wv.destroy()
        }

        webView = null
        containerView = null
        dragHandle = null
        resizeHandle = null
        onPositionChangedListener = null
        onSizeChangedListener = null
    }
}
