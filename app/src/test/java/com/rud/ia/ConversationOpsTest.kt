package com.rud.ia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationOpsTest {

    private fun conv(id: String, vararg texts: String) = Conversation(
        id = id,
        title = id,
        updatedAt = 0L,
        messages = texts.mapIndexed { i, t -> ChatMessage("$id-$i", Role.USER, t) }
    )

    private val fresh = { conv("nova") }

    @Test
    fun deletingOpenConversationRemovesItAndOpensNext() {
        val r = ConversationOps.delete(listOf(conv("a", "oi"), conv("b", "olá")), "a", "a", fresh)!!
        assertEquals(listOf("b"), r.conversations.map { it.id })
        assertEquals("b", r.currentId)
    }

    @Test
    fun deletingAnotherConversationKeepsTheOpenOne() {
        val r = ConversationOps.delete(listOf(conv("a", "oi"), conv("b", "olá")), "a", "b", fresh)!!
        assertEquals(listOf("a"), r.conversations.map { it.id })
        assertEquals("a", r.currentId)
    }

    @Test
    fun deletingLastConversationLeavesAnEmptyNewOne() {
        val r = ConversationOps.delete(listOf(conv("a", "oi")), "a", "a", fresh)!!
        assertEquals(listOf("nova"), r.conversations.map { it.id })
        assertTrue(r.conversations[0].messages.isEmpty())
        assertEquals("nova", r.currentId)
    }

    @Test
    fun deletingUnknownConversationDoesNothing() {
        assertNull(ConversationOps.delete(listOf(conv("a")), "a", "zzz", fresh))
    }

    @Test
    fun undoPutsConversationBackInItsPlace() {
        val list = listOf(conv("a", "1"), conv("b", "2"), conv("c", "3"))
        val r = ConversationOps.delete(list, "b", "b", fresh)!!
        val restored = ConversationOps.restore(r.conversations, r.removed, r.removedIndex)
        assertEquals(listOf("a", "b", "c"), restored.map { it.id })
        assertEquals(listOf("2"), restored[1].messages.map { it.text })
    }

    @Test
    fun undoAfterDeletingLastDropsTheAutoCreatedEmptyChat() {
        val r = ConversationOps.delete(listOf(conv("a", "oi")), "a", "a", fresh)!!
        val restored = ConversationOps.restore(r.conversations, r.removed, r.removedIndex)
        assertEquals(listOf("a"), restored.map { it.id })
    }
}
