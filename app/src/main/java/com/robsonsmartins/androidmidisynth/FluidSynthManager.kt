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
        val file = java.io.File(filename)
        if (!file.exists()) {
            android.util.Log.e("FluidSynthManager", "Falha ao carregar SoundFont: arquivo não existe. Caminho=$filename")
            return -1
        }
        if (!file.isFile) {
            android.util.Log.e("FluidSynthManager", "Falha ao carregar SoundFont: o caminho não é um arquivo regular. Caminho=$filename")
            return -1
        }
        if (file.length() <= 0) {
            android.util.Log.e("FluidSynthManager", "Falha ao carregar SoundFont: arquivo vazio (0 bytes). Caminho=$filename")
            return -1
        }

        android.util.Log.d("FluidSynthManager", "Iniciando carregamento da SoundFont: arquivo=${file.name}, caminho=$filename, tamanho=${file.length()} bytes")

        try {
            val sfid = fluidsynthLoadSF(
                filename,
                program
            )

            android.util.Log.d("FluidSynthManager", "Resultado fluidsynthLoadSF: arquivo=${file.name}, sfid=$sfid")

            if (sfid < 0) {
                android.util.Log.e("FluidSynthManager", "Erro ao carregar SoundFont no sintetizador: sfid=$sfid, caminho=$filename")
                return -1
            }

            return sfid

        } catch (e: Exception) {
            android.util.Log.e("FluidSynthManager", "Exceção ao carregar SoundFont: caminho=$filename", e)
            return -1
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

    fun sendCC(
        channel: Int,
        controller: Int,
        value: Int
    ) {

        fluidsynthCC(
            channel,
            controller,
            value
        )

    }

    fun setVolume(volume: Int) {

        setChannelVolume(
            0,
            volume
        )

    }

    fun listPresets(
        sfid: Int
    ): List<com.robsonsmartins.androidmidisynth.soundfont.PresetInfo> {

        return fluidsynthListPresets(sfid).toList()

    }

    external fun criarPresetTeste(): com.robsonsmartins.androidmidisynth.soundfont.PresetInfo

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

    external fun fluidsynthListPresets(
        sfid: Int
    ): Array<com.robsonsmartins.androidmidisynth.soundfont.PresetInfo>


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