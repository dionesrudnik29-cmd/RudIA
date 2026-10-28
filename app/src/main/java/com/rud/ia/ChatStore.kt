package com.rud.ia

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Salva as conversas no armazenamento privado do app (filesDir/chats).
 * A gravação é atômica (arquivo temporário + rename) e mantém uma cópia de
 * segurança, para que um fechamento repentino não apague o histórico.
 */
class ChatStore(context: Context) {

    private val dir = File(context.filesDir, "chats")
    private val file = File(dir, "conversations.json")
    private val backup = File(dir, "conversations.json.bak")
    private val mutex = Mutex()

    suspend fun load(): List<Conversation> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val fromMain = if (file.isFile) runCatching { parse(file.readText()) }.getOrNull() else null
            fromMain ?: (if (backup.isFile) runCatching { parse(backup.readText()) }.getOrNull() else null)
                ?: emptyList()
        }
    }

    /** [snapshot] é lido dentro do lock: sempre grava o estado mais recente. */
    suspend fun save(snapshot: () -> List<Conversation>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val conversations = snapshot()
            dir.mkdirs()
            val tmp = File(dir, "conversations.json.tmp")
            FileOutputStream(tmp).use { out ->
                out.write(toJson(conversations).toByteArray(Charsets.UTF_8))
                out.fd.sync()
            }
            if (file.isFile) file.copyTo(backup, overwrite = true)
            if (!tmp.renameTo(file)) {
                tmp.copyTo(file, overwrite = true)
                tmp.delete()
            }
            Unit
        }
    }

    private fun toJson(conversations: List<Conversation>): String {
        val array = JSONArray()
        conversations.forEach { c ->
            val messages = JSONArray()
            c.messages.forEach { m ->
                messages.put(
                    JSONObject()
                        .put("id", m.id)
                        .put("role", m.role.name)
                        .put("text", m.text)
                        .put("status", m.status.name)
                )
            }
            array.put(
                JSONObject()
                    .put("id", c.id)
                    .put("title", c.title)
                    .put("updatedAt", c.updatedAt)
                    .put("messages", messages)
            )
        }
        return JSONObject().put("version", 1).put("conversations", array).toString()
    }

    private fun parse(text: String): List<Conversation> {
        val root = JSONObject(text)
        val array = root.getJSONArray("conversations")
        return (0 until array.length()).map { i ->
            val c = array.getJSONObject(i)
            val msgs = c.getJSONArray("messages")
            Conversation(
                id = c.getString("id"),
                title = c.optString("title", "Conversa"),
                updatedAt = c.optLong("updatedAt", 0L),
                messages = (0 until msgs.length()).map { j ->
                    val m = msgs.getJSONObject(j)
                    ChatMessage(
                        id = m.getString("id"),
                        role = runCatching { Role.valueOf(m.getString("role")) }.getOrDefault(Role.ASSISTANT),
                        text = m.optString("text", ""),
                        status = runCatching { MessageStatus.valueOf(m.optString("status")) }
                            .getOrDefault(MessageStatus.DONE)
                    )
                }
            )
        }
    }
}
