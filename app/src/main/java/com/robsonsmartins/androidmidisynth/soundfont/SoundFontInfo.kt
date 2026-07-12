package com.robsonsmartins.androidmidisynth.soundfont

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SoundFontInfo(

    val id: Int,

    val nome: String,

    val caminho: String,

    carregada: Boolean = false,

    quantidadePresets: Int = 0,

    sfid: Int = -1

) {

    /**
     * Indica se esta SoundFont está carregada no FluidSynth.
     */
    var carregada by mutableStateOf(carregada)

    /**
     * Quantidade de presets disponíveis.
     *
     * Será preenchida automaticamente quando
     * implementarmos a leitura dos presets.
     */
    var quantidadePresets by mutableIntStateOf(quantidadePresets)

    /**
     * ID retornado pelo FluidSynth (fluid_synth_sfload).
     *
     * -1 indica que a SoundFont ainda não está carregada.
     */
    var sfid by mutableIntStateOf(sfid)

    /**
     * Lista de presets existentes nesta SoundFont.
     *
     * Será preenchida automaticamente após a
     * leitura da SoundFont pelo FluidSynth.
     */
    val presets = mutableStateListOf<PresetInfo>()

}