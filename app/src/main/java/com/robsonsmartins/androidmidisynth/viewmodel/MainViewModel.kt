package com.robsonsmartins.androidmidisynth.viewmodel

import android.media.midi.MidiDeviceInfo
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.robsonsmartins.androidmidisynth.audio.SynthController
import com.robsonsmartins.androidmidisynth.core.DeviceState
import com.robsonsmartins.androidmidisynth.core.MidiMixer
import com.robsonsmartins.androidmidisynth.core.MixerState
import com.robsonsmartins.androidmidisynth.soundfont.InstrumentItem
import com.robsonsmartins.androidmidisynth.soundfont.PresetInfo
import com.robsonsmartins.androidmidisynth.soundfont.SoundFontInfo
import com.robsonsmartins.androidmidisynth.soundfont.SoundFontManager
import com.robsonsmartins.androidmidisynth.sync.ConfigurationSynchronizer
import com.robsonsmartins.androidmidisynth.session.SessionCoordinator
import com.robsonsmartins.androidmidisynth.MidiManager

class MainViewModel : ViewModel() {

    private lateinit var synthController: SynthController

    private lateinit var soundFontManager: SoundFontManager

    private lateinit var configurationSynchronizer:
            ConfigurationSynchronizer

    private lateinit var sessionCoordinator:
            SessionCoordinator

    private lateinit var midiManager:
            MidiManager

    fun setSoundFontManager(
        manager: SoundFontManager
    ) {

        soundFontManager = manager

    }

    fun setSynthController(
        controller: SynthController
    ) {

        synthController = controller

    }

    fun setConfigurationSynchronizer(
        synchronizer: ConfigurationSynchronizer
    ) {

        configurationSynchronizer =
            synchronizer

    }

    fun setSessionCoordinator(
        coordinator: SessionCoordinator
    ) {

        sessionCoordinator =
            coordinator

    }

    fun setMidiManager(
        manager: MidiManager
    ) {

        midiManager = manager

        sincronizarControlSources()

    }

    /**
     * Envia para o código nativo a configuração atual
     * de controle de todos os canais.
     *
     * Cada canal MIDI Android pode responder a:
     *
     * 1 = Teclado
     * 2 = Baixos fundamentais
     * 3 = Acordes
     * 4 = Baixos + acordes
     */
    private fun sincronizarControlSources() {

        if (
            !::midiManager.isInitialized
        ) {

            return

        }

        mixerState.channels.forEach { channel ->

            midiManager.setControlSource(
                channel.channel,
                channel.controlSource
            )

        }

    }

    // =============================================================================
    // Mixer
    // =============================================================================
    fun ativarLedCanal(
        canal: Int
    ) {

        if (canal !in mixerState.channels.indices)
            return

        val channel =
            mixerState.getChannel(canal)

        channel.led = true

        // Desliga o LED após um pequeno intervalo.
        android.os.Handler(
            android.os.Looper.getMainLooper()
        ).postDelayed({

            channel.led = false

        }, 100)

    }

    private val midiMixer =
        MidiMixer()

    // =============================================================================
    // Estado da aplicação
    // =============================================================================

    val mixerState =
        MixerState()

    val deviceState =
        DeviceState()

    // =============================================================================
    // Quantidade de canais
    // =============================================================================

    fun getNumberOfChannels(): Int {

        return mixerState.channels.size

    }

    /**
     * Altera a quantidade de canais disponíveis.
     *
     * O limite é de 5 a 16 canais.
     *
     * Os canais existentes são preservados.
     * Novos canais recebem a configuração padrão.
     */
    fun setNumberOfChannels(
        quantidade: Int
    ) {

        val novaQuantidade =
            quantidade.coerceIn(
                MixerState.MIN_CHANNELS,
                MixerState.MAX_CHANNELS
            )

        if (
            novaQuantidade ==
            mixerState.channels.size
        ) {

            return

        }

        mixerState.ajustarQuantidadeCanais(
            novaQuantidade
        )

        salvarSessao()

    }

    // =============================================================================
    // Controle do acordeão
    // =============================================================================

