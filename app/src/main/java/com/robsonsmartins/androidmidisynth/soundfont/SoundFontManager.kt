package com.robsonsmartins.androidmidisynth.soundfont

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import androidx.compose.runtime.mutableStateListOf

class SoundFontManager(
    private val context: Context
) {

    // =============================================================================
    // Lista observável pelo Compose
    // =============================================================================

    private val soundFonts = mutableStateListOf<SoundFontInfo>()

    init {

        // SoundFonts fictícias durante o desenvolvimento da interface

        soundFonts.add(
            SoundFontInfo(
                id = 1,
                nome = "Acordeoes.sf2",
                caminho = "",
                carregada = true
            )
        )

        soundFonts.add(
            SoundFontInfo(
                id = 2,
                nome = "Baixos.sf2",
                caminho = "",
                carregada = true
            )
        )

        soundFonts.add(
            SoundFontInfo(
                id = 3,
                nome = "GeneralUser.sf2",
                caminho = "",
                carregada = false
            )
        )

        soundFonts.add(
            SoundFontInfo(
                id = 4,
                nome = "Strings.sf2",
                caminho = "",
                carregada = false
            )
        )
    }

    // =============================================================================
    // Gerenciamento da lista
    // =============================================================================

    fun listar(): List<SoundFontInfo> = soundFonts

    fun adicionar(soundFont: SoundFontInfo) {
        soundFonts.add(soundFont)
    }

    fun remover(soundFont: SoundFontInfo) {
        soundFonts.remove(soundFont)
    }

    fun carregar(id: Int) {
        soundFonts.find { it.id == id }?.carregada = true
    }

    fun descarregar(id: Int) {
        soundFonts.find { it.id == id }?.carregada = false
    }

    fun alternar(id: Int) {

        soundFonts.find { it.id == id }?.let {

            it.carregada = !it.carregada

        }

    }

    fun carregadas(): List<SoundFontInfo> =
        soundFonts.filter { it.carregada }

    fun disponiveis(): List<SoundFontInfo> =
        soundFonts.filter { !it.carregada }

    // =============================================================================
    // Arquivos
    // =============================================================================

    /**
     * Garante que a SoundFont exista no armazenamento interno.
     *
     * Caso ainda não exista, copia automaticamente da pasta assets.
     *
     * Retorna o caminho absoluto do arquivo.
     */
    fun prepareSoundFont(nomeArquivo: String): String {

        val arquivoDestino = File(
            context.filesDir,
            nomeArquivo
        )

        if (!arquivoDestino.exists()) {

            context.assets.open(nomeArquivo).use { input ->

                FileOutputStream(arquivoDestino).use { output ->

                    val buffer = ByteArray(8192)

                    var bytes: Int

                    while (true) {

                        bytes = input.read(buffer)

                        if (bytes <= 0)
                            break

                        output.write(
                            buffer,
                            0,
                            bytes
                        )
                    }

                    output.flush()
                }
            }
        }

        return arquivoDestino.absolutePath
    }

    /**
     * Retorna o arquivo interno correspondente à SoundFont.
     */
    fun getArquivo(nomeArquivo: String): File {

        return File(
            context.filesDir,
            nomeArquivo
        )

    }

}