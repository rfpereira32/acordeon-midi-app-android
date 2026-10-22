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
    fun carregarSoundFont(soundFont: SoundFontInfo): Boolean {

        Log.d(
            "SoundFontDebug",
            "INICIO SynthController.carregarSoundFont"
        )

        Log.d(
            "SoundFontDebug",
            "SoundFont: id=${soundFont.id} " +
                    "nome=${soundFont.nome} " +
                    "sfid=${soundFont.sfid} " +
                    "caminho=${soundFont.caminho}"
        )

        if (soundFont.sfid >= 0) {

            Log.d(
                "SoundFontDebug",
                "SoundFont já possui sfid=${soundFont.sfid}"
            )

            if (soundFont.presets.isNotEmpty()) {
                Log.d(
                    "SoundFontDebug",
                    "Lista de presets já preenchida: ${soundFont.presets.size}"
                )
                return true
            }

            Log.d(
                "SoundFontDebug",
                "SoundFont carregada, mas lista vazia. Recuperando presets."
            )

            val presets = synth.listPresets(soundFont.sfid)

            if (presets.isEmpty()) {
                Log.e(
                    "SynthController",
                    "SoundFont '${soundFont.nome}' (sfid=${soundFont.sfid}) possui lista de presets vazia."
                )
                synth.unloadSF(soundFont.sfid)
                soundFont.sfid = -1
                return false
            }

            soundFont.presets.clear()
            soundFont.presets.addAll(presets)
            soundFont.quantidadePresets = presets.size

            Log.d(
                "SoundFontDebug",
                "Presets recuperados: quantidade=${presets.size}"
            )

            return true
        }

        Log.d(
            "SoundFontDebug",
            "ANTES synth.loadSF"
        )

        val sfid = synth.loadSF(
            soundFont.caminho
        )

        Log.d(
            "SoundFontDebug",
            "DEPOIS synth.loadSF: sfid=$sfid"
        )

        soundFont.sfid = sfid

        if (sfid < 0) {
            Log.e(
                "SynthController",
                "Falha ao carregar SoundFont '${soundFont.nome}' (caminho=${soundFont.caminho}): sfid inválido ($sfid)"
            )
            return false
        }

        Log.d(
            "SoundFontDebug",
            "ANTES synth.listPresets"
        )

        val presets = synth.listPresets(sfid)

        Log.d(
            "SoundFontDebug",
            "DEPOIS synth.listPresets: quantidade=${presets.size}"
        )

        if (presets.isEmpty()) {
            Log.e(
                "SynthController",
                "SoundFont '${soundFont.nome}' carregada (sfid=$sfid) retornou lista de presets vazia."
            )
            synth.unloadSF(sfid)
            soundFont.sfid = -1
            return false
        }

        soundFont.presets.clear()

        soundFont.presets.addAll(presets)

        soundFont.quantidadePresets = presets.size

        Log.d(
            "SoundFontDebug",
            "FIM SynthController.carregarSoundFont"
        )

        return true
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

    fun sendCC(
        channel: Int,
        controller: Int,
        value: Int
    ) {
        synth.sendCC(
            channel,
            controller,
            value
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