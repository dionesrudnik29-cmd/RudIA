package com.rud.ia

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileOutputStream

class RgbImage(val pixels: ByteArray, val width: Int, val height: Int)

/** Prepara as fotos enviadas: reduz, corrige a rotação e salva no app. */
object ImageTools {

    fun imagesDir(context: Context): File = File(context.filesDir, "chat_images").apply { mkdirs() }

    /**
     * Abre a foto escolhida, reduz para no máximo [ImageSizing.MAX_SIDE] e
     * grava como JPEG em [dest]. Bloqueante: chamar fora da thread principal.
     */
    fun prepare(context: Context, uri: Uri, dest: File) {
        val bitmap = decode(context, uri) ?: throw ModelException("Não foi possível abrir essa imagem.")
        try {
            dest.parentFile?.mkdirs()
            FileOutputStream(dest).use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)) {
                    throw ModelException("Não foi possível salvar a imagem.")
                }
            }
        } finally {
            bitmap.recycle()
        }
    }

    /** Lê o JPEG salvo e devolve os pixels em RGB para a IA. */
    fun loadRgb(file: File): RgbImage {
        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
            ?: throw ModelException("A imagem desta mensagem não foi encontrada.")
        try {
            val w = bitmap.width
            val h = bitmap.height
            val argb = IntArray(w * h)
            bitmap.getPixels(argb, 0, w, 0, 0, w, h)
            val rgb = ByteArray(w * h * 3)
            var j = 0
            for (p in argb) {
                rgb[j++] = (p shr 16 and 0xFF).toByte()
                rgb[j++] = (p shr 8 and 0xFF).toByte()
                rgb[j++] = (p and 0xFF).toByte()
            }
            return RgbImage(rgb, w, h)
        } finally {
            bitmap.recycle()
        }
    }

    /** Miniatura para mostrar na conversa. */
    fun loadPreview(file: File, maxSide: Int = 720): Bitmap? {
        if (!file.isFile) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = ImageSizing.sampleSize(bounds.outWidth, bounds.outHeight, maxSide)
        }
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    private fun decode(context: Context, uri: Uri): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder já corrige a rotação das fotos da câmera.
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val (w, h) = ImageSizing.targetSize(info.size.width, info.size.height)
                decoder.setTargetSize(w, h)
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
            }
        } else {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val options = BitmapFactory.Options().apply {
                inSampleSize = ImageSizing.sampleSize(bounds.outWidth, bounds.outHeight)
            }
            val raw = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return null
            val (w, h) = ImageSizing.targetSize(raw.width, raw.height)
            if (w == raw.width && h == raw.height) raw
            else Bitmap.createScaledBitmap(raw, w, h, true).also { if (it !== raw) raw.recycle() }
        }
    }
}
