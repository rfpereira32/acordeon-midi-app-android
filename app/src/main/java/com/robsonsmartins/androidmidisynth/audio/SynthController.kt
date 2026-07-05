package com.robsonsmartins.androidmidisynth.audio

import com.robsonsmartins.androidmidisynth.FluidSynthManager

/**
 * Centraliza todas as operações relacionadas ao sintetizador.
 *
 * Esta classe encapsula o FluidSynthManager e será o ponto único
 * para controle de:
 *
 * - Volume por canal
 * - Mute
 * - Program Change
 * - Bank Select
 * - SoundFonts
 * - Presets
 * - Reverb / Chorus
 */
class SynthController(

    private val synth: FluidSynthManager

) {

    fun setVolume(
        channel: Int,
        volume: Int
    ) {
        synth.setChannelVolume(channel, volume)
    }

    fun noteOn(
        channel: Int,
        note: Int,
        velocity: Int
    ) {
        // Implementaremos depois
    }

    fun noteOff(
        channel: Int,
        note: Int
    ) {
        // Implementaremos depois
    }

    fun setMute(
        channel: Int,
        mute: Boolean
    ) {
        // Implementaremos depois
    }

    fun setProgram(
        channel: Int,
        program: Int
    ) {
        // Implementaremos depois
    }

    fun loadSoundFont(
        filename: String
    ) {
        // Implementaremos depois
    }
}