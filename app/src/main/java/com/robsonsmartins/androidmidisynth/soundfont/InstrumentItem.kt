package com.robsonsmartins.androidmidisynth.soundfont

/**
 * Representa um instrumento disponível para seleção.
 *
 * Cada item associa um preset à SoundFont
 * da qual ele pertence.
 */
data class InstrumentItem(

    val soundFont: SoundFontInfo,

    val preset: PresetInfo

)