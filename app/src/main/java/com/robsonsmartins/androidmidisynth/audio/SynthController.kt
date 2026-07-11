package com.robsonsmartins.androidmidisynth.audio

import com.robsonsmartins.androidmidisynth.FluidSynthManager
import com.robsonsmartins.androidmidisynth.soundfont.SoundFontInfo

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

        synth.setChannelVolume(
            channel,
            valor
        )

    }

    fun setMute(channel: Int, mute: Boolean) {

        if (mute) {
            synth.setChannelVolume(channel, 0)
        }

    }

    fun setProgram(
        channel: Int,
        bank: Int,
        program: Int
    ) {

        synth.programChange(
            channel,
            bank,
            program
        )

    }

    /**
     * Carrega uma SoundFont no FluidSynth.
     *
     * Não recarrega caso ela já esteja carregada.
     */
    fun carregarSoundFont(soundFont: SoundFontInfo) {

        if (soundFont.sfid >= 0)
            return

        val sfid = synth.loadSF(soundFont.caminho)

        soundFont.sfid = sfid

    }

    /**
     * Remove uma SoundFont do FluidSynth.
     */
    fun descarregarSoundFont(soundFont: SoundFontInfo) {

        if (soundFont.sfid < 0)
            return

        synth.unloadSF(soundFont.sfid)

        soundFont.sfid = -1

    }

    fun noteOn(
        channel: Int,
        note: Int,
        velocity: Int
    ) {
        // futuro
    }

    fun noteOff(
        channel: Int,
        note: Int
    ) {
        // futuro
    }

}