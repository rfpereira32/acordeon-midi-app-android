package com.robsonsmartins.androidmidisynth.viewmodel

import android.media.midi.MidiDeviceInfo
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.robsonsmartins.androidmidisynth.audio.SynthController
import com.robsonsmartins.androidmidisynth.core.DeviceState
import com.robsonsmartins.androidmidisynth.core.MidiMixer
import com.robsonsmartins.androidmidisynth.core.MixerState
import com.robsonsmartins.androidmidisynth.soundfont.SoundFontInfo
import com.robsonsmartins.androidmidisynth.soundfont.SoundFontManager
import com.robsonsmartins.androidmidisynth.soundfont.PresetInfo
import com.robsonsmartins.androidmidisynth.soundfont.InstrumentItem
import com.robsonsmartins.androidmidisynth.sync.ConfigurationSynchronizer
import com.robsonsmartins.androidmidisynth.session.SessionCoordinator
import android.util.Log


class MainViewModel : ViewModel() {

    private lateinit var synthController: SynthController

    private lateinit var soundFontManager: SoundFontManager

    private lateinit var configurationSynchronizer: ConfigurationSynchronizer
    private lateinit var sessionCoordinator: SessionCoordinator

    fun setSoundFontManager(manager: SoundFontManager) {
        soundFontManager = manager
    }

    fun setSynthController(controller: SynthController) {
        synthController = controller
    }

    fun setConfigurationSynchronizer(
        synchronizer: ConfigurationSynchronizer
    ) {
        configurationSynchronizer = synchronizer
    }

    fun setSessionCoordinator(
        coordinator: SessionCoordinator
    ) {

        sessionCoordinator = coordinator

    }

    // =============================================================================
    // Mixer
    // =============================================================================

    private val midiMixer = MidiMixer()

    // =============================================================================
    // Estado da aplicação
    // =============================================================================

    val mixerState = MixerState()

    val deviceState = DeviceState()

    // =============================================================================
    // Volume
    // =============================================================================

    /**
     * Altera o volume de um canal.
     *
     * Quando o canal 1 está funcionando como Master,
     * somente ele pode alterar o volume.
     *
     * Os demais canais acompanham o Master utilizando
     * os offsets calculados no momento da ativação.
     */
    fun setChannelVolume(
        channel: Int,
        volume: Int
    ) {

        if (channel !in 0..4)
            return

        val novoVolume =
            volume.coerceIn(0, 127)

        // -------------------------------------------------------------------------
        // Master ativo
        // -------------------------------------------------------------------------

        if (mixerState.channel1AsMaster) {

            // Somente o canal 1 controla o volume
            // quando o Master está ativo.
            if (channel != 0)
                return

            mixerState.getChannel(0).volume =
                novoVolume

            aplicarVolumesDoMaster(
                novoVolume
            )

            salvarSessao()

            return
        }

        // -------------------------------------------------------------------------
        // Master desligado
        // -------------------------------------------------------------------------

        aplicarVolumeIndividual(
            channel,
            novoVolume
        )

        salvarSessao()
    }

    /**
     * Aplica o volume individual de um canal.
     */
    private fun aplicarVolumeIndividual(
        channel: Int,
        volume: Int
    ) {

        val channelState =
            mixerState.getChannel(channel)

        channelState.volume =
            volume.coerceIn(0, 127)

        if (!channelState.muted) {

            synthController.setVolume(
                channel,
                channelState.volume
            )

        }

        if (::configurationSynchronizer.isInitialized) {

            configurationSynchronizer.setVolume(
                channel,
                channelState.volume
            )

        }

    }

