package com.robsonsmartins.androidmidisynth.session

import android.util.Log
import com.robsonsmartins.androidmidisynth.core.MixerState
import com.robsonsmartins.androidmidisynth.soundfont.PresetInfo
import com.robsonsmartins.androidmidisynth.soundfont.SoundFontInfo
import com.robsonsmartins.androidmidisynth.soundfont.SoundFontManager

/**
 * Coordena a persistência da sessão do aplicativo.
 *
 * Reúne o estado da biblioteca de SoundFonts e do mixer,
 * delegando ao SessionManager a gravação e leitura do JSON.
 */
class SessionCoordinator(
    private val sessionManager: SessionManager,
    private val soundFontManager: SoundFontManager,
    private val mixerState: MixerState,
    private val restaurarCanal: (Int, SoundFontInfo, PresetInfo) -> Unit,
    private val restaurarVolume: (Int, Int, Boolean) -> Unit
) {

    fun salvar() {
        Log.d(
            "SessionCoordinator",
            "Salvando sessão. MixerState=${System.identityHashCode(mixerState)}"
        )

        val session = SessionState()

        // Biblioteca de SoundFonts
        soundFontManager.listar().forEach { soundFont ->
            session.soundFonts.add(
                SessionSoundFont(
                    id = soundFont.id,
                    nome = soundFont.nome,
                    arquivo = soundFont.getArquivo().name,
                    carregada = soundFont.carregada
                )
            )
        }

        // Configuração do mixer, incluindo SoundFont, banco e preset de cada canal
        session.mixer = mixerState.exportarConfiguracao()

        sessionManager.salvar(session)
    }

    fun restaurar() {
        val session = sessionManager.carregar()

        Log.d(
            "SessionCoordinator",
            "Restaurando sessão: " +
                    "${session.soundFonts.size} SoundFonts"
        )

        // 1. Reconstrói a biblioteca com os IDs salvos.
        soundFontManager.aplicarSessao(session.soundFonts)

        // 2. Recarrega as SoundFonts que estavam carregadas na última sessão.
        // Isso também permite reconstruir a lista de presets antes de restaurar
        // as seleções dos canais.
        session.soundFonts
            .filter { it.carregada }
            .forEach { soundFontSalva ->
                Log.d(
                    "SessionCoordinator",
                    "Recarregando SoundFont: " +
                            "id=${soundFontSalva.id}, " +
                            "nome=${soundFontSalva.nome}"
                )

                soundFontManager.carregar(soundFontSalva.id)
            }

        // 3. Restaura a configuração do mixer após carregar a biblioteca.
        mixerState.aplicarConfiguracao(session.mixer)

        Log.d(
            "SessionCoordinator",
            "Configuração do mixer restaurada"
        )
    }

    fun restaurarInstrumentos() {
        // Restaura o preset selecionado em cada canal.
        mixerState.channels.forEachIndexed { index, channel ->

            if (channel.soundFontId < 0) {
                return@forEachIndexed
            }

            val instrumento = soundFontManager.localizarInstrumento(
                channel.soundFontId,
                channel.bankMsb,
                channel.program
            )

            if (instrumento == null) {
                Log.e(
                    "SessionCoordinator",
                    "Canal ${index + 1}: instrumento não encontrado. " +
                            "SoundFont=${channel.soundFontId}, " +
                            "bank=${channel.bankMsb}, " +
                            "program=${channel.program}"
                )

                return@forEachIndexed
            }

            restaurarCanal(
                index,
                instrumento.first,
                instrumento.second
            )

            Log.d(
                "SessionCoordinator",
                "Canal ${index + 1} restaurado: " +
                        "SoundFont=${instrumento.first.nome}, " +
                        "preset=${instrumento.second.nome}"
            )
        }

        // Reaplica volumes, mute e canais vazios respeitando o estado de mute e o modo Master.
        mixerState.channels.forEach { channel ->
            val volumeEfetivo = if (channel.muted || channel.soundFontId < 0) {
                0
            } else if (mixerState.channel1AsMaster) {
                mixerState.calcularVolumeComMaster(
                    channel.channel,
                    mixerState.getChannel(0).volume
                )
            } else {
                channel.volume
            }

            val muteEfetivo = channel.muted || channel.soundFontId < 0

            restaurarVolume(
                channel.channel,
                volumeEfetivo,
                muteEfetivo
            )
        }
    }
}
