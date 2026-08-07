package com.robsonsmartins.androidmidisynth.soundfont

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import java.io.File
import java.io.FileOutputStream
import com.robsonsmartins.androidmidisynth.audio.SynthController
import com.robsonsmartins.androidmidisynth.session.SessionSoundFont
import android.util.Log

class SoundFontManager(

    private val context: Context,

    private val synthController: SynthController

){

    // =============================================================================
    // Lista observável pelo Compose
    // =============================================================================

    private val soundFonts = mutableStateListOf<SoundFontInfo>()

    init {

        criarSessaoInicial()

    }

    private fun criarSessaoInicial() {

        soundFonts.clear()

    }

    // =============================================================================
    // Gerenciamento da lista
    // =============================================================================

    fun listar(): List<SoundFontInfo> = soundFonts

    /**
     * Retorna uma SoundFont pelo ID.
     */
    fun getSoundFont(id: Int): SoundFontInfo? {

        return soundFonts.find { it.id == id }

    }

    /**
     * Retorna uma SoundFont pelo sfid do FluidSynth.
     */
    fun getSoundFontBySfid(sfid: Int): SoundFontInfo? {

        return soundFonts.find { it.sfid == sfid }

    }

    /**
     * Localiza um preset dentro de uma SoundFont.
     */
    fun localizarPreset(
        soundFont: SoundFontInfo,
        bank: Int,
        program: Int
    ): PresetInfo? {

        return soundFont.presets.firstOrNull {

            it.bank == bank &&
                    it.program == program

        }

    }

    /**
     * Localiza uma SoundFont e um preset.
     */
    fun localizarInstrumento(
        soundFontId: Int,
        bank: Int,
        program: Int
    ): Pair<SoundFontInfo, PresetInfo>? {

        val soundFont = getSoundFont(soundFontId)

        if (soundFont == null) {

            Log.d(
                "SoundFontManager",
                "SoundFont $soundFontId não encontrada"
            )

            return null

        }

        Log.d(
            "SoundFontManager",
            "SoundFont '${soundFont.nome}' carregada " +
                    "sfid=${soundFont.sfid} " +
                    "presets=${soundFont.presets.size}"
        )

        val preset = localizarPreset(
            soundFont,
            bank,
            program
        )

        if (preset == null) {

            Log.d(
                "SoundFontManager",
                "Preset não encontrado. " +
                        "bank=$bank program=$program"
            )

            return null

        }

        Log.d(
            "SoundFontManager",
            "Preset encontrado: ${preset.nome}"
        )

        return soundFont to preset

    }

    /**
     * Limpa todas as informações carregadas
     * de uma SoundFont.
     */
    fun limparPresets(soundFont: SoundFontInfo) {

        soundFont.presets.clear()

        soundFont.quantidadePresets = 0

        soundFont.presetSelecionado = null

    }

    fun adicionar(soundFont: SoundFontInfo) {

        soundFonts.add(soundFont)

    }

    fun remover(soundFont: SoundFontInfo) {

        soundFonts.remove(soundFont)

    }

    fun carregar(id: Int) {

        val soundFont =
            soundFonts.find { it.id == id }
                ?: return

        if (!soundFont.carregada) {
            soundFont.carregada = true
        }

        synthController.carregarSoundFont(soundFont)

    }

    fun descarregar(id: Int) {

        val soundFont =
            soundFonts.find { it.id == id }
                ?: return

        synthController.descarregarSoundFont(soundFont)

        soundFont.carregada = false

    }

    fun alternar(id: Int) {

        val soundFont =
            soundFonts.find { it.id == id }
                ?: return

        if (soundFont.carregada) {

            descarregar(id)

        } else {

            carregar(id)

        }

    }

    fun carregadas(): List<SoundFontInfo> =
        soundFonts.filter { it.carregada }

    fun disponiveis(): List<SoundFontInfo> =
        soundFonts.filter { !it.carregada }

    fun restaurarBiblioteca() {

        soundFonts
            .filter { it.carregada }
            .forEach {

                synthController.carregarSoundFont(it)

            }

    }

    // =============================================================================
    // Arquivos
    // =============================================================================

    /**
     * Importa uma SoundFont escolhida pelo usuário.
     *
     * @return true se a SoundFont foi importada.
     *         false se ela já existia.
     */
    fun importarSoundFont(uri: Uri): Boolean {

        val resolver = context.contentResolver

        var nomeArquivo = "SoundFont_${System.currentTimeMillis()}.sf2"

        resolver.query(
            uri,
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->

            if (cursor.moveToFirst()) {

                val index =
                    cursor.getColumnIndex(
                        android.provider.OpenableColumns.DISPLAY_NAME
                    )

                if (index >= 0) {
                    nomeArquivo = cursor.getString(index)
                }

            }

        }

        // Evita importar duas vezes a mesma SoundFont
        if (
            soundFonts.any {
                it.nome.equals(
                    nomeArquivo,
                    ignoreCase = true
                )
            }
        ) {
            return false
        }

        val arquivoDestino = File(
            context.filesDir,
            nomeArquivo
        )

        resolver.openInputStream(uri)?.use { input ->

            FileOutputStream(arquivoDestino).use { output ->

                input.copyTo(output)

            }

        }

        val novoId =
            (soundFonts.maxOfOrNull { it.id } ?: 0) + 1

        soundFonts.add(

            SoundFontInfo(

                id = novoId,

                nome = nomeArquivo,

                caminho = arquivoDestino.absolutePath,

                carregada = false

            )

        )
        return true

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

    private fun adicionarSoundFont(
        item: SessionSoundFont
    ) {

        soundFonts.add(

            SoundFontInfo(

                id = item.id,

                nome = item.nome,

                caminho = File(
                    context.filesDir,
                    item.arquivo
                ).absolutePath,

                carregada = item.carregada

            )

        )

    }

    fun aplicarSessao(
        sessaoSoundFonts: List<SessionSoundFont>
    ): Boolean {

        if (sessaoSoundFonts.isEmpty())
            return false

        soundFonts.clear()

        sessaoSoundFonts.forEach { item ->

            adicionarSoundFont(item)

        }

        return true

    }

    fun aplicarBiblioteca() {

        soundFonts.forEach { soundFont ->

            if (soundFont.carregada) {

                synthController.carregarSoundFont(soundFont)

            } else {

                synthController.descarregarSoundFont(soundFont)

            }

        }

    }

    fun sincronizarBiblioteca() {

        Log.d(
            "SoundFontManager",
            "Sincronizando ${soundFonts.size} SoundFonts"
        )

        soundFonts.forEach { soundFont ->

            if (soundFont.carregada) {

                synthController.carregarSoundFont(soundFont)

            } else {

                synthController.descarregarSoundFont(soundFont)

            }

        }

    }

    fun podeExcluir(
        soundFont: SoundFontInfo,
        estaEmUso: (SoundFontInfo) -> Boolean
    ): Boolean {

        if (soundFont.carregada)
            return false

        if (estaEmUso(soundFont))
            return false

        return true

    }

    fun excluir(
        id: Int,
        estaEmUso: (SoundFontInfo) -> Boolean
    ): Boolean {

        val soundFont =
            soundFonts.find { it.id == id }
                ?: return false

        if (!podeExcluir(soundFont, estaEmUso))
            return false

        try {

            File(soundFont.caminho).delete()

        } catch (_: Exception) {

            return false

        }

        soundFonts.remove(soundFont)

        return true

    }
}