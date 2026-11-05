package com.rud.ia

/**
 * Monta o prompt no formato de chat do Qwen3 (ChatML) e limpa a saída.
 * Funções puras: não dependem do Android, por isso têm testes unitários.
 */
object PromptBuilder {

    const val SYSTEM_PROMPT =
        "Você é o Rud IA, um assistente de inteligência artificial simpático que roda localmente " +
            "no celular do usuário, sem internet. Responda sempre em português do Brasil, " +
            "de forma clara, correta e objetiva. Use emojis com naturalidade para deixar a conversa " +
            "leve e expressiva (normalmente 1 a 3 por resposta, combinando com o assunto, por exemplo " +
            "😊 👍 💡 🚀 📚 🍳), sem exagerar. Se não souber algo, diga que não sabe. " +
            "Quando a mensagem do usuário trouxer [Foto], você recebeu uma descrição automática da " +
            "imagem feita pelo seu leitor de imagens: responda como quem viu a foto, sem dizer que " +
            "recebeu uma descrição e sem inventar detalhes que não aparecem nela."

    /**
     * Pedido para o leitor de imagens (SmolVLM, que entende melhor inglês).
     * "<__media__>" marca onde a foto entra.
     */
    const val VISION_PROMPT =
        "<|im_start|>User:<__media__>Describe this image in 3 to 5 sentences: the main subjects, " +
            "the setting and what is happening. If there is text, quote the most important words." +
            "<end_of_utterance>\nAssistant:"

    /** Tamanho máximo da descrição da foto, em tokens. */
    const val VISION_MAX_TOKENS = 160

    private const val SAFETY_TOKENS = 32

    /** Quantos tokens novos pedir, de acordo com o tamanho do contexto. */
    fun maxNewTokens(contextSize: Int): Int = (contextSize / 3).coerceIn(128, 768)

    /** Texto que a IA principal recebe para uma mensagem (com ou sem foto). */
    fun userContent(text: String, imageDescription: String?): String {
        val question = text.trim()
        if (imageDescription.isNullOrBlank()) return question
        val ask = question.ifEmpty { "Comente esta foto." }
        return "[Foto enviada pelo usuário. Descrição automática da imagem (em inglês): " +
            "${imageDescription.trim()}]\n\n$ask"
    }

    /**
     * Inclui o máximo possível do histórico recente que caiba no contexto,
     * sempre mantendo o prompt de sistema e a mensagem atual.
     */
    fun build(
        history: List<ChatMessage>,
        userText: String,
        contextSize: Int,
        maxNewTokens: Int,
        countTokens: (String) -> Int,
        imageDescription: String? = null
    ): String {
        val head = "<|im_start|>system\n$SYSTEM_PROMPT<|im_end|>\n"
        // O bloco <think> vazio desliga o "modo raciocínio" do Qwen3:
        // a resposta começa direto, sem gastar tokens pensando.
        val tail = "<|im_start|>user\n${userContent(userText, imageDescription)}<|im_end|>\n" +
            "<|im_start|>assistant\n<think>\n\n</think>\n\n"

        val turns = history
            .filter {
                (it.text.isNotBlank() || !it.imageDescription.isNullOrBlank()) &&
                    it.status != MessageStatus.ERROR && it.status != MessageStatus.PENDING
            }
            .map { msg ->
                val role = if (msg.role == Role.USER) "user" else "assistant"
                val content = if (msg.role == Role.USER) userContent(msg.text, msg.imageDescription) else msg.text.trim()
                "<|im_start|>$role\n$content<|im_end|>\n"
            }

        val budget = contextSize - maxNewTokens - SAFETY_TOKENS
        var prompt = head + tail
        if (countTokens(prompt) > budget) return prompt

        for (count in 1..turns.size) {
            val candidate = head + turns.takeLast(count).joinToString("") + tail
            if (countTokens(candidate) > budget) break
            prompt = candidate
        }
        return prompt
    }

    /** Remove blocos de raciocínio e marcas de controle da resposta. */
    fun clean(raw: String): String {
        var text = raw.replace(Regex("(?s)<think>.*?</think>"), "")
        val open = text.indexOf("<think>")
        if (open >= 0) text = text.substring(0, open)
        return text
            .replace("<|im_end|>", "")
            .replace("<|im_start|>", "")
            .replace("\uFFFD", "")
            .trimStart()
    }

    /**
     * Limpa a descrição do leitor de imagens: corta se ele começar a inventar
     * a fala do usuário e tira a última frase se ela ficou pela metade.
     */
    fun cleanDescription(raw: String): String {
        var text = raw
        val cut = text.indexOf("User:")
        if (cut >= 0) text = text.substring(0, cut)
        text = text.replace("<end_of_utterance>", "").replace("\uFFFD", "").trim()
        if (text.isEmpty()) return text
        val last = text.last()
        if (last != '.' && last != '!' && last != '?' && last != '"') {
            val end = text.lastIndexOfAny(charArrayOf('.', '!', '?'))
            if (end > 0) text = text.substring(0, end + 1)
        }
        return text
    }

    /** Título da conversa a partir da primeira mensagem. */
    fun makeTitle(firstMessage: String): String {
        val singleLine = firstMessage.trim().replace(Regex("\\s+"), " ")
        if (singleLine.isEmpty()) return "Nova conversa"
        return if (singleLine.length <= 40) singleLine else singleLine.take(39).trimEnd() + "…"
    }
}
