package com.rud.ia

/**
 * Regras puras (sem Android) para apagar e restaurar conversas.
 * Ficam separadas do ViewModel para poderem ser testadas.
 */
object ConversationOps {

    data class DeleteResult(
        val conversations: List<Conversation>,
        val currentId: String,
        val removed: Conversation,
        val removedIndex: Int
    )

    /**
     * Remove a conversa [id]. Se era a última, cria uma nova vazia com [fresh].
     * Se era a conversa aberta, abre a primeira que sobrou.
     * Retorna null se a conversa não existe.
     */
    fun delete(
        conversations: List<Conversation>,
        currentId: String?,
        id: String,
        fresh: () -> Conversation
    ): DeleteResult? {
        val index = conversations.indexOfFirst { it.id == id }
        if (index < 0) return null
        val removed = conversations[index]
        val remaining = conversations.filterNot { it.id == id }.ifEmpty { listOf(fresh()) }
        val current = if (currentId == null || currentId == id || remaining.none { it.id == currentId }) {
            remaining.first().id
        } else {
            currentId
        }
        return DeleteResult(remaining, current, removed, index)
    }

    /**
     * Desfaz uma exclusão: recoloca [removed] na posição original.
     * A conversa vazia criada automaticamente na exclusão é descartada.
     */
    fun restore(conversations: List<Conversation>, removed: Conversation, index: Int): List<Conversation> {
        if (conversations.any { it.id == removed.id }) return conversations
        val base = if (conversations.size == 1 && conversations[0].messages.isEmpty()) emptyList() else conversations
        val at = index.coerceIn(0, base.size)
        return base.take(at) + removed + base.drop(at)
    }
}