    /**
     * Aplica o volume do Master a todos os canais
     * utilizando os offsets calculados anteriormente.
     */
    private fun aplicarVolumesDoMaster(
        masterVolume: Int
    ) {

        val volumeMaster =
            masterVolume.coerceIn(0, 127)

        mixerState.channels.forEach { channel ->

            val volumeEfetivo =
                mixerState.calcularVolumeComMaster(
                    channel.channel,
                    volumeMaster
                )

            channel.volume =
                volumeEfetivo

            // Mute continua independente do Master.
            if (!channel.muted) {

                synthController.setVolume(
                    channel.channel,
                    volumeEfetivo
                )

            }

            if (::configurationSynchronizer.isInitialized) {

                configurationSynchronizer.setVolume(
                    channel.channel,
                    volumeEfetivo
                )

            }

            Log.d(
                "MainViewModel",
                "Master: canal=${channel.channel} " +
                        "offset=${mixerState.getMasterOffset(channel.channel)} " +
                        "volume=$volumeEfetivo"
            )

        }

    }

    /**
     * Ativa ou desativa o Canal 1 como Master.
     *
     * Ao ativar, os offsets são calculados com base
     * nos volumes atuais dos cinco canais.
     */
    fun setChannel1AsMaster(
        enabled: Boolean
    ) {

        if (enabled) {

            mixerState.ativarMaster()

            Log.d(
                "MainViewModel",
                "Canal 1 definido como Master"
            )

            aplicarVolumesDoMaster(
                mixerState.getChannel(0).volume
            )

        } else {

            mixerState.desativarMaster()

            Log.d(
                "MainViewModel",
                "Canal 1 deixou de ser Master"
            )

        }

        salvarSessao()

    }

    /**
     * Retorna se o Canal 1 está funcionando como Master.
     */
    fun isChannel1AsMaster(): Boolean {

        return mixerState.channel1AsMaster

    }

    fun getChannel(channel: Int) =
        midiMixer.getChannel(channel)

    // =============================================================================
    // Mute
    // =============================================================================

    /**
     * Altera o mute de um canal.
     *
     * O mute é sempre independente do Master.
     */
    fun setChannelMute(
        channel: Int,
        mute: Boolean
    ) {

        if (channel !in 0..4)
            return

        Log.d(
            "MainViewModel",
            "Alterando mute canal=$channel mute=$mute"
        )

        val channelState =
            mixerState.getChannel(channel)

        channelState.muted =
            mute

        if (mute) {

            synthController.setVolume(
                channel,
                0
            )

        } else {

            synthController.setVolume(
                channel,
                channelState.volume
            )

        }

        salvarSessao()

    }

    fun isChannelMuted(
        channel: Int
    ): Boolean {

        return mixerState
            .getChannel(channel)
            .muted

    }

    fun getEffectiveChannelVolume(
        channel: Int
    ): Int {

        return if (midiMixer.isMuted(channel))
            0
        else
            midiMixer.getVolume(channel)

    }

    // =============================================================================
    // Program Change
    // =============================================================================

    /**
     * API antiga.
     * Será removida futuramente.
     */
    fun setChannelProgram(
        channel: Int,
        program: Int
    ) {

        midiMixer.setProgram(
            channel,
            program
        )

    }

    fun getChannelProgram(
        channel: Int
    ): Int {

        return midiMixer.getProgram(
            channel
        )

    }

    // =============================================================================
    // Instrumentos
    // =============================================================================

    /**
     * Nova API.
     *
     * Associa uma SoundFont a um canal e seleciona
     * banco/preset no FluidSynth.
     */
    fun setChannelInstrument(
        channel: Int,
        soundFont: SoundFontInfo,
        preset: PresetInfo
    ) {

        Log.d(
            "MainViewModel",
            "MixerState = ${System.identityHashCode(mixerState)}"
        )

        // Garante que a SoundFont esteja carregada
        synthController.carregarSoundFont(
            soundFont
        )

        midiMixer.setSoundFont(
            channel,
            soundFont
        )

        midiMixer.setPreset(
            channel,
            preset
        )

        val channelState =
            mixerState.getChannel(channel)

        channelState.soundFont =
            soundFont

        channelState.soundFontId =
            soundFont.id

        channelState.preset =
            preset

        channelState.bankMsb =
            preset.bank

        channelState.program =
            preset.program

        Log.d(
            "MainViewModel",
            "Canal=$channel " +
                    "SF=${channelState.soundFontId} " +
                    "Program=${channelState.program}"
        )

        val midiChannel =
            midiMixer.getChannel(channel)

        midiChannel.soundFont =
            soundFont

        midiChannel.preset =
            preset

        midiChannel.bankMsb =
            preset.bank

        midiChannel.program =
            preset.program

        soundFont.presetSelecionado =
            preset

        synthController.setInstrument(
            channel,
            soundFont,
            preset
        )

        if (::configurationSynchronizer.isInitialized) {

            configurationSynchronizer.setInstrumento(
                channel,
                preset.program
            )

        }

        salvarSessao()

    }

