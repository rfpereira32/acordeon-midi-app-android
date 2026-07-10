package com.robsonsmartins.androidmidisynth.viewmodel
import com.robsonsmartins.androidmidisynth.audio.SynthController
import com.robsonsmartins.androidmidisynth.soundfont.SoundFontManager

import android.media.midi.MidiDeviceInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.robsonsmartins.androidmidisynth.core.MidiMixer
import com.robsonsmartins.androidmidisynth.core.DeviceState
import com.robsonsmartins.androidmidisynth.core.MixerState
import com.robsonsmartins.androidmidisynth.core.SystemState

class MainViewModel : ViewModel() {

    private lateinit var synthController: SynthController

    private lateinit var soundFontManager: SoundFontManager

    fun setSoundFontManager(manager: SoundFontManager) {
        soundFontManager = manager
    }
    fun setSynthController(controller: SynthController) {
        synthController = controller
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

    fun setChannelProgram(channel: Int, program: Int) {
        midiMixer.setProgram(channel, program)
    }

    fun getChannelProgram(channel: Int): Int {
        return midiMixer.getProgram(channel)
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
}
