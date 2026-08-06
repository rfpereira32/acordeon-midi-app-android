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

        MutableList(5) {

            ChannelConfiguration()

        }

)