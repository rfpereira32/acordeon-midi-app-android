package com.robsonsmartins.androidmidisynth.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.robsonsmartins.androidmidisynth.soundfont.PresetInfo
import com.robsonsmartins.androidmidisynth.soundfont.SoundFontInfo

class ChannelState(

    val channel: Int

) {

    var volume by mutableIntStateOf(100)

    var muted by mutableStateOf(false)

    var expression by mutableIntStateOf(127)

    var pan by mutableIntStateOf(64)

    var bankMsb by mutableIntStateOf(0)

    var bankLsb by mutableIntStateOf(0)

    var program by mutableIntStateOf(0)

    /**
     * Instrumento atualmente selecionado.
     */
    var preset by mutableStateOf<PresetInfo?>(null)

    /**
     * SoundFont atualmente utilizada.
     */
    var soundFont by mutableStateOf<SoundFontInfo?>(null)

    var led by mutableStateOf(false)

}