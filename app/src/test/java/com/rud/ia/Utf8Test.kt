package com.rud.ia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class Utf8Test {

    private val text = "Oi 😊"
    private val bytes = text.toByteArray(Charsets.UTF_8)

    @Test
    fun emojiCutInHalfIsHeldBackUntilComplete() {
        // "Oi " = 3 bytes; o emoji ocupa os 4 seguintes.
        assertEquals(3, Utf8.completeLength(bytes, bytes.size - 1))
        assertEquals("Oi ", Utf8.decodeComplete(bytes, bytes.size - 2))
    }

    @Test
    fun completeEmojiIsShown() {
        assertEquals(bytes.size, Utf8.completeLength(bytes))
        assertEquals(text, Utf8.decodeComplete(bytes))
    }

    @Test
    fun neverShowsReplacementCharacter() {
        for (cut in 0..bytes.size) {
            assertFalse(Utf8.decodeComplete(bytes, cut).contains('\uFFFD'))
        }
    }
}
