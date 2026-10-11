package com.samzebrado.transparentfloatingbrowser

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.WindowManager
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.widget.EditText

/** A service WebView cannot use WebView's default Activity-hosted JS dialogs. */
internal class OverlayJsDialogs(
    private val context: Context,
    private val canShow: () -> Boolean
) {
    private data class Pending(val dialog: AlertDialog, val completion: DialogCompletion)
    private var pending: Pending? = null

    fun alert(url: String?, message: String?, result: JsResult): Boolean =
        show(url, message, null, false, { result.confirm() }, { result.cancel() })

    fun confirm(url: String?, message: String?, result: JsResult): Boolean =
        show(url, message, null, true, { result.confirm() }, { result.cancel() })

    fun prompt(url: String?, message: String?, defaultValue: String?, result: JsPromptResult): Boolean =
        show(url, message, defaultValue.orEmpty(), true, { result.confirm(it.orEmpty()) }, { result.cancel() })

    private fun show(
        url: String?,
        message: String?,
        defaultValue: String?,
        hasCancel: Boolean,
        onConfirm: (String?) -> Unit,
        onCancel: () -> Unit
    ): Boolean {
        if (!canShow() || !Settings.canDrawOverlays(context) || pending != null) {
            onCancel()
            return true
        }

        val completion = DialogCompletion(onConfirm, onCancel)
        var entry: Pending? = null
        try {
            val themedContext = ContextThemeWrapper(context, android.R.style.Theme_Material_Light_Dialog_Alert)
            val input = defaultValue?.let { value ->
                EditText(themedContext).apply {
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
                    setSingleLine(true)
                    setText(value)
                    selectAll()
                }
            }
            val builder = AlertDialog.Builder(themedContext)
                .setTitle(context.getString(R.string.web_page_dialog, pageOrigin(url)))
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    entry?.let { finish(it, true, input?.text?.toString()) }
                }
            if (input != null) builder.setView(input)
            if (hasCancel) {
                builder.setNegativeButton(android.R.string.cancel) { _, _ ->
                    entry?.let { finish(it, false) }
                }
            }
            val dialog = builder.create()
            val created = Pending(dialog, completion)
            entry = created
            pending = created
            dialog.setCanceledOnTouchOutside(false)
            dialog.setOnCancelListener { finish(created, false) }
            dialog.setOnDismissListener { finish(created, false) }

            // Use only the overlay permission already needed by this service. The
            // dialog is focusable for keyboard input; the WebView's flags stay intact.
            val window = checkNotNull(dialog.window)
            window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            if (input != null) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
            dialog.show()
            input?.requestFocus()
        } catch (e: RuntimeException) {
            Log.w("OverlayJsDialogs", "Unable to show web page dialog", e)
            entry?.let { finish(it, false) } ?: completion.cancel()
        }
        return true
    }

    private fun finish(entry: Pending, confirmed: Boolean, value: String? = null) {
        if (pending !== entry) return
        // Release UI state before resuming JS, which can immediately request alert().
        pending = null
        try {
            entry.dialog.dismiss()
        } catch (e: RuntimeException) {
            Log.w("OverlayJsDialogs", "Unable to dismiss web page dialog", e)
        } finally {
            if (confirmed) entry.completion.confirm(value) else entry.completion.cancel()
        }
    }

    fun cancel() {
        pending?.let { finish(it, false) }
    }

    private fun pageOrigin(url: String?): String {
        val uri = Uri.parse(url.orEmpty())
        val host = uri.host
        return if (host != null) {
            val port = if (uri.port == -1) "" else ":${uri.port}"
            "${uri.scheme}://$host$port"
        } else {
            "${uri.scheme ?: "about"}:"
        }
    }
}
