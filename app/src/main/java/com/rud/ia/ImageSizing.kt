package com.rud.ia

/** Regras de tamanho das fotos enviadas. Funções puras, com testes. */
object ImageSizing {

    /**
     * Lado maior da foto usada pela IA. Fotos maiores deixam a análise muito
     * mais lenta no Snapdragon 685 sem melhorar a resposta.
     */
    const val MAX_SIDE = 768

    /** Tamanho final mantendo a proporção; nunca aumenta a foto. */
    fun targetSize(width: Int, height: Int, maxSide: Int = MAX_SIDE): Pair<Int, Int> {
        if (width <= 0 || height <= 0) return 0 to 0
        val longest = maxOf(width, height)
        if (longest <= maxSide) return width to height
        val scale = maxSide.toDouble() / longest
        val w = (width * scale).toInt().coerceAtLeast(1)
        val h = (height * scale).toInt().coerceAtLeast(1)
        return w to h
    }

    /** Fator de redução (potência de 2) para abrir fotos grandes sem estourar a memória. */
    fun sampleSize(width: Int, height: Int, maxSide: Int = MAX_SIDE): Int {
        var sample = 1
        while (maxOf(width, height) / (sample * 2) >= maxSide) sample *= 2
        return sample
    }
}
