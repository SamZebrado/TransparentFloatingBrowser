package com.samzebrado.transparentfloatingbrowser

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {

    companion object {
        private const val REQUEST_EXPORT = 1001
        private const val REQUEST_IMPORT = 1002
    }

    private lateinit var etUrl: EditText
    private lateinit var etTransparentColors: EditText
    private lateinit var tvHandleSize: TextView
    private lateinit var tvControlButtonSize: TextView
    private lateinit var tvViewModeAlpha: TextView
    private lateinit var langButton: Button
    private lateinit var windowsContainer: LinearLayout
    private val windowCheckboxes = mutableMapOf<Int, CheckBox>()
    private val savedConfigs = mutableMapOf<Int, FloatingWindowConfig>()

    override fun attachBaseContext(newBase: Context) {
        val lang = LocaleHelper.getLanguage(newBase)
        super.attachBaseContext(LocaleHelper.setLocale(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        langButton = Button(this).apply {
            text = LocaleHelper.getLanguageButtonText(this@MainActivity)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                val currentLang = LocaleHelper.getLanguage(this@MainActivity)
                val newLang = if (currentLang == AppPrefs.LANGUAGE_ZH) {
                    AppPrefs.LANGUAGE_EN
                } else {
                    AppPrefs.LANGUAGE_ZH
                }
                LocaleHelper.saveLanguage(this@MainActivity, newLang)
                recreate()
            }
        }
        headerLayout.addView(langButton)
        layout.addView(headerLayout)

        val urlLabel = TextView(this).apply {
            text = getString(R.string.url_input_label)
        }
        layout.addView(urlLabel)

        etUrl = EditText(this).apply {
            hint = getString(R.string.url_hint)
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        layout.addView(etUrl)

        val btnCheckPermission = Button(this).apply {
            text = getString(R.string.btn_check_permission)
            setOnClickListener { checkOverlayPermission() }
        }
        layout.addView(btnCheckPermission)

        val btnStartService = Button(this).apply {
            text = getString(R.string.btn_start_service)
            setOnClickListener {
                startFloatingServiceFromInput()
            }
        }
        layout.addView(btnStartService)

        val btnStopService = Button(this).apply {
            text = getString(R.string.btn_stop_service)
            setOnClickListener { stopFloatingService() }
        }
        layout.addView(btnStopService)

        val handleSizeLabel = TextView(this).apply {
            text = getString(R.string.handle_size_label)
            setPadding(0, 16, 0, 8)
        }
        layout.addView(handleSizeLabel)

        tvHandleSize = TextView(this).apply {
            val prefs = getSharedPreferences(AppPrefs.MAIN_PREFS, Context.MODE_PRIVATE)
            val sizeDp = prefs.getInt(AppPrefs.KEY_HANDLE_SIZE_DP, AppPrefs.DEFAULT_HANDLE_SIZE_DP)
            text = "${sizeDp}dp"
        }
        layout.addView(tvHandleSize)

        val seekHandleSize = SeekBar(this).apply {
            max = AppPrefs.MAX_HANDLE_SIZE_DP - AppPrefs.MIN_HANDLE_SIZE_DP
            val prefs = getSharedPreferences(AppPrefs.MAIN_PREFS, Context.MODE_PRIVATE)
            val current = prefs.getInt(AppPrefs.KEY_HANDLE_SIZE_DP, AppPrefs.DEFAULT_HANDLE_SIZE_DP)
            progress = current - AppPrefs.MIN_HANDLE_SIZE_DP

            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val sizeDp = AppPrefs.MIN_HANDLE_SIZE_DP + progress
                    tvHandleSize.text = "${sizeDp}dp"
                    prefs.edit().putInt(AppPrefs.KEY_HANDLE_SIZE_DP, sizeDp).apply()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    Toast.makeText(this@MainActivity, R.string.toast_restart_overlay_to_apply, Toast.LENGTH_SHORT).show()
                }
            })
        }
        layout.addView(seekHandleSize)

        val controlButtonSizeLabel = TextView(this).apply {
            text = getString(R.string.control_button_size_label)
            setPadding(0, 16, 0, 8)
        }
        layout.addView(controlButtonSizeLabel)

        tvControlButtonSize = TextView(this).apply {
            val prefs = getSharedPreferences(AppPrefs.MAIN_PREFS, Context.MODE_PRIVATE)
            val sizeDp = prefs.getInt(AppPrefs.KEY_CONTROL_BUTTON_SIZE_DP, AppPrefs.DEFAULT_CONTROL_BUTTON_SIZE_DP)
            text = "${sizeDp}dp"
        }
        layout.addView(tvControlButtonSize)

        val seekControlButtonSize = SeekBar(this).apply {
            max = AppPrefs.MAX_CONTROL_BUTTON_SIZE_DP - AppPrefs.MIN_CONTROL_BUTTON_SIZE_DP
            val prefs = getSharedPreferences(AppPrefs.MAIN_PREFS, Context.MODE_PRIVATE)
            val current = prefs.getInt(AppPrefs.KEY_CONTROL_BUTTON_SIZE_DP, AppPrefs.DEFAULT_CONTROL_BUTTON_SIZE_DP)
            progress = current - AppPrefs.MIN_CONTROL_BUTTON_SIZE_DP

            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val sizeDp = AppPrefs.MIN_CONTROL_BUTTON_SIZE_DP + progress
                    tvControlButtonSize.text = "${sizeDp}dp"
                    prefs.edit().putInt(AppPrefs.KEY_CONTROL_BUTTON_SIZE_DP, sizeDp).apply()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    Toast.makeText(this@MainActivity, R.string.toast_restart_overlay_to_apply, Toast.LENGTH_SHORT).show()
                }
            })
        }
        layout.addView(seekControlButtonSize)

        val viewModeAlphaLabel = TextView(this).apply {
            text = getString(R.string.view_mode_alpha_label)
            setPadding(0, 16, 0, 8)
        }
        layout.addView(viewModeAlphaLabel)

        tvViewModeAlpha = TextView(this).apply {
            val prefs = getSharedPreferences(AppPrefs.MAIN_PREFS, Context.MODE_PRIVATE)
            val alphaPercent = prefs.getInt(AppPrefs.KEY_VIEW_MODE_ALPHA_PERCENT, AppPrefs.DEFAULT_VIEW_MODE_ALPHA_PERCENT)
            text = "${alphaPercent}%"
        }
        layout.addView(tvViewModeAlpha)

        val seekViewModeAlpha = SeekBar(this).apply {
            max = AppPrefs.MAX_VIEW_MODE_ALPHA_PERCENT - AppPrefs.MIN_VIEW_MODE_ALPHA_PERCENT
            val prefs = getSharedPreferences(AppPrefs.MAIN_PREFS, Context.MODE_PRIVATE)
            val current = prefs.getInt(AppPrefs.KEY_VIEW_MODE_ALPHA_PERCENT, AppPrefs.DEFAULT_VIEW_MODE_ALPHA_PERCENT)
            progress = current - AppPrefs.MIN_VIEW_MODE_ALPHA_PERCENT

            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val alphaPercent = AppPrefs.MIN_VIEW_MODE_ALPHA_PERCENT + progress
                    tvViewModeAlpha.text = "${alphaPercent}%"
                    prefs.edit().putInt(AppPrefs.KEY_VIEW_MODE_ALPHA_PERCENT, alphaPercent).apply()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    Toast.makeText(this@MainActivity, R.string.toast_view_alpha_apply_next_view_mode, Toast.LENGTH_SHORT).show()
                }
            })
        }
        layout.addView(seekViewModeAlpha)

        val cbEnableZoom = CheckBox(this).apply {
            text = getString(R.string.enable_zoom)
            val prefs = getSharedPreferences(AppPrefs.MAIN_PREFS, Context.MODE_PRIVATE)
            isChecked = prefs.getBoolean(AppPrefs.KEY_ENABLE_WEBVIEW_ZOOM, AppPrefs.DEFAULT_ENABLE_WEBVIEW_ZOOM)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean(AppPrefs.KEY_ENABLE_WEBVIEW_ZOOM, isChecked).apply()
                Toast.makeText(this@MainActivity, R.string.toast_restart_overlay_to_apply, Toast.LENGTH_SHORT).show()
            }
        }
        layout.addView(cbEnableZoom)

        val transparentColorsLabel = TextView(this).apply {
            text = getString(R.string.transparent_colors_label)
            setPadding(0, 16, 0, 8)
        }
        layout.addView(transparentColorsLabel)

        etTransparentColors = EditText(this).apply {
            hint = getString(R.string.transparent_colors_hint)
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
            val prefs = getSharedPreferences(AppPrefs.MAIN_PREFS, Context.MODE_PRIVATE)
            val savedColors = prefs.getString(AppPrefs.KEY_TRANSPARENT_COLORS, AppPrefs.DEFAULT_TRANSPARENT_COLORS)
            setText(savedColors)

            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    Toast.makeText(this@MainActivity, R.string.toast_transparent_colors_apply, Toast.LENGTH_SHORT).show()
                }
            }
        }
        layout.addView(etTransparentColors)

        val windowManagementLabel = TextView(this).apply {
            text = "Window Management"
            setPadding(0, 24, 0, 8)
            textSize = 18f
        }
        layout.addView(windowManagementLabel)

        val btnAddWindow = Button(this).apply {
            text = "Add Window"
            setOnClickListener { addNewWindowConfig() }
        }
        layout.addView(btnAddWindow)

        windowsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 0)
        }
        layout.addView(windowsContainer)

        val importExportLabel = TextView(this).apply {
            text = "Import/Export"
            setPadding(0, 24, 0, 8)
            textSize = 18f
        }
        layout.addView(importExportLabel)

        val btnExport = Button(this).apply {
            text = "Export Config"
            setOnClickListener { exportConfig() }
        }
        layout.addView(btnExport)

        val btnImport = Button(this).apply {
            text = "Import Config"
            setOnClickListener { importConfig() }
        }
        layout.addView(btnImport)

        scrollView.addView(layout)
        setContentView(scrollView)

        loadSavedUrl()
        loadSavedWindowConfigs()
    }

    private fun loadSavedUrl() {
        val prefs = getSharedPreferences(AppPrefs.MAIN_PREFS, Context.MODE_PRIVATE)
        val savedUrl = prefs.getString(AppPrefs.KEY_URL, getString(R.string.default_url))
        etUrl.setText(savedUrl)
    }

    private fun loadSavedWindowConfigs() {
        val prefs = getSharedPreferences(FloatingWebViewService.PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(FloatingWebViewService.KEY_WINDOWS_JSON, null)

        if (jsonString != null) {
            try {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val configJson = jsonArray.getJSONObject(i)
                    val config = FloatingWindowConfig.fromJson(configJson)
                    addWindowConfigToUI(config)
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun addWindowConfigToUI(config: FloatingWindowConfig) {
        val windowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 8)
            setBackgroundResource(android.R.color.white)
            tag = config.id
            val marginParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            marginParams.setMargins(0, 8, 0, 0)
            layoutParams = marginParams
        }

        val windowTitle = TextView(this).apply {
            text = "Window ${config.id}"
            textSize = 16f
            setPadding(0, 0, 0, 4)
        }
        windowLayout.addView(windowTitle)

        val checkboxLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val cbVisible = CheckBox(this).apply {
            text = "Visible"
            isChecked = config.isVisible
            setOnCheckedChangeListener { _, isChecked ->
                savedConfigs[config.id]?.isVisible = isChecked
                saveWindowConfigs()
                FloatingWebViewService.setWindowVisible(this@MainActivity, config.id, isChecked)
            }
        }
        checkboxLayout.addView(cbVisible)
        windowCheckboxes[config.id] = cbVisible

        val btnRemove = Button(this).apply {
            text = "Remove"
            setOnClickListener { removeWindowConfig(config.id) }
        }
        checkboxLayout.addView(btnRemove)
        windowLayout.addView(checkboxLayout)

        val urlLabel = TextView(this).apply {
            text = "URL:"
            textSize = 12f
        }
        windowLayout.addView(urlLabel)

        val etWindowUrl = EditText(this).apply {
            setText(config.url)
            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    savedConfigs[config.id]?.url = text.toString()
                    saveWindowConfigs()
                }
            }
        }
        windowLayout.addView(etWindowUrl)

        windowsContainer.addView(windowLayout)

        savedConfigs[config.id] = config
    }

    private fun addNewWindowConfig() {
        var nextId = 1
        while (savedConfigs.containsKey(nextId)) {
            nextId++
        }

        val density = resources.displayMetrics.density
        val config = FloatingWindowConfig.createDefault(
            nextId,
            getString(R.string.default_url),
            density
        )

        addWindowConfigToUI(config)
        saveWindowConfigs()
        
        FloatingWebViewService.addWindowWithConfig(this, config)
    }

    private fun removeWindowConfig(windowId: Int) {
        savedConfigs.remove(windowId)
        windowCheckboxes.remove(windowId)

        for (i in 0 until windowsContainer.childCount) {
            val child = windowsContainer.getChildAt(i)
            val childTag = child.tag
            if (childTag is Int && childTag == windowId) {
                windowsContainer.removeViewAt(i)
                break
            }
        }
        
        saveWindowConfigs()
        FloatingWebViewService.removeWindow(this, windowId)
    }
    
    private fun saveWindowConfigs() {
        val prefs = getSharedPreferences(FloatingWebViewService.PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        
        savedConfigs.values.forEach { config ->
            jsonArray.put(config.toJson())
        }
        
        prefs.edit()
            .putString(FloatingWebViewService.KEY_WINDOWS_JSON, jsonArray.toString())
            .apply()
    }

    private fun exportConfig() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "floating_browser_config.json")
        }
        startActivityForResult(intent, REQUEST_EXPORT)
    }

    private fun importConfig() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        startActivityForResult(intent, REQUEST_IMPORT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_EXPORT && resultCode == RESULT_OK) {
            data?.data?.let { uri -> writeConfigToUri(uri) }
        } else if (requestCode == REQUEST_IMPORT && resultCode == RESULT_OK) {
            data?.data?.let { uri -> readConfigFromUri(uri) }
        }
    }

    private fun writeConfigToUri(uri: Uri) {
        try {
            val jsonArray = JSONArray()
            savedConfigs.values.forEach { config ->
                jsonArray.put(config.toJson())
            }

            contentResolver.openOutputStream(uri)?.use { output ->
                output.write(jsonArray.toString().toByteArray())
            }
            Toast.makeText(this, "Config exported successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to export config", Toast.LENGTH_SHORT).show()
        }
    }

    private fun readConfigFromUri(uri: Uri) {
        try {
            val jsonString = contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader().use { it.readText() }
            }

            if (jsonString != null) {
                val jsonArray = JSONArray(jsonString)
                savedConfigs.clear()
                windowsContainer.removeAllViews()

                for (i in 0 until jsonArray.length()) {
                    val configJson = jsonArray.getJSONObject(i)
                    val config = FloatingWindowConfig.fromJson(configJson)
                    addWindowConfigToUI(config)
                }
                Toast.makeText(this, "Config imported successfully", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to import config", Toast.LENGTH_SHORT).show()
        }
    }

    private fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            Toast.makeText(this, R.string.toast_url_empty, Toast.LENGTH_SHORT).show()
            return getString(R.string.default_url)
        }
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return trimmed
        }
        return "http://$trimmed"
    }

    private fun parseColorKeys(raw: String): List<String> {
        val colors = raw.split(",", " ", "\n", ";")
            .map { it.trim() }
            .filter { it.matches(Regex("^#?[0-9a-fA-F]{6}$")) }
            .map { if (it.startsWith("#")) it.uppercase() else "#${it.uppercase()}" }
            .distinct()

        return if (colors.isEmpty()) {
            listOf(AppPrefs.DEFAULT_TRANSPARENT_COLORS)
        } else {
            colors
        }
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.permission_already_granted, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startFloatingServiceFromInput() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
                return
            }
        }

        val url = normalizeUrl(etUrl.text.toString())
        etUrl.setText(url)

        val colorKeys = parseColorKeys(etTransparentColors.text.toString())
        val colorKeysString = colorKeys.joinToString(",")

        val prefs = getSharedPreferences(AppPrefs.MAIN_PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(AppPrefs.KEY_URL, url)
            .putString(AppPrefs.KEY_TRANSPARENT_COLORS, colorKeysString)
            .apply()

        FloatingWebViewService.start(this, url)
        Toast.makeText(this, R.string.service_started, Toast.LENGTH_SHORT).show()
    }

    private fun stopFloatingService() {
        FloatingWebViewService.stop(this)
        Toast.makeText(this, R.string.service_stopped, Toast.LENGTH_SHORT).show()
    }

    data class WindowConfigViewModel(
        val id: Int,
        var url: String,
        var isVisible: Boolean
    )
}
