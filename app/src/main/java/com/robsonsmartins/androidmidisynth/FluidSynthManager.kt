/*
 * Copyright (c) 2024 Robson Martins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

// -----------------------------------------------------------------------------------------------
/**
 * @file FluidSynthManager.kt
 * @brief Kotlin implementation of FluidSynthManager.
 *
 * @author Robson Martins (https://www.robsonmartins.com)
 */
// -----------------------------------------------------------------------------------------------

package com.robsonsmartins.androidmidisynth

import android.content.Context
import java.io.IOException
import android.util.Log

/**
 * Encapsula toda a comunicação entre o Kotlin e a biblioteca FluidSynth.
 *
 * Esta classe é responsável apenas por controlar o sintetizador.
 * Toda a lógica de canais, mixer e presets ficará fora dela.
 */
class FluidSynthManager(private val context: Context) {

    // =============================================================================================
    // Propriedades
    // =============================================================================================

    /** Caminho temporário da SoundFont carregada. */
    private var soundFontPath: String? = null

    // =============================================================================================
    // Inicialização
    // =============================================================================================

    init {
        fluidsynthInit()
    }

    /**
     * Libera os recursos utilizados pelo FluidSynth.
     */
    fun finalize() {
        fluidsynthFree()
    }

    // =============================================================================================
    // API Pública
    // =============================================================================================

    /**
     * Carrega uma SoundFont.
     *
     * @param filename Nome do arquivo .sf2 localizado na pasta assets.
     * @param program Programa MIDI inicial.
     */
    fun loadSF(filename: String, program: Int = 0) {
        try {
            if (fluidsynthLoadSF(filename, program) < 0) {
                throw IOException("Erro ao carregar $filename")
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    /**
     * Ajusta o volume geral do sintetizador.
     */
    fun setChannelVolume(
        channel: Int,
        volume: Int
    ) {
        fluidsynthCC(
            channel,
            7,
            volume
        )

    }

    fun setVolume(volume: Int) {
        setChannelVolume(0, volume)
    }
    /**
     * Toca uma nota.
     */
    external fun fluidsynthNoteOn(note: Int, velocity: Int)

    /**
     * Finaliza uma nota.
     */
    external fun fluidsynthNoteOff(note: Int)

    // =============================================================================================
    // Métodos Privados
    // =============================================================================================

    /**
     * Copia uma SoundFont da pasta assets para a área privada do aplicativo.
     */
    @Throws(IOException::class)
    private fun copyAssetToTmpFile(filename: String): String {

        context.assets.open(filename).use { input ->

            val tempFilename = "tmp_$filename"

            context.openFileOutput(tempFilename, Context.MODE_PRIVATE).use { output ->

                val buffer = ByteArray(4096)

                var bytesRead: Int

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
            }

            return "${context.filesDir}/$tempFilename"
        }
    }

    // =============================================================================================
    // JNI
    // =============================================================================================

    /**
     * Inicializa o FluidSynth.
     */
    private external fun fluidsynthInit()

    /**
     * Carrega uma SoundFont.
     */
    private external fun fluidsynthLoadSF(
        soundfontPath: String?,
        program: Int
    ): Int

    /**
     * Libera o FluidSynth.
     */
    private external fun fluidsynthFree()

    /**
     * Envia um Control Change.
     */
    private external fun fluidsynthCC(
        channel: Int,
        controller: Int,
        value: Int
    )

    /**
     * Ajusta o nível de Reverb.
     */
    private external fun fluidsynthReverb(
        level: Int
    )
}