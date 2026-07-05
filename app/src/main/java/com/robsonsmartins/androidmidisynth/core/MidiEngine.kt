package com.robsonsmartins.androidmidisynth.core

class MidiEngine(
    private val mixer: MidiMixer
) {

    fun getEffectiveVolume(channel: Int): Int {

        val dados = mixer.getChannel(channel)

        return if (dados.muted)
            0
        else
            dados.volume
    }

    fun isMuted(channel: Int): Boolean {
        return mixer.isMuted(channel)
    }

    fun getChannel(channel: Int): MidiChannel {
        return mixer.getChannel(channel)
    }
}