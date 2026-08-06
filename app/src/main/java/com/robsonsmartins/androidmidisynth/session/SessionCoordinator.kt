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

)