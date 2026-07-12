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
     * @return sfid retornado pelo FluidSynth.
     */
    fun loadSF(
        filename: String,
        program: Int = 0
    ): Int {

        try {

            val sfid = fluidsynthLoadSF(
                filename,
                program
            )

            if (sfid < 0) {
                throw IOException("Erro ao carregar $filename")
            }

            return sfid

        } catch (e: IOException) {

            throw RuntimeException(e)

        }

    }

    /**
     * Remove uma SoundFont do FluidSynth.
     */
    fun unloadSF(
        sfid: Int
    ) {

        if (sfid >= 0) {

            fluidsynthUnloadSF(sfid)

        }

    }

    /**
     * Mantido por compatibilidade.
     */
    fun programChange(
        channel: Int,
        bank: Int,
        program: Int
    ) {

        fluidsynthProgramChange(
            channel,
            bank,
            program
        )

    }

    /**
     * Seleciona explicitamente uma SoundFont, banco e preset.
     */
    fun programSelect(
        channel: Int,
        sfid: Int,
        bank: Int,
        program: Int
    ) {

        fluidsynthProgramSelect(
            channel,
            sfid,
            bank,
            program
        )

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

        setChannelVolume(
            0,
            volume
        )

    }

    /**
     * Toca uma nota.
     */
    external fun fluidsynthNoteOn(
        note: Int,
        velocity: Int
    )

    /**
     * Finaliza uma nota.
     */
    external fun fluidsynthNoteOff(
        note: Int
    )

    // =============================================================================================
    // Métodos Privados
    // =============================================================================================

    @Throws(IOException::class)
    private fun copyAssetToTmpFile(filename: String): String {

        context.assets.open(filename).use { input ->

            val tempFilename = "tmp_$filename"

            context.openFileOutput(
                tempFilename,
                Context.MODE_PRIVATE
            ).use { output ->

                val buffer = ByteArray(4096)

                var bytesRead: Int

                while (input.read(buffer).also { bytesRead = it } != -1) {

                    output.write(
                        buffer,
                        0,
                        bytesRead
                    )

                }

            }

            return "${context.filesDir}/$tempFilename"

        }

    }

    // =============================================================================================
    // JNI
    // =============================================================================================

    private external fun fluidsynthInit()

    private external fun fluidsynthLoadSF(
        soundfontPath: String?,
        program: Int
    ): Int

    private external fun fluidsynthUnloadSF(
        sfid: Int
    )

    private external fun fluidsynthFree()

    private external fun fluidsynthCC(
        channel: Int,
        controller: Int,
        value: Int
    )

    private external fun fluidsynthReverb(
        level: Int
    )

    /**
     * API antiga.
     */
    private external fun fluidsynthProgramChange(
        channel: Int,
        bank: Int,
        program: Int
    )

    /**
     * Nova API.
     */
    private external fun fluidsynthProgramSelect(
        channel: Int,
        sfid: Int,
        bank: Int,
        program: Int
    )
}