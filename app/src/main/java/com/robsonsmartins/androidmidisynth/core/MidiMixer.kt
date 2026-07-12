package com.robsonsmartins.androidmidisynth.core

data class MidiChannel(

    val channel: Int,

    var volume: Int = 100,
    var expression: Int = 127,
    var pan: Int = 64,

    // Seleção da SoundFont
    var sfid: Int = -1,

    // Seleção do instrumento
    var bankMsb: Int = 0,
    var bankLsb: Int = 0,
    var program: Int = 0,

    var muted: Boolean = false,

    // Apenas informações para a interface
    var instrumentName: String = "",
    var soundFontName: String = ""

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

    fun getSoundFontId(channel: Int): Int {
        requireChannel(channel)
        return channels[channel].sfid
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

    fun setSoundFontId(channel: Int, sfid: Int) {
        requireChannel(channel)
        channels[channel].sfid = sfid
    }

    fun setMute(channel: Int, mute: Boolean) {
        requireChannel(channel)
        channels[channel].muted = mute
    }

    fun setInstrumentName(channel: Int, name: String) {
        requireChannel(channel)
        channels[channel].instrumentName = name
    }

    fun setSoundFontName(channel: Int, name: String) {
        requireChannel(channel)
        channels[channel].soundFontName = name
    }
}