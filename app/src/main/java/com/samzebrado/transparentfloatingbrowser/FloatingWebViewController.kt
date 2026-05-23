package com.samzebrado.transparentfloatingbrowser

import android.content.Context
import android.graphics.Color
import android.util.Log
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

    fun createView(): View {
        containerView = FrameLayout(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }

        // WebView
        webView = WebView(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false

            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "onPageFinished: $url")
                    if (view != null) {
                        TransparentStyleInjector.inject(view) { result ->
                            Log.d(TAG, "Transparent injection result: $result")
                        }
                    }
                }
            }
        }

        // Top drag handle
        dragHandle = View(context).apply {
            val density = resources.displayMetrics.density
            val prefs = context.getSharedPreferences(AppPrefs.MAIN_PREFS, Context.MODE_PRIVATE)
            val sizeDp = prefs.getInt(AppPrefs.KEY_HANDLE_SIZE_DP, AppPrefs.DEFAULT_HANDLE_SIZE_DP)
            val heightPx = (sizeDp * density).toInt()
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, heightPx).apply {
                gravity = android.view.Gravity.TOP
            }
            setBackgroundColor(Color.parseColor("#6633B5E5")) // Semi-transparent cyan
        }

        // Bottom-right resize handle
        resizeHandle = View(context).apply {
            val density = resources.displayMetrics.density
            val prefs = context.getSharedPreferences(AppPrefs.MAIN_PREFS, Context.MODE_PRIVATE)
            val sizeDp = prefs.getInt(AppPrefs.KEY_HANDLE_SIZE_DP, AppPrefs.DEFAULT_HANDLE_SIZE_DP)
            val sizePx = (sizeDp * density).toInt()
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx).apply {
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
            }
            setBackgroundColor(Color.parseColor("#66FF9800")) // Semi-transparent orange
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
    }
}
