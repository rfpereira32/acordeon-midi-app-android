package com.robsonsmartins.androidmidisynth.session

import com.robsonsmartins.androidmidisynth.configuration.MixerConfiguration

data class SessionSoundFont(

    val id: Int,

    val nome: String,

    val arquivo: String,

    val carregada: Boolean

)

data class SessionState(

    val soundFonts: MutableList<SessionSoundFont> =
        mutableListOf(),

    var mixer: MixerConfiguration =
        MixerConfiguration()

)