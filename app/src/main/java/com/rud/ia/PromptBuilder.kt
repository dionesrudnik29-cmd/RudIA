package com.rud.ia

/**
 * Monta o prompt no formato de chat do Qwen3 (ChatML) e limpa a saída.
 * Funções puras: não dependem do Android, por isso têm testes unitários.
 */
object PromptBuilder {

    const val SYSTEM_PROMPT =
        "Você é o Rud IA, um assistente de inteligência artificial que roda localmente " +
            "no celular do usuário, sem internet. Responda sempre em português do Brasil, " +
            "de forma clara, correta e objetiva. Se não souber algo, diga que não sabe."

    private const val SAFETY_TOKENS = 32

    /** Quantos tokens novos pedir, de acordo com o tamanho do contexto. */
    fun maxNewTokens(contextSize: Int): Int = (contextSize / 3).coerceIn(128, 768)

    /**
     * Inclui o máximo possível do histórico recente que caiba no contexto,
     * sempre mantendo o prompt de sistema e a mensagem atual.
     */
    fun build(
        history: List<ChatMessage>,
        userText: String,
        contextSize: Int,
        maxNewTokens: Int,
        countTokens: (String) -> Int
    ): String {
        val head = "<|im_start|>system\n$SYSTEM_PROMPT<|im_end|>\n"
        // O bloco <think> vazio desliga o "modo raciocínio" do Qwen3:
        // a resposta começa direto, sem gastar tokens pensando.
        val tail = "<|im_start|>user\n${userText.trim()}<|im_end|>\n" +
            "<|im_start|>assistant\n<think>\n\n</think>\n\n"

        val turns = history
            .filter { it.text.isNotBlank() && it.status != MessageStatus.ERROR && it.status != MessageStatus.PENDING }
            .map { msg ->
                val role = if (msg.role == Role.USER) "user" else "assistant"
                "<|im_start|>$role\n${msg.text.trim()}<|im_end|>\n"
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
            .trimStart()
    }

    /** Título da conversa a partir da primeira mensagem. */
    fun makeTitle(firstMessage: String): String {
        val singleLine = firstMessage.trim().replace(Regex("\\s+"), " ")
        if (singleLine.isEmpty()) return "Nova conversa"
        return if (singleLine.length <= 40) singleLine else singleLine.take(39).trimEnd() + "…"
    }
}
