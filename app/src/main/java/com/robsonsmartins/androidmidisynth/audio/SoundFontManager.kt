package com.robsonsmartins.androidmidisynth.audio

import android.content.Context
import java.io.File
import java.io.FileOutputStream

class SoundFontManager(
    private val context: Context
) {

    /**
     * Garante que a SoundFont exista no armazenamento interno.
     *
     * Se ainda não existir, copia do diretório assets.
     *
     * Retorna o caminho absoluto do arquivo.
     */
    fun prepareSoundFont(nomeArquivo: String): String {

        val arquivoDestino = File(context.filesDir, nomeArquivo)

        if (!arquivoDestino.exists()) {

            context.assets.open(nomeArquivo).use { input ->

                FileOutputStream(arquivoDestino).use { output ->

                    val buffer = ByteArray(8192)

                    var bytes: Int

                    while (input.read(buffer).also { bytes = it } != -1) {
                        output.write(buffer, 0, bytes)
                    }

                    output.flush()
                }
            }
        }

        return arquivoDestino.absolutePath
    }
}