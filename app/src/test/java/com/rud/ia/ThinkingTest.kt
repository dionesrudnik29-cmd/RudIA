package com.rud.ia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThinkingTest {
    @Test fun splitsThinkingFromAnswer() {
        val (t, a) = PromptBuilder.splitThinking("O usuário disse oi.\n</think>\n\nOlá! 😊<|im_end|>")
        assertEquals("O usuário disse oi.", t)
        assertEquals("Olá! 😊", a)
    }
    @Test fun stillThinkingHasNoAnswer() {
        val (t, a) = PromptBuilder.splitThinking("Vou pensar")
        assertEquals("Vou pensar", t)
        assertEquals("", a)
    }
    @Test fun promptOpensThinkBlockOnlyWhenEnabled() {
        val on = PromptBuilder.build(emptyList(), "oi", 2048, 512, { it.length / 4 }, thinking = true)
        val off = PromptBuilder.build(emptyList(), "oi", 2048, 512, { it.length / 4 })
        assertTrue(on.endsWith("assistant\n<think>\n"))
        assertTrue(off.endsWith("</think>\n\n"))
    }
}
