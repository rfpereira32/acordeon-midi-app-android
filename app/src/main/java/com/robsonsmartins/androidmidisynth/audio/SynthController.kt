package com.robsonsmartins.androidmidisynth.audio

import com.robsonsmartins.androidmidisynth.FluidSynthManager
import android.util.Log

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

    fun setVolume(channel: Int, volume: Int) {

        val valor = volume.coerceIn(0, 127)
        synth.setChannelVolume(channel, valor)
    }

    fun setMute(channel: Int, mute: Boolean) {
        // próximo commit
    }

    fun setProgram(channel: Int, program: Int) {
        // próximo commit
    }

    fun noteOn(channel: Int, note: Int, velocity: Int) {
        // futuro
    }

    fun noteOff(channel: Int, note: Int) {
        // futuro
    }
}