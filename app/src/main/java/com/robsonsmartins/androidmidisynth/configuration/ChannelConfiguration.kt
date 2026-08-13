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

    val mute: Boolean = false,

    /**
     * Define qual controle de notas enviado pelo ESP32
     * controla este canal.
     *
     * 1 = Controle 1
     * 2 = Controle 2
     * 3 = Controle 3
     * 4 = Controles 2 + 3
     */
    val controlSource: Int = 1

)