package com.rud.ia

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Conversation(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val messages: List<ChatMessage>
)

class ChatHistoryStore(context: Context) {

    private val file = File(context.filesDir, "conversations.json")

    fun load(): List<Conversation> {
        if (!file.exists()) return emptyList()

        val array = JSONArray(file.readText())
        return List(array.length()) { index ->
            array.getJSONObject(index).toConversation()
        }
    }

    fun save(conversations: List<Conversation>) {
        val array = JSONArray()
        conversations.forEach { array.put(it.toJson()) }

        val temp = File(file.parentFile, file.name + ".tmp")
        temp.writeText(array.toString())
        if (!temp.renameTo(file)) {
            temp.copyTo(file, overwrite = true)
            temp.delete()
        }
    }

    private fun Conversation.toJson(): JSONObject {
        val messagesJson = JSONArray()
        messages.forEach { message ->
            messagesJson.put(
                JSONObject()
                    .put("text", message.text)
                    .put("isUser", message.isUser)
            )
        }

        return JSONObject()
            .put("id", id)
            .put("title", title)
            .put("updatedAt", updatedAt)
            .put("messages", messagesJson)
    }

    private fun JSONObject.toConversation(): Conversation {
        val messagesJson = getJSONArray("messages")
        val messages = List(messagesJson.length()) { index ->
            val item = messagesJson.getJSONObject(index)
            ChatMessage(
                text = item.getString("text"),
                isUser = item.getBoolean("isUser")
            )
        }

        return Conversation(
            id = getString("id"),
            title = getString("title"),
            updatedAt = getLong("updatedAt"),
            messages = messages
        )
    }
}
