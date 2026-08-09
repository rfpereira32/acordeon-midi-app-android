package com.robsonsmartins.androidmidisynth.audio

import com.robsonsmartins.androidmidisynth.FluidSynthManager
import com.robsonsmartins.androidmidisynth.soundfont.PresetInfo
import com.robsonsmartins.androidmidisynth.soundfont.SoundFontInfo
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

    // =============================================================================
    // Mixer
    // =============================================================================

    fun setVolume(channel: Int, volume: Int) {

        val valor = volume.coerceIn(0, 127)
        Log.d(
            "SynthController",
            "setVolume canal=$channel volume=$valor"
        )

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

    // =============================================================================
    // Instrumentos
    // =============================================================================

    /**
     * API antiga.
     *
     * Mantida temporariamente por compatibilidade.
     */
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
     * API antiga.
     *
     * Mantida temporariamente.
     */
    fun setInstrument(
        channel: Int,
        sfid: Int,
        bank: Int,
        preset: Int
    ) {

        synth.programSelect(
            channel,
            sfid,
            bank,
            preset
        )

    }

    /**
     * Nova API.
     *
     * Seleciona um instrumento utilizando
     * os próprios objetos da aplicação.
     */
    fun setInstrument(
        channel: Int,
        soundFont: SoundFontInfo,
        preset: PresetInfo
    ) {
        Log.d(
            "SynthController",
            "setInstrument canal=$channel"
        )

        if (soundFont.sfid < 0)
            return

        synth.programSelect(
            channel,
            soundFont.sfid,
            preset.bank,
            preset.program
        )

    }

    // =============================================================================
    // SoundFonts
    // =============================================================================

    /**
     * Carrega uma SoundFont.
     *
     * Após o carregamento, todos os presets são
     * lidos do FluidSynth e armazenados na própria
     * SoundFontInfo.
     */
    fun carregarSoundFont(soundFont: SoundFontInfo) {

        if (soundFont.sfid >= 0)
            return

        val sfid = synth.loadSF(
            soundFont.caminho
        )

        soundFont.sfid = sfid

        val presets = synth.listPresets(sfid)

        soundFont.presets.clear()

        soundFont.presets.addAll(presets)

        soundFont.quantidadePresets = presets.size

    }

    /**
     * Descarrega uma SoundFont.
     */
    fun descarregarSoundFont(soundFont: SoundFontInfo) {

        if (soundFont.sfid < 0)
            return

        synth.unloadSF(soundFont.sfid)

        soundFont.sfid = -1

        soundFont.presets.clear()

        soundFont.quantidadePresets = 0

        soundFont.presetSelecionado = null

    }

    /**
     * Retorna todos os presets existentes
     * em uma SoundFont já carregada.
     */
    fun listarPresets(
        soundFont: SoundFontInfo
    ): List<PresetInfo> {

        if (soundFont.sfid < 0)
            return emptyList()

        return synth.listPresets(
            soundFont.sfid
        )

    }

    // =============================================================================
    // MIDI
    // =============================================================================

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