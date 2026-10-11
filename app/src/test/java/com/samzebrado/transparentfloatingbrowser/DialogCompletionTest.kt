package com.samzebrado.transparentfloatingbrowser

import org.junit.Assert.assertEquals
import org.junit.Test

class DialogCompletionTest {
    @Test fun confirmThenDismissAndLifecycleCancelReturnsEnteredTextOnce() {
        val responses = mutableListOf<String?>()
        var cancellations = 0
        val result = DialogCompletion({ responses.add(it) }, { cancellations++ })
        result.confirm("ws://192.168.1.2:8001")
        result.cancel()
        result.cancel()
        result.confirm("different")
        assertEquals(listOf("ws://192.168.1.2:8001"), responses)
        assertEquals(0, cancellations)
    }

    @Test fun cancelThenLateConfirmCannotSaveAddress() {
        val responses = mutableListOf<String?>()
        var cancellations = 0
        val result = DialogCompletion({ responses.add(it) }, { cancellations++ })
        result.cancel()
        result.confirm("ws://192.168.1.2:8001")
        result.cancel()
        assertEquals(emptyList<String>(), responses)
        assertEquals(1, cancellations)
    }

    @Test fun emptyInputAndRepeatedPromptsHaveIndependentResults() {
        val responses = mutableListOf<String?>()
        repeat(3) {
            val result = DialogCompletion({ responses.add(it) }, { error("unexpected cancel") })
            result.confirm("")
            result.cancel()
        }
        assertEquals(listOf("", "", ""), responses)
    }
}