    /**
     * Retorna qual controle do acordeão controla
     * determinado canal.
     *
     * O valor retornado é:
     *
     * 1 = Teclado
     * 2 = Baixos fundamentais
     * 3 = Acordes
     * 4 = Baixos + acordes
     */
    fun getChannelControlSource(
        channel: Int
    ): Int {

        if (
            channel !in
            mixerState.channels.indices
        ) {

            return 1

        }

        return mixerState
            .getChannel(channel)
            .controlSource

    }

    /**
     * Define qual controle do acordeão controla
     * determinado canal.
     *
     * O valor deve estar entre 1 e 4.
     *
     * 1 = Teclado
     * 2 = Baixos fundamentais
     * 3 = Acordes
     * 4 = Baixos + acordes
     */
    fun setChannelControlSource(
        channel: Int,
        controlSource: Int
    ) {

        if (
            channel !in
            mixerState.channels.indices
        ) {

            return

        }

        val novoControle =
            controlSource.coerceIn(
                1,
                4
            )

        val channelState =
            mixerState.getChannel(
                channel
            )

        if (
            channelState.controlSource ==
            novoControle
        ) {

            return

        }

        channelState.controlSource =
            novoControle

        Log.d(
            "MainViewModel",
            "Canal $channel " +
                    "controlSource=$novoControle"
        )

        if (
            ::midiManager.isInitialized
        ) {

            midiManager.setControlSource(
                channel,
                novoControle
            )

        }

        salvarSessao()

    }

    // =============================================================================
    // Volume
    // =============================================================================

    /**
     * Altera o volume de um canal.
     *
     * Quando o canal 1 está funcionando como Master,
     * somente ele pode alterar o volume.
     *
     * Os demais canais acompanham o Master utilizando
     * os offsets calculados no momento da ativação.
     */
    fun setChannelVolume(
        channel: Int,
        volume: Int
    ) {

        if (
            channel !in
            mixerState.channels.indices
        ) {

            return

        }

        val novoVolume =
            volume.coerceIn(
                0,
                127
            )

        // -------------------------------------------------------------------------
        // Master ativo
        // -------------------------------------------------------------------------

        if (mixerState.channel1AsMaster) {

            if (channel != 0)
                return

            mixerState
                .getChannel(0)
                .volume =
                novoVolume

            aplicarVolumesDoMaster(
                novoVolume
            )

            salvarSessao()

            return

        }

        // -------------------------------------------------------------------------
        // Master desligado
        // -------------------------------------------------------------------------

        aplicarVolumeIndividual(
            channel,
            novoVolume
        )

        salvarSessao()

    }

    /**
     * Aplica o volume individual de um canal.
     */
    private fun aplicarVolumeIndividual(
        channel: Int,
        volume: Int
    ) {

        val channelState =
            mixerState.getChannel(
                channel
            )

        channelState.volume =
            volume.coerceIn(
                0,
                127
            )

        if (!channelState.muted) {

            synthController.setVolume(
                channel,
                channelState.volume
            )

        }

        if (
            ::configurationSynchronizer
                .isInitialized
        ) {

            configurationSynchronizer.setVolume(
                channel,
                channelState.volume
            )

        }

    }

    /**
     * Aplica o volume do Master a todos os canais
     * utilizando os offsets calculados anteriormente.
     */
    private fun aplicarVolumesDoMaster(
        masterVolume: Int
    ) {

        val volumeMaster =
            masterVolume.coerceIn(
                0,
                127
            )

        mixerState.channels.forEach { channel ->

            val volumeEfetivo =
                mixerState.calcularVolumeComMaster(
                    channel.channel,
                    volumeMaster
                )

            channel.volume =
                volumeEfetivo

            if (!channel.muted) {

                synthController.setVolume(
                    channel.channel,
                    volumeEfetivo
                )

            }

            if (
                ::configurationSynchronizer
                    .isInitialized
            ) {

                configurationSynchronizer.setVolume(
                    channel.channel,
                    volumeEfetivo
                )

            }

            Log.d(
                "MainViewModel",
                "Master: canal=${channel.channel} " +
                        "offset=${mixerState.getMasterOffset(channel.channel)} " +
                        "volume=$volumeEfetivo"
            )

        }

    }

