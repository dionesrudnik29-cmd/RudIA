package com.rud.ia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {

    // Contador simples: 1 "token" por caractere.
    private val countChars: (String) -> Int = { it.length }

    private fun msg(role: Role, text: String, status: MessageStatus = MessageStatus.DONE) =
        ChatMessage(id = text, role = role, text = text, status = status)

    @Test
    fun cleanRemovesThinkBlock() {
        assertEquals("Olá!", PromptBuilder.clean("<think>\nraciocínio\n</think>\n\nOlá!"))
    }

    @Test
    fun cleanHidesUnfinishedThinkBlock() {
        assertEquals("", PromptBuilder.clean("<think>ainda pensando"))
    }

    @Test
    fun promptDisablesThinkingAndEndsAtAssistantTurn() {
        val prompt = PromptBuilder.build(emptyList(), "Oi", 4096, 512, countChars)
        assertTrue(prompt.endsWith("<|im_start|>assistant\n<think>\n\n</think>\n\n"))
        assertTrue(prompt.contains("<|im_start|>user\nOi<|im_end|>"))
    }

    @Test
    fun promptKeepsRecentHistoryThatFits() {
        val history = listOf(
            msg(Role.USER, "pergunta antiga " + "x".repeat(3000)),
            msg(Role.ASSISTANT, "resposta antiga"),
            msg(Role.USER, "pergunta recente"),
            msg(Role.ASSISTANT, "resposta recente")
        )
        val prompt = PromptBuilder.build(history, "nova", 2048, 512, countChars)
        assertTrue(prompt.contains("resposta recente"))
        assertFalse(prompt.contains("pergunta antiga"))
    }

    @Test
    fun promptSkipsFailedMessages() {
        val history = listOf(msg(Role.ASSISTANT, "Erro: falhou", MessageStatus.ERROR))
        val prompt = PromptBuilder.build(history, "oi", 4096, 512, countChars)
        assertFalse(prompt.contains("Erro: falhou"))
    }

    @Test
    fun titleIsTruncatedTo40Chars() {
        val title = PromptBuilder.makeTitle("a".repeat(100))
        assertEquals(40, title.length)
        assertTrue(title.endsWith("…"))
    }
}
