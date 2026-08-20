package com.robsonsmartins.androidmidisynth.configuration

/**
 * Representa a configuração completa do mixer.
 *
 * Atualmente contém os cinco canais do acordeão.
 *
 * Futuramente esta mesma estrutura será utilizada
 * para armazenar presets do usuário.
 */
data class MixerConfiguration(

    val channels: MutableList<ChannelConfiguration> =
        MutableList(5) { index ->

            ChannelConfiguration(

                controlSource =
                    when (index) {
                        0 -> 1
                        1 -> 2
                        2 -> 3
                        else -> 4
                    }

            )

        },

    /**
     * Quando ativado, o volume do canal 1
     * controla também os demais canais.
     */
    val channel1AsMaster: Boolean = false

)