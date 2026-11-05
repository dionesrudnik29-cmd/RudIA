package com.rud.ia

/**
 * A IA entrega o texto em pedaços de bytes. Um emoji ocupa 4 bytes e pode
 * chegar dividido entre dois pedaços; se decodificarmos no meio dele, aparece
 * um "�" na tela. Estas funções só decodificam caracteres completos.
 */
object Utf8 {

    /** Quantos bytes do início formam caracteres UTF-8 completos. */
    fun completeLength(bytes: ByteArray, length: Int = bytes.size): Int {
        if (length <= 0) return 0
        // Volta no máximo 3 bytes procurando o início do último caractere.
        var i = length - 1
        var back = 0
        while (i >= 0 && back < 4 && (bytes[i].toInt() and 0xC0) == 0x80) {
            i--
            back++
        }
        if (i < 0) return 0
        val lead = bytes[i].toInt() and 0xFF
        val needed = when {
            lead < 0x80 -> 1
            lead in 0xC0..0xDF -> 2
            lead in 0xE0..0xEF -> 3
            lead in 0xF0..0xF7 -> 4
            else -> 1 // byte inválido: deixa o decodificador tratar
        }
        val have = length - i
        return if (have >= needed) length else i
    }

    /** Decodifica só a parte completa. */
    fun decodeComplete(bytes: ByteArray, length: Int = bytes.size): String =
        String(bytes, 0, completeLength(bytes, length), Charsets.UTF_8)
}
