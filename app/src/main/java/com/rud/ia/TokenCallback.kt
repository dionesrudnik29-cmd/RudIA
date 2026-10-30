package com.rud.ia

/**
 * Chamado pelo código nativo a cada pedaço de texto gerado (bytes UTF-8).
 * Retorne false para interromper a geração.
 */
fun interface TokenCallback {
    fun onToken(bytes: ByteArray): Boolean
}
