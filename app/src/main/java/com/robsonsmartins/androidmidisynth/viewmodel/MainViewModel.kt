package com.robsonsmartins.androidmidisynth.viewmodel

import android.media.midi.MidiDeviceInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.robsonsmartins.androidmidisynth.core.MidiMixer

class MainViewModel : ViewModel() {

    // =============================================================================
    // Mixer
    // =============================================================================

    private val midiMixer = MidiMixer()

    fun setChannelVolume(channel: Int, volume: Int) {
        midiMixer.setVolume(channel, volume)
    }

    fun getChannelVolume(channel: Int): Int {
        return midiMixer.getVolume(channel)
    }

    fun getChannel(channel: Int) =
        midiMixer.getChannel(channel)

    fun setChannelMute(channel: Int, mute: Boolean) {
        midiMixer.setMute(channel, mute)
    }

    fun isChannelMuted(channel: Int): Boolean {
        return midiMixer.isMuted(channel)
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

    fun getChannels() = List(5) {
        midiMixer.getChannel(it)
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
}