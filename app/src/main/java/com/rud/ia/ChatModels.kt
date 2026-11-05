package com.rud.ia

enum class Role { USER, ASSISTANT }

enum class MessageStatus {
    /** Resposta completa. */
    DONE,
    /** Resposta sendo gerada. */
    PENDING,
    /** Falha ao gerar. */
    ERROR,
    /** Interrompida pelo usuário ou porque o app foi fechado. */
    STOPPED
}

data class ChatMessage(
    val id: String,
    val role: Role,
    val text: String,
    val status: MessageStatus = MessageStatus.DONE,
    /** Foto enviada junto com a mensagem (arquivo no armazenamento do app). */
    val imagePath: String? = null,
    /** O que o leitor de imagens viu na foto; reaproveitado nas perguntas seguintes. */
    val imageDescription: String? = null
)

data class Conversation(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val messages: List<ChatMessage>
)