    /**
     * Ativa ou desativa o Canal 1 como Master.
     */
    fun setChannel1AsMaster(
        enabled: Boolean
    ) {

        if (enabled) {

            mixerState.ativarMaster()

            Log.d(
                "MainViewModel",
                "Canal 1 definido como Master"
            )

            aplicarVolumesDoMaster(
                mixerState
                    .getChannel(0)
                    .volume
            )

        } else {

            mixerState.desativarMaster()

            Log.d(
                "MainViewModel",
                "Canal 1 deixou de ser Master"
            )

        }

        salvarSessao()

    }

    /**
     * Retorna se o Canal 1 está funcionando como Master.
     */
    fun isChannel1AsMaster(): Boolean {

        return mixerState.channel1AsMaster

    }

    fun getChannel(
        channel: Int
    ) =
        midiMixer.getChannel(
            channel
        )

    // =============================================================================
    // Mute
    // =============================================================================

    /**
     * Altera o mute de um canal.
     *
     * O mute é sempre independente do Master.
     */
    fun setChannelMute(
        channel: Int,
        mute: Boolean
    ) {

        if (
            channel !in
            mixerState.channels.indices
        ) {

            return

        }

        Log.d(
            "MainViewModel",
            "Alterando mute canal=$channel mute=$mute"
        )

        val channelState =
            mixerState.getChannel(
                channel
            )

        channelState.muted =
            mute

        if (mute) {

            synthController.setVolume(
                channel,
                0
            )

        } else {

            val volumeEfetivo =
                if (
                    mixerState.channel1AsMaster
                ) {

                    mixerState.calcularVolumeComMaster(
                        channel,
                        mixerState
                            .getChannel(0)
                            .volume
                    )

                } else {

                    channelState.volume

                }

            synthController.setVolume(
                channel,
                volumeEfetivo
            )

        }

        salvarSessao()

    }

    fun isChannelMuted(
        channel: Int
    ): Boolean {

        if (
            channel !in
            mixerState.channels.indices
        ) {

            return false

        }

        return mixerState
            .getChannel(channel)
            .muted

    }

    fun getEffectiveChannelVolume(
        channel: Int
    ): Int {

        return if (
            midiMixer.isMuted(channel)
        ) {

            0

        } else {

            midiMixer.getVolume(channel)

        }

    }

    // =============================================================================
    // Program Change
    // =============================================================================

    fun setChannelProgram(
        channel: Int,
        program: Int
    ) {

        midiMixer.setProgram(
            channel,
            program
        )

    }

    fun getChannelProgram(
        channel: Int
    ): Int {

        return midiMixer.getProgram(
            channel
        )

    }

    // =============================================================================
    // Instrumentos
    // =============================================================================

    fun setChannelInstrument(
        channel: Int,
        soundFont: SoundFontInfo,
        preset: PresetInfo
    ) {

        if (
            channel !in
            mixerState.channels.indices
        ) {

            return

        }

        Log.d(
            "MainViewModel",
            "MixerState = ${System.identityHashCode(mixerState)}"
        )

        synthController.carregarSoundFont(
            soundFont
        )

        midiMixer.setSoundFont(
            channel,
            soundFont
        )

        midiMixer.setPreset(
            channel,
            preset
        )

        val channelState =
            mixerState.getChannel(
                channel
            )

        channelState.soundFont =
            soundFont

        channelState.soundFontId =
            soundFont.id

        channelState.preset =
            preset

        channelState.bankMsb =
            preset.bank

        channelState.program =
            preset.program

        Log.d(
            "MainViewModel",
            "Canal=$channel " +
                    "SF=${channelState.soundFontId} " +
                    "Program=${channelState.program}"
        )

        val midiChannel =
            midiMixer.getChannel(
                channel
            )

        midiChannel.soundFont =
            soundFont

        midiChannel.preset =
            preset

        midiChannel.bankMsb =
            preset.bank

        midiChannel.program =
            preset.program

        soundFont.presetSelecionado =
            preset

        synthController.setInstrument(
            channel,
            soundFont,
            preset
        )

        if (
            ::configurationSynchronizer
                .isInitialized
        ) {

            configurationSynchronizer.setInstrumento(
                channel,
                preset.program
            )

        }

        salvarSessao()

    }

    fun getChannels() =
        mixerState.channels

