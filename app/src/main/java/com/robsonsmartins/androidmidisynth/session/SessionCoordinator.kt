package com.robsonsmartins.androidmidisynth.session

import android.util.Log
import com.robsonsmartins.androidmidisynth.core.MixerState
import com.robsonsmartins.androidmidisynth.soundfont.PresetInfo
import com.robsonsmartins.androidmidisynth.soundfont.SoundFontInfo
import com.robsonsmartins.androidmidisynth.soundfont.SoundFontManager

/**
 * Coordena a persistência da sessão do aplicativo.
 *
 * Esta classe reúne o estado dos diferentes componentes
 * (biblioteca de SoundFonts, mixer, etc.) e delega ao
 * SessionManager a gravação e leitura do arquivo JSON.
 */
class SessionCoordinator(

    private val sessionManager:
    SessionManager,

    private val soundFontManager:
    SoundFontManager,

    private val mixerState:
    MixerState,

    private val restaurarCanal:
        (Int, SoundFontInfo, PresetInfo) -> Unit,

    private val restaurarVolume:
        (Int, Int, Boolean) -> Unit

) {

    fun salvar() {

        Log.d(
            "SessionCoordinator",
            "MixerState = " +
                    "${System.identityHashCode(mixerState)}"
        )

        val session =
            SessionState()

        //------------------------------------------------------
        // Biblioteca de SoundFonts
        //------------------------------------------------------

        soundFontManager.listar().forEach { soundFont ->

            session.soundFonts.add(

                SessionSoundFont(

                    id =
                        soundFont.id,

                    nome =
                        soundFont.nome,

                    arquivo =
                        soundFont.getArquivo().name,

                    carregada =
                        soundFont.carregada

                )

            )

        }

        //------------------------------------------------------
        // Mixer
        //------------------------------------------------------

        session.mixer =
            mixerState.exportarConfiguracao()

        //------------------------------------------------------

        sessionManager.salvar(
            session
        )

    }

    fun restaurar() {

        val session =
            sessionManager.carregar()

        soundFontManager.aplicarSessao(
            session.soundFonts
        )

        soundFontManager.sincronizarBiblioteca()

        /*
         * MixerState ajusta automaticamente sua quantidade
         * de canais de acordo com a quantidade salva na sessão.
         */
        mixerState.aplicarConfiguracao(
            session.mixer
        )

    }

    fun restaurarInstrumentos() {

        mixerState.channels.forEachIndexed {
                index,
                channel ->

            if (channel.soundFontId < 0)
                return@forEachIndexed

            val instrumento =
                soundFontManager.localizarInstrumento(
                    channel.soundFontId,
                    channel.bankMsb,
                    channel.program
                )

            if (instrumento == null) {

                Log.d(
                    "SessionCoordinator",
                    "Canal $index: instrumento não encontrado"
                )

                return@forEachIndexed

            }

            restaurarCanal(
                index,
                instrumento.first,
                instrumento.second
            )

        }

        //------------------------------------------------------
        // Reaplica volumes e mute
        //------------------------------------------------------

        mixerState.channels.forEach { channel ->

            restaurarVolume(

                channel.channel,

                channel.volume,

                channel.muted

            )

        }

    }

}