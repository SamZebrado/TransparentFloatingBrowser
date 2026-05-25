package com.samzebrado.transparentfloatingbrowser

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.input.InputManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import kotlin.math.min
import kotlin.math.pow

class FloatingWebViewService : Service() {

    companion object {
        private const val TAG = "FloatingWebViewService"
        const val PREFS_NAME = "FloatingWebViewServicePrefs"
        const val KEY_WINDOWS_JSON = "windows_json"

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "TransparentFloatingBrowser"

        private const val ACTION_START = "com.samzebrado.transparentfloatingbrowser.START"
        private const val ACTION_STOP = "com.samzebrado.transparentfloatingbrowser.STOP"
        private const val ACTION_TOGGLE_MODE = "com.samzebrado.transparentfloatingbrowser.TOGGLE_MODE"
        private const val ACTION_ADD_WINDOW = "com.samzebrado.transparentfloatingbrowser.ADD_WINDOW"
        private const val ACTION_ADD_WINDOW_CONFIG = "com.samzebrado.transparentfloatingbrowser.ADD_WINDOW_CONFIG"
        private const val ACTION_REMOVE_WINDOW = "com.samzebrado.transparentfloatingbrowser.REMOVE_WINDOW"
        private const val ACTION_SET_WINDOW_VISIBLE = "com.samzebrado.transparentfloatingbrowser.SET_WINDOW_VISIBLE"
        private const val EXTRA_URL = "url"
        private const val EXTRA_WINDOW_ID = "window_id"
        private const val EXTRA_WINDOW_CONFIG_JSON = "window_config_json"
        private const val EXTRA_WINDOW_VISIBLE = "window_visible"
        private const val MAX_WINDOWS = 3

        fun start(context: Context, url: String? = null) {
            val intent = Intent(context, FloatingWebViewService::class.java).apply {
                action = ACTION_START
                url?.let { putExtra(EXTRA_URL, it) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingWebViewService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun toggleMode(context: Context) {
            val intent = Intent(context, FloatingWebViewService::class.java).apply {
                action = ACTION_TOGGLE_MODE
            }
            context.startService(intent)
        }

        fun addWindow(context: Context) {
            val intent = Intent(context, FloatingWebViewService::class.java).apply {
                action = ACTION_ADD_WINDOW
            }
            context.startService(intent)
        }

        fun removeWindow(context: Context, windowId: Int) {
            val intent = Intent(context, FloatingWebViewService::class.java).apply {
                action = ACTION_REMOVE_WINDOW
                putExtra(EXTRA_WINDOW_ID, windowId)
            }
            context.startService(intent)
        }
        
        fun addWindowWithConfig(context: Context, config: FloatingWindowConfig) {
            val intent = Intent(context, FloatingWebViewService::class.java).apply {
                action = ACTION_ADD_WINDOW_CONFIG
                putExtra(EXTRA_WINDOW_CONFIG_JSON, config.toJson().toString())
            }
            context.startService(intent)
        }
        
        fun setWindowVisible(context: Context, windowId: Int, isVisible: Boolean) {
            val intent = Intent(context, FloatingWebViewService::class.java).apply {
                action = ACTION_SET_WINDOW_VISIBLE
                putExtra(EXTRA_WINDOW_ID, windowId)
                putExtra(EXTRA_WINDOW_VISIBLE, isVisible)
            }
            context.startService(intent)
        }
    }

    private var windowManager: WindowManager? = null
    private val windows = linkedMapOf<Int, FloatingWindowInstance>()
    private var activeWindowId: Int? = null
    private var currentMode: OverlayMode = OverlayMode.EDIT
    private var controlBubble: OverlayControlBubble? = null
    private var controlBubbleView: View? = null
    private var controlBubbleParams: WindowManager.LayoutParams? = null

    private fun getMaxObscuringOpacityForTouch(): Float {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(InputManager::class.java)?.maximumObscuringOpacityForTouch ?: 1.0f
        } else {
            1.0f
        }
    }

    private fun computeSafeAlpha(requestedAlpha: Float, overlapCount: Int): Float {
        val requested = requestedAlpha.coerceIn(0f, 1f)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return requested
        }

        val maxOpacity = getMaxObscuringOpacityForTouch()
        val n = overlapCount.coerceAtLeast(1)

        val perWindowMax = 1.0 - (1.0 - maxOpacity).pow(1.0 / n.toDouble())

        val safeCap = (perWindowMax - 0.01).coerceIn(0.0, 1.0).toFloat()

        return min(requested, safeCap)
    }

    private fun getWindowRect(instance: FloatingWindowInstance): Rect {
        return Rect(
            instance.params.x,
            instance.params.y,
            instance.params.x + instance.params.width,
            instance.params.y + instance.params.height
        )
    }

    private fun estimateOverlapCount(target: FloatingWindowInstance): Int {
        val targetRect = getWindowRect(target)

        val overlappingOthers = windows.values.count { other ->
            other.id != target.id &&
                    other.config.isVisible &&
                    other.rootView.isAttachedToWindow &&
                    Rect.intersects(targetRect, getWindowRect(other))
        }

        return 1 + overlappingOthers
    }

    private fun normalizeUrlOrNull(rawUrl: String?): String? {
        val trimmed = rawUrl?.trim().orEmpty()
        if (trimmed.isBlank()) return null

        val uri = Uri.parse(trimmed)
        val scheme = uri.scheme?.lowercase()

        return when (scheme) {
            "http", "https", "file" -> trimmed
            else -> null
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startAsForegroundService()
        loadSavedWindows()
        addControlBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand startId=$startId action=${intent?.action}, windows=${windows.size}, mode=$currentMode")

        when (intent?.action) {
            ACTION_START -> {
                val url = intent.getStringExtra(EXTRA_URL)
                handleStart(url)
            }
            ACTION_STOP -> {
                stopSelf()
            }
            ACTION_TOGGLE_MODE -> {
                toggleMode()
            }
            ACTION_ADD_WINDOW -> {
                addNewWindow()
            }
            ACTION_REMOVE_WINDOW -> {
                val windowId = intent.getIntExtra(EXTRA_WINDOW_ID, -1)
                if (windowId != -1) {
                    removeWindow(windowId)
                }
            }
            ACTION_ADD_WINDOW_CONFIG -> {
                val configJsonStr = intent.getStringExtra(EXTRA_WINDOW_CONFIG_JSON)
                if (configJsonStr != null) {
                    try {
                        val config = FloatingWindowConfig.fromJson(org.json.JSONObject(configJsonStr))
                        if (!windows.containsKey(config.id) && windows.size < MAX_WINDOWS) {
                            addWindow(config)
                            saveWindows()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse window config", e)
                    }
                }
            }
            ACTION_SET_WINDOW_VISIBLE -> {
                val windowId = intent.getIntExtra(EXTRA_WINDOW_ID, -1)
                val isVisible = intent.getBooleanExtra(EXTRA_WINDOW_VISIBLE, true)
                if (windowId != -1) {
                    setWindowVisible(windowId, isVisible)
                }
            }
        }

        return START_STICKY
    }

    private fun handleStart(url: String?) {
        if (windows.isEmpty()) {
            val defaultUrl = url ?: getString(R.string.default_url)
            val config = FloatingWindowConfig.createDefault(
                1,
                defaultUrl,
                resources.displayMetrics.density
            )
            addWindow(config)
        }
    }

    private fun addNewWindow() {
        if (windows.size >= MAX_WINDOWS) {
            return
        }

        var nextId = 1
        while (windows.containsKey(nextId)) {
            nextId++
        }

        val defaultUrl = getString(R.string.default_url)
        val config = FloatingWindowConfig.createDefault(
            nextId,
            defaultUrl,
            resources.displayMetrics.density
        )

        addWindow(config)
        saveWindows()
    }

    private fun addWindow(config: FloatingWindowConfig) {
        val controller = FloatingWebViewController(this)
        val rootView = controller.createView()

        val params = WindowManager.LayoutParams(
            config.width,
            config.height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = config.x
        params.y = config.y

        if (currentMode == OverlayMode.DISPLAY) {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            val requestedAlpha = config.viewModeAlpha / 100f
            val overlapCount = windows.size.coerceAtLeast(1)
            params.alpha = computeSafeAlpha(requestedAlpha, overlapCount)
            controller.setEditHandlesVisible(false)
        } else {
            params.alpha = 1f
            controller.setEditHandlesVisible(true)
        }

        if (config.isVisible) {
            windowManager?.addView(rootView, params)
        }

        val instance = FloatingWindowInstance(
            id = config.id,
            config = config,
            controller = controller,
            rootView = rootView,
            params = params
        )

        windows[config.id] = instance

        if (activeWindowId == null) {
            activeWindowId = config.id
            instance.isActive = true
        }

        controller.setMode(currentMode)
        controller.setOnPositionChangedListener { deltaX, deltaY ->
            params.x += deltaX
            params.y += deltaY
            instance.config.x = params.x
            instance.config.y = params.y
            try {
                if (rootView.isAttachedToWindow) {
                    windowManager?.updateViewLayout(rootView, params)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update window position", e)
            }
        }
        controller.setOnPositionChangeFinishedListener {
            saveWindows()
        }
        controller.setOnSizeChangedListener { deltaW, deltaH ->
            params.width = (params.width + deltaW).coerceAtLeast(100)
            params.height = (params.height + deltaH).coerceAtLeast(100)
            instance.config.width = params.width
            instance.config.height = params.height
            try {
                if (rootView.isAttachedToWindow) {
                    windowManager?.updateViewLayout(rootView, params)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update window size", e)
            }
        }
        controller.setOnSizeChangeFinishedListener {
            saveWindows()
        }

        controller.setInitialPosition(params.x, params.y)
        controller.setInitialSize(params.width, params.height)

        val validUrl = normalizeUrlOrNull(config.url) ?: getString(R.string.default_url)
        controller.loadUrl(validUrl)
    }

    private fun destroyWindow(instance: FloatingWindowInstance) {
        try {
            if (instance.rootView.isAttachedToWindow) {
                windowManager?.removeViewImmediate(instance.rootView)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to remove view for window ${instance.id}", e)
        }

        try {
            instance.controller.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to destroy controller for window ${instance.id}", e)
        }
    }

    private fun removeWindow(windowId: Int) {
        windows[windowId]?.let { instance ->
            destroyWindow(instance)
            windows.remove(windowId)
            if (activeWindowId == windowId) {
                activeWindowId = windows.keys.firstOrNull()
            }
            saveWindows()
        }
    }
    
    private fun setWindowVisible(windowId: Int, isVisible: Boolean) {
        windows[windowId]?.let { instance ->
            instance.config.isVisible = isVisible
            
            try {
                if (isVisible && !instance.rootView.isAttachedToWindow) {
                    windowManager?.addView(instance.rootView, instance.params)
                } else if (!isVisible && instance.rootView.isAttachedToWindow) {
                    windowManager?.removeView(instance.rootView)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update window visibility", e)
            }
            
            saveWindows()
        }
    }

    private fun toggleMode() {
        currentMode = if (currentMode == OverlayMode.EDIT) {
            OverlayMode.DISPLAY
        } else {
            OverlayMode.EDIT
        }

        windows.values.forEach { instance ->
            applyModeToWindow(instance)
        }

        controlBubble?.setMode(currentMode)
    }

    private fun applyModeToWindow(instance: FloatingWindowInstance) {
        if (!instance.config.isVisible) return

        val params = instance.params

        if (currentMode == OverlayMode.EDIT) {
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            params.alpha = 1f
            instance.controller.setEditHandlesVisible(true)
        } else {
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            val requestedAlpha = instance.config.viewModeAlpha / 100f
            val overlapCount = estimateOverlapCount(instance)
            params.alpha = computeSafeAlpha(requestedAlpha, overlapCount)
            instance.controller.setEditHandlesVisible(false)
        }

        try {
            windowManager?.updateViewLayout(instance.rootView, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update window mode", e)
        }
    }

    private fun loadSavedWindows() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_WINDOWS_JSON, null)

        if (jsonString != null) {
            try {
                val jsonArray = JSONArray(jsonString)
                val usedIds = mutableSetOf<Int>()
                
                for (i in 0 until jsonArray.length()) {
                    if (windows.size >= MAX_WINDOWS) {
                        Log.w(TAG, "Reached max windows ($MAX_WINDOWS), skipping remaining configs")
                        break
                    }
                    
                    val configJson = jsonArray.getJSONObject(i)
                    var config = FloatingWindowConfig.fromJson(configJson)
                    
                    // 处理重复ID
                    if (usedIds.contains(config.id)) {
                        var newId = 1
                        while (usedIds.contains(newId) || windows.containsKey(newId)) {
                            newId++
                        }
                        config = config.copy(id = newId)
                        Log.w(TAG, "Duplicate window id, reassigning to $newId")
                    }
                    
                    if (!windows.containsKey(config.id)) {
                        addWindow(config)
                        usedIds.add(config.id)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading windows", e)
            }
        }
    }

    private fun saveWindows() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()

        windows.values.forEach { instance ->
            jsonArray.put(instance.config.toJson())
        }

        prefs.edit().putString(KEY_WINDOWS_JSON, jsonArray.toString()).apply()
    }

    private fun addControlBubble() {
        try {
            val density = resources.displayMetrics.density
            val appPrefs = getSharedPreferences(AppPrefs.MAIN_PREFS, Context.MODE_PRIVATE)
            val sizeDp = appPrefs.getInt(
                AppPrefs.KEY_CONTROL_BUTTON_SIZE_DP,
                AppPrefs.DEFAULT_CONTROL_BUTTON_SIZE_DP
            )
            val sizePx = (sizeDp * density).toInt()

            controlBubble = OverlayControlBubble(
                context = this,
                onToggleMode = { toggleMode() },
                onClose = { stopSelf() }
            )
            controlBubbleView = controlBubble?.createView(sizePx)

            val defaultX = (AppPrefs.DEFAULT_CONTROL_BUTTON_X * density).toInt()
            val defaultY = (AppPrefs.DEFAULT_CONTROL_BUTTON_Y * density).toInt()

            controlBubbleParams = controlBubble?.getParams(defaultX, defaultY)
            controlBubble?.setMode(currentMode)

            controlBubble?.setOnPositionChangedListener { x, y ->
                val prefs = getSharedPreferences(AppPrefs.MAIN_PREFS, Context.MODE_PRIVATE)
                prefs.edit()
                    .putInt(AppPrefs.KEY_CONTROL_BUTTON_X, x)
                    .putInt(AppPrefs.KEY_CONTROL_BUTTON_Y, y)
                    .apply()
            }

            windowManager?.addView(controlBubbleView, controlBubbleParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add control bubble", e)
            controlBubble = null
            controlBubbleView = null
            controlBubbleParams = null
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.app.PendingIntent.getActivity(
                this,
                0,
                intent,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            android.app.PendingIntent.getActivity(
                this,
                0,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Transparent Floating Browser")
            .setContentText("Running in background")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun startAsForegroundService() {
        Log.d(TAG, "Calling startForeground")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    createNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, createNotification())
            }
            Log.d(TAG, "startForeground success")
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed", e)
            throw e
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Floating Browser Service"
            val descriptionText = "Transparent Floating Browser"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")

        windows.values.toList().forEach { instance ->
            destroyWindow(instance)
        }
        windows.clear()
        activeWindowId = null

        controlBubbleView?.let { view ->
            try {
                windowManager?.removeViewImmediate(view)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove control bubble", e)
            }
        }
        controlBubbleView = null
        controlBubble = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
