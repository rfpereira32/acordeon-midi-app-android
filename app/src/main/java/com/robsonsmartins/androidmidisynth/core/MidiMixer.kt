package com.robsonsmartins.androidmidisynth.core

data class MidiChannel(
    val channel: Int,

    var volume: Int = 100,
    var expression: Int = 127,
    var pan: Int = 64,

    var bankMsb: Int = 0,
    var bankLsb: Int = 0,
    var program: Int = 0,

    var muted: Boolean = false,

    var instrumentName: String = "",
    var soundFontId: String = ""
)

class MidiMixer(numberOfChannels: Int = 5) {

    val channels = MutableList(numberOfChannels) { index ->
        MidiChannel(channel = index)
    }

    fun setVolume(channel: Int, volume: Int) {
        channels[channel].volume = volume.coerceIn(0, 127)
    }

    fun getVolume(channel: Int): Int {
        return channels[channel].volume
    }

    fun setProgram(channel: Int, program: Int) {
        channels[channel].program = program
    }

    fun setMute(channel: Int, mute: Boolean) {
        channels[channel].muted = mute
    }

    fun isMuted(channel: Int): Boolean {
        return channels[channel].muted
    }
}