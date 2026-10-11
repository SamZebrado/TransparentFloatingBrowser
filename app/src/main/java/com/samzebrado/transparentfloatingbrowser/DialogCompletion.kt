package com.samzebrado.transparentfloatingbrowser

/** Button, dismissal and lifecycle callbacks must resolve a JS result only once. */
internal class DialogCompletion(
    private val onConfirm: (String?) -> Unit,
    private val onCancel: () -> Unit
) {
    private var completed = false

    fun confirm(value: String?) {
        if (completed) return
        completed = true
        onConfirm(value)
    }

    fun cancel() {
        if (completed) return
        completed = true
        onCancel()
    }
}