    fun toggleChannelMute(
        channel: Int
    ) {

        if (
            channel !in
            mixerState.channels.indices
        ) {

            return

        }

        val novoEstado =
            !mixerState
                .getChannel(channel)
                .muted

        setChannelMute(
            channel,
            novoEstado
        )

    }

    // =============================================================================
    // Áudio
    // =============================================================================

    var masterVolume by
    mutableFloatStateOf(
        0.8f
    )

    // =============================================================================
    // Sistema
    // =============================================================================

    var usoCpu by
    mutableIntStateOf(0)

    var bateria by
    mutableIntStateOf(100)

    // =============================================================================
    // BLE / MIDI
    // =============================================================================

    var dispositivosMidi by
    mutableStateOf<List<MidiDeviceInfo>>(
        emptyList()
    )

    var dispositivoConectado by
    mutableStateOf<MidiDeviceInfo?>(
        null
    )

    var bleConectado by
    mutableStateOf(false)

    // =============================================================================
    // SoundFont
    // =============================================================================

    fun listarSoundFonts() =
        soundFontManager.listar()

    fun carregarSoundFont(
        id: Int
    ) {

        soundFontManager.carregar(id)

        salvarSessao()

    }

    fun descarregarSoundFont(
        id: Int
    ) {

        val soundFont =
            soundFontManager.getSoundFont(id)
                ?: return

        /*
         * Antes de descarregar a SoundFont, limpa
         * o instrumento de todos os canais que a utilizam.
         */
        mixerState.channels.forEach { channel ->

            if (channel.soundFontId == id) {

                channel.soundFont = null
                channel.preset = null
                channel.soundFontId = -1
                channel.bankMsb = 0
                channel.bankLsb = 0
                channel.program = 0

                /*
                 * Mantém o estado paralelo do MidiMixer
                 * sincronizado com o ChannelState.
                 */
                val midiChannel =
                    midiMixer.getChannel(
                        channel.channel
                    )

                midiChannel.soundFont = null
                midiChannel.preset = null
                midiChannel.bankMsb = 0
                midiChannel.bankLsb = 0
                midiChannel.program = 0
            }
        }

        soundFontManager.descarregar(
            id
        )

        salvarSessao()

    }

    fun alternarSoundFont(
        id: Int
    ) {

        val soundFont =
            soundFontManager.getSoundFont(id)
                ?: return

        if (soundFont.carregada) {

            // Usa o método que também limpa os canais
            // que utilizavam esta SoundFont.
            descarregarSoundFont(id)

        } else {

            carregarSoundFont(id)

        }

    }

    fun importarSoundFont(
        uri: Uri
    ): Boolean {

        val sucesso =
            soundFontManager.importarSoundFont(
                uri
            )

        if (sucesso) {

            salvarSessao()

        }

        return sucesso

    }

    fun listarInstrumentos():
            List<InstrumentItem> {

        val instrumentos =
            mutableListOf<InstrumentItem>()

        soundFontManager
            .carregadas()
            .forEach { soundFont ->

                soundFont.presets.forEach { preset ->

                    instrumentos.add(

                        InstrumentItem(
                            soundFont =
                                soundFont,
                            preset =
                                preset
                        )

                    )

                }

            }

        return instrumentos.sortedBy {

            it.preset.nome.lowercase()

        }

    }

    fun sincronizarBiblioteca() {

        soundFontManager.sincronizarBiblioteca()

    }

    fun podeExcluirSoundFont(
        id: Int
    ): Boolean {

        val soundFont =
            soundFontManager
                .getSoundFont(id)
                ?: return false

        return soundFontManager.podeExcluir(
            soundFont
        ) {

            mixerState.usaSoundFont(
                it.id
            )

        }

    }

    fun excluirSoundFont(
        id: Int
    ): Boolean {

        return soundFontManager.excluir(id) {

            mixerState.usaSoundFont(
                it.id
            )

        }

    }

    fun salvarSessao() {

        if (
            ::sessionCoordinator
                .isInitialized
        ) {

            sessionCoordinator.salvar()

        }

    }

    fun excluirSoundFont(
        id: Int,
        estaEmUso:
            (SoundFontInfo) -> Boolean
    ): Boolean {

        val sucesso =
            soundFontManager.excluir(
                id,
                estaEmUso
            )

        if (sucesso) {

            salvarSessao()

        }

        return sucesso

    }

}