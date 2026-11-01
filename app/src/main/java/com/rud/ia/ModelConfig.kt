package com.rud.ia

/**
 * Regras de desempenho do modelo. Funções puras (sem Android) para ter testes.
 */
object ModelConfig {

    private const val GB = 1024L * 1024L * 1024L

    /**
     * Tamanho do contexto (memória da conversa) conforme a RAM livre.
     * No máximo 2048: com mais que isso cada resposta demora muito para
     * começar num Snapdragon 685, porque o histórico inteiro é relido.
     */
    fun contextSizeFor(availableBytes: Long): Int = when {
        availableBytes >= 2 * GB -> 2048
        availableBytes >= 1 * GB -> 1536
        else -> 1024
    }

    /**
     * Usa só os núcleos rápidos. O Snapdragon 685 tem 4 rápidos + 4 lentos;
     * usar os 8 deixa a geração MAIS lenta, porque os rápidos esperam os lentos.
     */
    fun threadsFor(cores: Int): Int = (cores / 2).coerceIn(2, 4)
}
