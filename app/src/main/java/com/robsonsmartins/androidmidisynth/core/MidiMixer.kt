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
    var soundFont: String = ""
)

class MidiMixer(numberOfChannels: Int = 5) {

    private val channels = MutableList(numberOfChannels) { index ->
        MidiChannel(channel = index)
    }

    // ============================================================================
    // Validação
    // ============================================================================

    private fun requireChannel(channel: Int) {
        require(channel in channels.indices) {
            "Canal MIDI inválido: $channel"
        }
    }

    // ============================================================================
    // Consulta
    // ============================================================================

    fun getChannel(channel: Int): MidiChannel {
        requireChannel(channel)
        return channels[channel]
    }

    fun getVolume(channel: Int): Int {
        requireChannel(channel)
        return channels[channel].volume
    }

    fun getProgram(channel: Int): Int {
        requireChannel(channel)
        return channels[channel].program
    }

    fun isMuted(channel: Int): Boolean {
        requireChannel(channel)
        return channels[channel].muted
    }

    // ============================================================================
    // Alterações
    // ============================================================================

    fun setVolume(channel: Int, volume: Int) {
        requireChannel(channel)
        channels[channel].volume = volume.coerceIn(0, 127)
    }

    fun setProgram(channel: Int, program: Int) {
        requireChannel(channel)
        channels[channel].program = program.coerceIn(0, 127)
    }

    fun setMute(channel: Int, mute: Boolean) {
        requireChannel(channel)
        channels[channel].muted = mute
    }

    fun setInstrumentName(channel: Int, name: String) {
        requireChannel(channel)
        channels[channel].instrumentName = name
    }

    fun setSoundFont(channel: Int, soundFont: String) {
        requireChannel(channel)
        channels[channel].soundFont = soundFont
    }
}