
package com.robsonsmartins.androidmidisynth.soundfont

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import com.robsonsmartins.androidmidisynth.audio.SynthController
import com.robsonsmartins.androidmidisynth.session.SessionSoundFont
import java.io.File
import java.io.FileOutputStream

class SoundFontManager(
    private val context: Context,
    private val synthController: SynthController
) {

    companion object {
        private const val TAG = "SoundFontManager"
        private const val DEBUG_TAG = "SoundFontDebug"
    }

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

    fun getSoundFont(id: Int): SoundFontInfo? {
        return soundFonts.find { it.id == id }
    }

    fun getSoundFontBySfid(sfid: Int): SoundFontInfo? {
        return soundFonts.find { it.sfid == sfid }
    }

    fun localizarPreset(
        soundFont: SoundFontInfo,
        bank: Int,
        program: Int
    ): PresetInfo? {
        return soundFont.presets.firstOrNull {
            it.bank == bank && it.program == program
        }
    }

    fun localizarInstrumento(
        soundFontId: Int,
        bank: Int,
        program: Int
    ): Pair<SoundFontInfo, PresetInfo>? {

        val soundFont = getSoundFont(soundFontId)

        if (soundFont == null) {
            Log.d(TAG, "SoundFont $soundFontId não encontrada")
            return null
        }

        Log.d(
            TAG,
            "SoundFont '${soundFont.nome}' carregada " +
                    "sfid=${soundFont.sfid} presets=${soundFont.presets.size}"
        )

        val preset = localizarPreset(soundFont, bank, program)

        if (preset == null) {
            Log.d(
                TAG,
                "Preset não encontrado. bank=$bank program=$program"
            )
            return null
        }

        Log.d(TAG, "Preset encontrado: ${preset.nome}")

        return soundFont to preset
    }

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

    // =============================================================================
    // Carregamento no FluidSynth
    // =============================================================================

    fun carregar(id: Int) {
        Log.d(DEBUG_TAG, "INICIO carregar id=$id")

        val soundFont = soundFonts.find { it.id == id }
            ?: run {
                Log.e(DEBUG_TAG, "SoundFont id=$id não encontrada")
                return
            }

        Log.d(
            DEBUG_TAG,
            "SoundFont encontrada: id=${soundFont.id} " +
                    "nome=${soundFont.nome} caminho=${soundFont.caminho} " +
                    "carregada=${soundFont.carregada}"
        )

        Log.d(DEBUG_TAG, "ANTES synthController.carregarSoundFont")

        val sucesso = synthController.carregarSoundFont(soundFont)

        Log.d(
            DEBUG_TAG,
            "DEPOIS synthController.carregarSoundFont: sucesso=$sucesso"
        )

        soundFont.carregada = sucesso

        Log.d(
            DEBUG_TAG,
            "SoundFont carregada status = ${soundFont.carregada}"
        )
    }

    fun descarregar(id: Int) {
        val soundFont = soundFonts.find { it.id == id } ?: return

        if (soundFont.sfid >= 0) {
            synthController.descarregarSoundFont(soundFont)
        }

        soundFont.carregada = false
        soundFont.sfid = -1
    }

    fun alternar(id: Int) {
        val soundFont = soundFonts.find { it.id == id } ?: return

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
            .forEach { soundFont ->
                val sucesso = synthController.carregarSoundFont(soundFont)
                soundFont.carregada = sucesso
            }
    }

    // =============================================================================
    // Importação de arquivos
    // =============================================================================

    /**
     * Copia uma SoundFont selecionada pelo usuário para o armazenamento privado.
     *
     * Se existir um cadastro cujo arquivo desapareceu, tenta restaurar o arquivo
     * no caminho já cadastrado, preservando o ID, o nome e as referências da sessão.
     *
     * @return true se a importação ou recuperação foi concluída.
     */
    fun importarSoundFont(uri: Uri): Boolean {
        val nomeArquivo = obterNomeArquivo(uri)

        if (nomeArquivo.isNullOrBlank()) {
            Log.e(TAG, "Importação recusada: não foi possível obter o nome do arquivo.")
            return false
        }

        if (!nomeArquivo.endsWith(".sf2", ignoreCase = true)) {
            Log.e(
                TAG,
                "Importação recusada: extensão inválida. Nome=$nomeArquivo"
            )
            return false
        }

        Log.d(
            TAG,
            "Solicitação de importação: nome=$nomeArquivo uri=$uri"
        )

        // Se o nome já está cadastrado, verificar se o arquivo ainda existe.
        val existente = soundFonts.firstOrNull {
            it.nome.equals(nomeArquivo, ignoreCase = true)
        }

        if (existente != null) {
            val arquivoExistente = File(existente.caminho)

            if (arquivoExistente.isFile && arquivoExistente.length() > 0L) {
                Log.w(
                    TAG,
                    "Importação recusada: já existe um cadastro válido para " +
                            "'${existente.nome}', caminho=${existente.caminho}"
                )
                return false
            }

            Log.w(
                TAG,
                "Cadastro antigo encontrado sem arquivo válido. " +
                        "Tentando recuperar: id=${existente.id}, " +
                        "nome=${existente.nome}, caminho=${existente.caminho}"
            )

            val recuperado = copiarUriParaArquivo(
                uri = uri,
                destino = arquivoExistente
            )

            if (!recuperado) {
                Log.e(
                    TAG,
                    "Falha ao recuperar a SoundFont '${existente.nome}'."
                )
                return false
            }

            // O cadastro original é mantido para preservar o ID e as referências.
            existente.carregada = false
            existente.sfid = -1
            limparPresets(existente)

            Log.i(
                TAG,
                "SoundFont recuperada: id=${existente.id}, " +
                        "nome=${existente.nome}, caminho=${existente.caminho}"
            )

            return true
        }

        // Não há cadastro com esse nome: criar uma nova entrada.
        val arquivoDestino = File(context.filesDir, nomeArquivo)

        if (!copiarUriParaArquivo(uri, arquivoDestino)) {
            Log.e(
                TAG,
                "Importação falhou ao copiar '$nomeArquivo' para " +
                        arquivoDestino.absolutePath
            )
            return false
        }

        val novoId = (soundFonts.maxOfOrNull { it.id } ?: 0) + 1

        soundFonts.add(
            SoundFontInfo(
                id = novoId,
                nome = nomeArquivo,
                caminho = arquivoDestino.absolutePath,
                carregada = false
            )
        )

        Log.i(
            TAG,
            "SoundFont importada: id=$novoId, nome=$nomeArquivo, " +
                    "tamanho=${arquivoDestino.length()} bytes, " +
                    "caminho=${arquivoDestino.absolutePath}"
        )

        return true
    }

    /**
     * Obtém o nome do arquivo fornecido pelo seletor de documentos do Android.
     */
    private fun obterNomeArquivo(uri: Uri): String? {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    )

                    if (index >= 0) cursor.getString(index) else null
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao consultar o nome do arquivo: uri=$uri", e)
            null
        }
    }

    /**
     * Copia o conteúdo para um arquivo temporário e só o instala no destino
     * depois de confirmar que a cópia não está vazia.
     */
    private fun copiarUriParaArquivo(
        uri: Uri,
        destino: File
    ): Boolean {

        val temporario = File(
            context.filesDir,
            ".soundfont_import_${System.nanoTime()}.tmp"
        )

        try {
            val input = context.contentResolver.openInputStream(uri)

            if (input == null) {
                Log.e(
                    TAG,
                    "Não foi possível abrir o arquivo selecionado: uri=$uri"
                )
                return false
            }

            input.use { stream ->
                FileOutputStream(temporario).use { output ->
                    stream.copyTo(output)
                    output.flush()
                }
            }

            if (!temporario.isFile || temporario.length() <= 0L) {
                Log.e(
                    TAG,
                    "A cópia temporária está vazia ou inválida: uri=$uri"
                )
                return false
            }

            destino.parentFile?.let { pasta ->
                if (!pasta.exists() && !pasta.mkdirs()) {
                    Log.e(
                        TAG,
                        "Não foi possível criar a pasta de destino: ${pasta.absolutePath}"
                    )
                    return false
                }
            }

            /*
             * Primeiro tenta renomear o arquivo temporário. Se não for possível,
             * usa uma cópia com substituição e confirma o resultado.
             */
            if (destino.exists() && !destino.delete()) {
                Log.e(
                    TAG,
                    "Não foi possível substituir o arquivo de destino: " +
                            destino.absolutePath
                )
                return false
            }

            val renomeado = temporario.renameTo(destino)

            if (!renomeado) {
                temporario.copyTo(destino, overwrite = true)
            }

            if (!destino.isFile || destino.length() <= 0L) {
                Log.e(
                    TAG,
                    "Arquivo de destino inválido após a cópia: " +
                            destino.absolutePath
                )
                return false
            }

            Log.d(
                TAG,
                "Cópia concluída: destino=${destino.absolutePath}, " +
                        "tamanho=${destino.length()} bytes"
            )

            return true
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Erro ao copiar SoundFont. uri=$uri destino=${destino.absolutePath}",
                e
            )
            return false
        } finally {
            if (temporario.exists()) {
                temporario.delete()
            }
        }
    }

    /**
     * Retorna o arquivo interno correspondente à SoundFont.
     */
    fun getArquivo(nomeArquivo: String): File {
        return File(context.filesDir, nomeArquivo)
    }

    private fun adicionarSoundFont(item: SessionSoundFont) {
        soundFonts.add(
            SoundFontInfo(
                id = item.id,
                nome = item.nome,
                caminho = File(
                    context.filesDir,
                    item.arquivo
                ).absolutePath,
                carregada = item.carregada,
                sfid = -1
            )
        )
    }

    fun aplicarSessao(
        sessaoSoundFonts: List<SessionSoundFont>
    ): Boolean {
        if (sessaoSoundFonts.isEmpty()) {
            return false
        }

        soundFonts.clear()

        sessaoSoundFonts.forEach { item ->
            adicionarSoundFont(item)
        }

        return true
    }

    fun aplicarBiblioteca() {
        soundFonts.forEach { soundFont ->
            if (soundFont.carregada) {
                val sucesso = synthController.carregarSoundFont(soundFont)
                soundFont.carregada = sucesso
            } else {
                if (soundFont.sfid >= 0) {
                    synthController.descarregarSoundFont(soundFont)
                }

                soundFont.carregada = false
                soundFont.sfid = -1
            }
        }
    }

    fun sincronizarBiblioteca() {
        Log.d(TAG, "Sincronizando ${soundFonts.size} SoundFonts")

        soundFonts.forEach { soundFont ->
            if (soundFont.carregada) {
                val sucesso = synthController.carregarSoundFont(soundFont)
                soundFont.carregada = sucesso
            } else {
                if (soundFont.sfid >= 0) {
                    synthController.descarregarSoundFont(soundFont)
                }

                soundFont.carregada = false
                soundFont.sfid = -1
            }
        }
    }

    // =============================================================================
    // Exclusão
    // =============================================================================

    fun podeExcluir(
        soundFont: SoundFontInfo,
        estaEmUso: (SoundFontInfo) -> Boolean
    ): Boolean {
        if (soundFont.carregada) return false
        if (estaEmUso(soundFont)) return false

        return true
    }

    fun excluir(
        id: Int,
        estaEmUso: (SoundFontInfo) -> Boolean
    ): Boolean {

        val soundFont = soundFonts.find { it.id == id } ?: return false

        if (!podeExcluir(soundFont, estaEmUso)) {
            return false
        }

        val arquivo = File(soundFont.caminho)

        // Se o arquivo já desapareceu, ainda é possível remover o cadastro,
        // desde que não esteja carregado nem em uso.
        val excluiu = try {
            !arquivo.exists() || arquivo.delete()
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Erro ao excluir arquivo da SoundFont: ${arquivo.absolutePath}",
                e
            )
            false
        }

        if (!excluiu) {
            return false
        }

        soundFonts.remove(soundFont)

        Log.i(TAG, "Cadastro da SoundFont removido: ${soundFont.nome}")

        return true
    }
}