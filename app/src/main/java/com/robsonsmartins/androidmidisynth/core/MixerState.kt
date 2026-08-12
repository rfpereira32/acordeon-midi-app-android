package com.robsonsmartins.androidmidisynth.core

import androidx.compose.runtime.mutableStateListOf
import com.robsonsmartins.androidmidisynth.configuration.ChannelConfiguration
import com.robsonsmartins.androidmidisynth.configuration.MixerConfiguration
import android.util.Log

class MixerState(
    numberOfChannels: Int = 5
) {

    companion object {

        const val MIN_CHANNELS = 5
        const val MAX_CHANNELS = 16

    }

    val channels =
        mutableStateListOf<ChannelState>()

    /**
     * Quando ativado, o canal 1 funciona como Master.
     *
     * O volume do canal 1 controla os demais canais
     * mantendo a diferença de volume existente no
     * momento em que o Master foi ativado.
     */
    var channel1AsMaster: Boolean = false

    /**
     * Diferença de volume de cada canal em relação
     * ao canal 1 no momento da ativação do Master.
     */
    private val masterOffsets =
        mutableListOf<Int>()

    init {

        ajustarQuantidadeCanais(
            numberOfChannels
        )

    }

    /**
     * Ajusta a quantidade de canais do mixer.
     *
     * O limite permitido é de 5 a 16 canais.
     *
     * Quando canais são adicionados, eles recebem
     * uma configuração padrão.
     *
     * Quando canais são removidos, somente os canais
     * excedentes são descartados.
     */
    fun ajustarQuantidadeCanais(
        quantidade: Int
    ) {

        val novaQuantidade =
            quantidade.coerceIn(
                MIN_CHANNELS,
                MAX_CHANNELS
            )

        while (channels.size < novaQuantidade) {

            channels += ChannelState(
                channel = channels.size
            )

            masterOffsets.add(0)

        }

        while (channels.size > novaQuantidade) {

            channels.removeAt(
                channels.lastIndex
            )

            if (masterOffsets.isNotEmpty()) {

                masterOffsets.removeAt(
                    masterOffsets.lastIndex
                )

            }

        }

    }

    fun getChannel(
        index: Int
    ): ChannelState {

        return channels[index]

    }

    fun usaSoundFont(
        id: Int
    ): Boolean {

        return channels.any {

            it.soundFont?.id == id

        }

    }

    /**
     * Ativa o modo Master.
     *
     * Guarda a diferença de volume de cada canal
     * em relação ao canal 1 no momento da ativação.
     */
    fun ativarMaster() {

        if (channels.isEmpty())
            return

        val volumeMaster =
            channels[0].volume

        channels.forEachIndexed {
                index,
                channel ->

            masterOffsets[index] =
                channel.volume - volumeMaster

        }

        channel1AsMaster = true

    }

    /**
     * Desativa o modo Master.
     *
     * Os volumes dos canais permanecem com os valores
     * atuais e voltam a funcionar de forma independente.
     */
    fun desativarMaster() {

        channel1AsMaster = false

        masterOffsets.fill(0)

    }

    /**
     * Retorna o volume efetivo de um canal quando
     * o Master está ativo.
     *
     * O canal 1 é utilizado como referência.
     */
    fun calcularVolumeComMaster(
        canal: Int,
        masterVolume: Int
    ): Int {

        if (canal !in channels.indices)
            return 0

        if (!channel1AsMaster)
            return channels[canal].volume

        return (
                masterVolume +
                        masterOffsets[canal]
                ).coerceIn(
                0,
                127
            )

    }

    /**
     * Retorna o deslocamento armazenado para um canal.
     *
     * Útil para depuração e testes.
     */
    fun getMasterOffset(
        canal: Int
    ): Int {

        if (canal !in masterOffsets.indices)
            return 0

        return masterOffsets[canal]

    }

    /**
     * Exporta o estado atual do mixer para uma configuração.
     *
     * A quantidade de canais é determinada pelo tamanho
     * da própria lista.
     */
    fun exportarConfiguracao(): MixerConfiguration {

        channels.forEachIndexed {
                index,
                channel ->

            Log.d(
                "MixerState",
                "Exportando canal $index " +
                        "SF=${channel.soundFontId} " +
                        "Program=${channel.program} " +
                        "Volume=${channel.volume} " +
                        "Controle=${channel.controlSource}"
            )

        }

        return MixerConfiguration(

            channels =
                channels.map { channel ->

                    ChannelConfiguration(

                        enabled = true,

                        soundFontId =
                            channel.soundFontId,

                        bank =
                            channel.bankMsb,

                        program =
                            channel.program,

                        volume =
                            channel.volume,

                        mute =
                            channel.muted,

                        controlSource =
                            channel.controlSource

                    )

                }.toMutableList(),

            channel1AsMaster =
                channel1AsMaster

        )

    }

    /**
     * Aplica uma configuração ao estado do mixer.
     *
     * A quantidade de canais da configuração passa a ser
     * a quantidade de canais do mixer.
     *
     * Nesta etapa apenas atualiza o estado interno.
     * A sincronização com o FluidSynth será feita
     * posteriormente pelo SessionCoordinator.
     */
    fun aplicarConfiguracao(
        configuracao: MixerConfiguration
    ) {

        ajustarQuantidadeCanais(
            configuracao.channels.size
        )

        channel1AsMaster =
            configuracao.channel1AsMaster

        configuracao.channels.forEachIndexed {
                index,
                channelConfig ->

            if (index >= channels.size)
                return@forEachIndexed

            val channel =
                channels[index]

            channel.volume =
                channelConfig.volume

            channel.muted =
                channelConfig.mute

            channel.soundFontId =
                channelConfig.soundFontId

            channel.bankMsb =
                channelConfig.bank

            channel.program =
                channelConfig.program

            channel.controlSource =
                channelConfig.controlSource

            // A SoundFont e o Preset serão restaurados
            // posteriormente, após todas as SoundFonts
            // estarem carregadas.

        }

        /*
         * Se a sessão foi salva com o Master ativo,
         * reconstruímos os offsets a partir dos volumes
         * restaurados.
         */
        if (channel1AsMaster) {

            ativarMaster()

        } else {

            masterOffsets.fill(0)

        }

    }

}