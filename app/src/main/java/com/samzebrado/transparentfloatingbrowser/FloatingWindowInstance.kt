package com.samzebrado.transparentfloatingbrowser

import android.view.View
import android.view.WindowManager

data class FloatingWindowInstance(
    val id: Int,
    val config: FloatingWindowConfig,
    val controller: FloatingWebViewController,
    val rootView: View,
    val params: WindowManager.LayoutParams,
    var isActive: Boolean = false
)
