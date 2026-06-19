package com.robsonmartins.androidmidisynth.model

import androidx.compose.ui.graphics.Color

data class MidiChannel(

    val id: Int,

    var instrument: String,

    var volume: Float,

    var muted: Boolean = false,

    var activity: Boolean = false,

    val color: Color

)