package com.robsonsmartins.androidmidisynth.core

import androidx.compose.runtime.mutableStateListOf
import com.robsonsmartins.androidmidisynth.configuration.ChannelConfiguration
import com.robsonsmartins.androidmidisynth.configuration.MixerConfiguration
import android.util.Log

class MixerState(
    numberOfChannels: Int = 5
) {

    val channels = mutableStateListOf<ChannelState>()

    init {

        repeat(numberOfChannels) {

            channels += ChannelState(channel = it)

        }

    }

    fun getChannel(index: Int): ChannelState {

        return channels[index]

    }

    fun usaSoundFont(id: Int): Boolean {

        return channels.any {

            it.soundFont?.id == id

        }

    }

    /**
     * Exporta o estado atual do mixer para uma configuração.
     */
    fun exportarConfiguracao(): MixerConfiguration {

        channels.forEachIndexed { index, channel ->

            Log.d(
                "MixerState",
                "Exportando canal $index " +
                        "SF=${channel.soundFontId} " +
                        "Program=${channel.program} " +
                        "Volume=${channel.volume}"
            )

        }

        return MixerConfiguration(

            channels.map { channel ->

                ChannelConfiguration(

                    enabled = true,

                    soundFontId = channel.soundFontId,

                    bank = channel.bankMsb,

                    program = channel.program,

                    volume = channel.volume,

                    mute = channel.muted

                )

            }.toMutableList()

        )

    }

    /**
     * Aplica uma configuração ao estado do mixer.
     *
     * Nesta etapa apenas atualiza o estado interno.
     * A sincronização com o FluidSynth será feita
     * posteriormente pelo SynthController.
     */
    fun aplicarConfiguracao(
        configuracao: MixerConfiguration
    ) {

        configuracao.channels.forEachIndexed { index, channelConfig ->

            if (index >= channels.size)
                return@forEachIndexed

            val channel = channels[index]

            channel.volume = channelConfig.volume
            channel.muted = channelConfig.mute

            channel.soundFontId = channelConfig.soundFontId
            channel.bankMsb = channelConfig.bank
            channel.program = channelConfig.program

            // A SoundFont e o Preset serão restaurados
            // posteriormente, após todas as SoundFonts
            // estarem carregadas.

        }

    }

}