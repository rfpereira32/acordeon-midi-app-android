package com.robsonsmartins.androidmidisynth.configuration

/**
 * Representa um preset completo do acordeão.
 *
 * Um preset guarda:
 *
 * - nome do preset;
 * - SoundFonts necessárias;
 * - configuração completa do mixer.
 *
 * As SoundFonts são apenas referenciadas pelo ID,
 * nome e arquivo. O arquivo .sf2 não é duplicado
 * dentro do preset.
 */
data class PresetConfiguration(

    val nome: String,

    val soundFonts: List<PresetSoundFont> = emptyList(),

    val mixer: MixerConfiguration = MixerConfiguration()

)

/**
 * Representa uma SoundFont utilizada por um preset.
 *
 * O arquivo da SoundFont não é armazenado dentro
 * do preset. Apenas sua identificação é guardada.
 */
data class PresetSoundFont(

    val id: Int,

    val nome: String,

    val arquivo: String

)