    fun getChannels() =
        mixerState.channels

    fun toggleChannelMute(
        channel: Int
    ) {

        val novoEstado =
            !mixerState
                .getChannel(channel)
                .muted

        setChannelMute(
            channel,
            novoEstado
        )

    }

    // =============================================================================
    // Áudio
    // =============================================================================

    /**
     * Mantido temporariamente para compatibilidade
     * com a MainActivity atual.
     *
     * A lógica de Master do mixer agora está em
     * channel1AsMaster / MixerState.
     *
     * Será removido quando a MainActivity deixar
     * de utilizar este valor.
     */
    var masterVolume by mutableFloatStateOf(0.8f)

    // =============================================================================
    // Sistema
    // =============================================================================

    var usoCpu by mutableIntStateOf(0)

    var bateria by mutableIntStateOf(100)

    // =============================================================================
    // BLE / MIDI
    // =============================================================================

    var dispositivosMidi by mutableStateOf<List<MidiDeviceInfo>>(
        emptyList()
    )

    var dispositivoConectado by mutableStateOf<MidiDeviceInfo?>(null)

    var bleConectado by mutableStateOf(false)

    // =============================================================================
    // SoundFont
    // =============================================================================

    fun listarSoundFonts() =
        soundFontManager.listar()

    fun carregarSoundFont(
        id: Int
    ) {

        soundFontManager.carregar(id)

        salvarSessao()

    }

    fun descarregarSoundFont(
        id: Int
    ) {

        soundFontManager.descarregar(id)

        salvarSessao()

    }

    fun alternarSoundFont(
        id: Int
    ) {

        soundFontManager.alternar(id)

        salvarSessao()

    }

    fun importarSoundFont(
        uri: Uri
    ): Boolean {

        val sucesso =
            soundFontManager.importarSoundFont(uri)

        if (sucesso) {

            salvarSessao()

        }

        return sucesso

    }

    /**
     * Retorna todos os instrumentos disponíveis
     * em todas as SoundFonts carregadas.
     *
     * A lista é ordenada alfabeticamente pelo
     * nome do instrumento.
     */
    fun listarInstrumentos(): List<InstrumentItem> {

        val instrumentos =
            mutableListOf<InstrumentItem>()

        soundFontManager
            .carregadas()
            .forEach { soundFont ->

                soundFont.presets.forEach { preset ->

                    instrumentos.add(

                        InstrumentItem(

                            soundFont = soundFont,

                            preset = preset

                        )

                    )

                }

            }

        return instrumentos.sortedBy {

            it.preset.nome.lowercase()

        }

    }

    fun sincronizarBiblioteca() {

        soundFontManager.sincronizarBiblioteca()

    }

    fun podeExcluirSoundFont(
        id: Int
    ): Boolean {

        val soundFont =
            soundFontManager
                .getSoundFont(id)
                ?: return false

        return soundFontManager.podeExcluir(
            soundFont
        ) {

            mixerState.usaSoundFont(
                it.id
            )

        }

    }

    fun excluirSoundFont(
        id: Int
    ): Boolean {

        return soundFontManager.excluir(id) {

            mixerState.usaSoundFont(
                it.id
            )

        }

    }

    fun salvarSessao() {

        if (::sessionCoordinator.isInitialized) {

            sessionCoordinator.salvar()

        }

    }

    fun excluirSoundFont(
        id: Int,
        estaEmUso: (SoundFontInfo) -> Boolean
    ): Boolean {

        val sucesso =
            soundFontManager.excluir(
                id,
                estaEmUso
            )

        if (sucesso) {

            salvarSessao()

        }

        return sucesso

    }

}