package com.robsonsmartins.androidmidisynth.soundfont

data class SoundFontInfo(

    val id: Int,

    val nome: String,

    val caminho: String,

    var carregada: Boolean = false,

    var quantidadePresets: Int = 0

)