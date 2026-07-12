package com.robsonsmartins.androidmidisynth.soundfont

/**
 * Representa um preset (instrumento) existente
 * dentro de uma SoundFont.
 */
data class PresetInfo(

    /**
     * Banco MIDI.
     */
    val bank: Int,

    /**
     * Programa (Preset).
     */
    val program: Int,

    /**
     * Nome do instrumento.
     */
    val nome: String

)