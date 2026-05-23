package com.samzebrado.transparentfloatingbrowser

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var etUrl: EditText
    private lateinit var tvHandleSize: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        etUrl = EditText(this).apply {
            hint = resources.getString(R.string.url_hint)
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        layout.addView(etUrl)

        val btnCheckPermission = Button(this).apply {
            text = resources.getString(R.string.check_permission)
            setOnClickListener { checkOverlayPermission() }
        }
        layout.addView(btnCheckPermission)

        val btnStartService = Button(this).apply {
            text = resources.getString(R.string.start_service)
            setOnClickListener {
                saveUrl()
                startFloatingService()
            }
        }
        layout.addView(btnStartService)

        val btnStopService = Button(this).apply {
            text = resources.getString(R.string.stop_service)
            setOnClickListener { stopFloatingService() }
        }
        layout.addView(btnStopService)

        val handleSizeLabel = TextView(this).apply {
            text = "Handle Size (Edit mode only)"
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
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        layout.addView(seekHandleSize)

        setContentView(layout)
        loadSavedUrl()
    }

    private fun loadSavedUrl() {
        val prefs = getSharedPreferences(AppPrefs.MAIN_PREFS, Context.MODE_PRIVATE)
        val savedUrl = prefs.getString(AppPrefs.KEY_URL, getString(R.string.default_url))
        etUrl.setText(savedUrl)
    }

    private fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        return "http://$trimmed"
    }

    private fun saveUrl() {
        val url = normalizeUrl(etUrl.text.toString())
        val prefs = getSharedPreferences(AppPrefs.MAIN_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(AppPrefs.KEY_URL, url).apply()
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
                Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startFloatingService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
                return
            }
        }

        val url = normalizeUrl(etUrl.text.toString())
        FloatingWebViewService.start(this, url)
        Toast.makeText(this, R.string.service_started, Toast.LENGTH_SHORT).show()
    }

    private fun stopFloatingService() {
        val intent = Intent(this, FloatingWebViewService::class.java)
        stopService(intent)
        Toast.makeText(this, R.string.service_stopped, Toast.LENGTH_SHORT).show()
    }
}
