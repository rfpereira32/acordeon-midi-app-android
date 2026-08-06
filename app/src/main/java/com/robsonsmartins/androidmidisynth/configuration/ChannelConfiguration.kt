package com.robsonsmartins.androidmidisynth.configuration

/**
 * Representa a configuração completa de um canal MIDI.
 *
 * Esta estrutura é utilizada para:
 *
 * - sessão atual do aplicativo;
 * - presets do usuário (futuro);
 * - restauração dos canais.
 */
data class ChannelConfiguration(

    val enabled: Boolean = true,

    val soundFontId: Int = -1,

    val bank: Int = 0,

    val program: Int = 0,

    val volume: Int = 100,

    val mute: Boolean = false

)