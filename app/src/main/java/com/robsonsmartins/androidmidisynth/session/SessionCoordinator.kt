package com.robsonsmartins.androidmidisynth.session

import com.robsonsmartins.androidmidisynth.core.MixerState
import com.robsonsmartins.androidmidisynth.soundfont.SoundFontManager

/**
 * Coordena a persistência da sessão do aplicativo.
 *
 * Esta classe reúne o estado dos diferentes componentes
 * (biblioteca de SoundFonts, mixer, etc.) e delega ao
 * SessionManager a gravação e leitura do arquivo JSON.
 */
class SessionCoordinator(

    private val sessionManager: SessionManager,

    private val soundFontManager: SoundFontManager,

    private val mixerState: MixerState

) {

    fun salvar() {

        val session = SessionState()

        //------------------------------------------------------
        // Biblioteca de SoundFonts
        //------------------------------------------------------

        soundFontManager.listar().forEach { soundFont ->

            session.soundFonts.add(

                SessionSoundFont(

                    id = soundFont.id,

                    nome = soundFont.nome,

                    arquivo = soundFont.getArquivo().name,

                    carregada = soundFont.carregada

                )

            )

        }

        //------------------------------------------------------
        // Mixer
        //------------------------------------------------------

        session.mixer =
            mixerState.exportarConfiguracao()

        //------------------------------------------------------

        sessionManager.salvar(session)

    }

    fun restaurar() {

        val session = sessionManager.carregar()

        soundFontManager.aplicarSessao(

            session.soundFonts

        )

    }

}