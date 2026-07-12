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
     * Indica se esta SoundFont está carregada
     * no FluidSynth.
     */
    var carregada by mutableStateOf(carregada)

    /**
     * Quantidade de presets encontrados.
     */
    var quantidadePresets by mutableIntStateOf(quantidadePresets)

    /**
     * ID retornado pelo FluidSynth.
     */
    var sfid by mutableIntStateOf(sfid)

    /**
     * Todos os presets existentes nesta SoundFont.
     *
     * A interface observa esta lista automaticamente.
     */
    val presets = mutableStateListOf<PresetInfo>()

    /**
     * Preset atualmente selecionado.
     */
    var presetSelecionado by mutableStateOf<PresetInfo?>(null)

}