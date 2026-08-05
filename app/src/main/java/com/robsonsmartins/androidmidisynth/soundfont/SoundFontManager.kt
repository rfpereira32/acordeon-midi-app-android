package com.robsonsmartins.androidmidisynth.soundfont

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import java.io.File
import java.io.FileOutputStream
import com.robsonsmartins.androidmidisynth.audio.SynthController
import com.robsonsmartins.androidmidisynth.session.SessionManager
import com.robsonsmartins.androidmidisynth.session.SessionState
import com.robsonsmartins.androidmidisynth.session.SessionSoundFont

class SoundFontManager(

    private val context: Context,

    private val synthController: SynthController

){

    // =============================================================================
    // Lista observável pelo Compose
    // =============================================================================

    private val soundFonts = mutableStateListOf<SoundFontInfo>()
    private val sessionManager = SessionManager(context)

    init {

        if (!restaurarSessao()) {

            criarSessaoInicial()

        }

    }

    private fun salvarSessao() {

        val session = SessionState()

        soundFonts.forEach { soundFont ->

            session.soundFonts.add(

                SessionSoundFont(

                    id = soundFont.id,

                    nome = soundFont.nome,

                    arquivo = File(soundFont.caminho).name,

                    carregada = soundFont.carregada

                )

            )

        }

        sessionManager.salvar(session)

    }

    private fun restaurarSessao(): Boolean {

        val session = sessionManager.carregar()

        if (session.soundFonts.isEmpty())
            return false

        soundFonts.clear()

        session.soundFonts.forEach { item ->

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

        return true

    }

    private fun criarSessaoInicial() {

        val caminhoInicial =
            prepareSoundFont("AcordeonGiulietti.sf2")

        soundFonts.add(

            SoundFontInfo(

                id = 1,

                nome = "AcordeonGiulietti.sf2",

                caminho = caminhoInicial,

                carregada = true

            )

        )

        salvarSessao()

    }

    // =============================================================================
    // Gerenciamento da lista
    // =============================================================================

    fun listar(): List<SoundFontInfo> = soundFonts

    /**
     * Retorna a SoundFont inicial do aplicativo.
     *
     * Temporariamente é a Giulietti.
     * No futuro poderá retornar null.
     */
    fun soundFontInicial(): SoundFontInfo? {

        return soundFonts.firstOrNull()

    }

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

        salvarSessao()

    }

    fun remover(soundFont: SoundFontInfo) {

        soundFonts.remove(soundFont)

        salvarSessao()

    }

    fun carregar(id: Int) {

        val soundFont =
            soundFonts.find { it.id == id }
                ?: return

        if (!soundFont.carregada) {
            soundFont.carregada = true
        }

        synthController.carregarSoundFont(soundFont)

        salvarSessao()

    }

    fun descarregar(id: Int) {

        val soundFont =
            soundFonts.find { it.id == id }
                ?: return

        synthController.descarregarSoundFont(soundFont)

        soundFont.carregada = false

        salvarSessao()

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
     * Garante que a SoundFont exista no armazenamento interno.
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
        salvarSessao()
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

    fun aplicarBiblioteca() {

        soundFonts.forEach { soundFont ->

            if (soundFont.carregada) {

                synthController.carregarSoundFont(soundFont)

            } else {

                synthController.descarregarSoundFont(soundFont)

            }

        }

        salvarSessao()

    }

    fun sincronizarBiblioteca() {

        soundFonts.forEach { soundFont ->

            if (soundFont.carregada) {

                synthController.carregarSoundFont(soundFont)

            } else {

                synthController.descarregarSoundFont(soundFont)

            }

        }

        salvarSessao()

    }
}