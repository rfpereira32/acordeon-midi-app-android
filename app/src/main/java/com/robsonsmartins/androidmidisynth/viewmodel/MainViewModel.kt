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

    fun setChannelVolume(channel: Int, volume: Int) {

        mixerState.getChannel(channel).volume = volume

        if (!mixerState.getChannel(channel).muted) {
            synthController.setVolume(channel, volume)
        }
        if (::configurationSynchronizer.isInitialized) {
            configurationSynchronizer.setVolume(
                channel,
                volume
            )
        }
    }

    fun getChannel(channel: Int) =
        midiMixer.getChannel(channel)

    fun setChannelMute(channel: Int, mute: Boolean) {

        mixerState.getChannel(channel).muted = mute

        if (mute) {
            synthController.setVolume(channel, 0)
        } else {
            synthController.setVolume(
                channel,
                mixerState.getChannel(channel).volume
            )
        }

    }

    fun isChannelMuted(channel: Int): Boolean {

        return mixerState
            .getChannel(channel)
            .muted

    }

    fun getEffectiveChannelVolume(channel: Int): Int {
        return if (midiMixer.isMuted(channel))
            0
        else
            midiMixer.getVolume(channel)
    }

    /**
     * API antiga.
     * Será removida futuramente.
     */
    fun setChannelProgram(channel: Int, program: Int) {
        midiMixer.setProgram(channel, program)
    }

    fun getChannelProgram(channel: Int): Int {
        return midiMixer.getProgram(channel)
    }

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

        // Garante que a SoundFont esteja carregada
        synthController.carregarSoundFont(soundFont)

        midiMixer.setSoundFont(
            channel,
            soundFont
        )

        midiMixer.setPreset(
            channel,
            preset
        )

        val channelState = mixerState.getChannel(channel)

        channelState.soundFont = soundFont
        channelState.preset = preset

        channelState.bankMsb = preset.bank
        channelState.program = preset.program

        val midiChannel =
            midiMixer.getChannel(channel)

        midiChannel.soundFont = soundFont
        midiChannel.preset = preset
        midiChannel.bankMsb = preset.bank
        midiChannel.program = preset.program

        soundFont.presetSelecionado = preset

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
    }


    fun getChannels() = mixerState.channels

    fun toggleChannelMute(channel: Int) {

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

    var masterVolume by mutableFloatStateOf(0.8f)

    // =============================================================================
    // Sistema
    // =============================================================================

    var usoCpu by mutableIntStateOf(0)

    var bateria by mutableIntStateOf(100)

    // =============================================================================
    // BLE / MIDI
    // =============================================================================

    var dispositivosMidi by mutableStateOf<List<MidiDeviceInfo>>(emptyList())

    var dispositivoConectado by mutableStateOf<MidiDeviceInfo?>(null)

    var bleConectado by mutableStateOf(false)

    // =============================================================================
    // SoundFont
    // =============================================================================

    fun listarSoundFonts() =
        soundFontManager.listar()

    fun carregarSoundFont(id: Int) {

        soundFontManager.carregar(id)

        salvarSessao()

    }

    fun descarregarSoundFont(id: Int) {

        soundFontManager.descarregar(id)

        salvarSessao()

    }

    fun alternarSoundFont(id: Int) {

        soundFontManager.alternar(id)

        salvarSessao()

    }

    fun importarSoundFont(uri: Uri): Boolean {

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

        val instrumentos = mutableListOf<InstrumentItem>()

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

    fun podeExcluirSoundFont(id: Int): Boolean {

        val soundFont =
            soundFontManager
                .getSoundFont(id)
                ?: return false

        return soundFontManager.podeExcluir(soundFont) {

            mixerState.usaSoundFont(it.id)

        }

    }
    fun excluirSoundFont(id: Int): Boolean {

        return soundFontManager.excluir(id) {

            mixerState.usaSoundFont(it.id)

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