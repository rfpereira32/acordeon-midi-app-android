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
import com.robsonsmartins.androidmidisynth.core.SystemState
import com.robsonsmartins.androidmidisynth.soundfont.SoundFontInfo
import com.robsonsmartins.androidmidisynth.soundfont.SoundFontManager
import com.robsonsmartins.androidmidisynth.soundfont.PresetInfo
import com.robsonsmartins.androidmidisynth.soundfont.InstrumentItem
import com.robsonsmartins.androidmidisynth.sync.ConfigurationSynchronizer

class MainViewModel : ViewModel() {

    private lateinit var synthController: SynthController

    private lateinit var soundFontManager: SoundFontManager

    private lateinit var configurationSynchronizer: ConfigurationSynchronizer

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

    // =============================================================================
    // Mixer
    // =============================================================================

    private val midiMixer = MidiMixer()

    // =============================================================================
    // Estado da aplicação
    // =============================================================================

    val mixerState = MixerState()

    val deviceState = DeviceState()

    val systemState = SystemState()

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

    var soundFontAtual by mutableStateOf("AcordeonGiulietti.sf2")

    fun listarSoundFonts() =
        soundFontManager.listar()

    fun carregarSoundFont(id: Int) =
        soundFontManager.carregar(id)

    fun descarregarSoundFont(id: Int) =
        soundFontManager.descarregar(id)

    fun alternarSoundFont(id: Int) =
        soundFontManager.alternar(id)

    fun importarSoundFont(uri: Uri) {

        soundFontManager.importarSoundFont(uri)

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
            .listar()
            .filter { it.carregada }
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

    /**
     * Sincroniza as SoundFonts marcadas na interface
     * com as realmente carregadas no FluidSynth.
     */
    fun aplicarSoundFonts(lista: List<SoundFontInfo>) {

        lista.forEach { soundFont ->

            if (soundFont.carregada) {

                synthController.carregarSoundFont(soundFont)

            } else {

                synthController.descarregarSoundFont(soundFont)

            }

        }

    }

}