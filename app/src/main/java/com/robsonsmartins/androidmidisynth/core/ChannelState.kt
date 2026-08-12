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

    var soundFontId by mutableIntStateOf(-1)

    /**
     * Controle de notas do acordeão que controla este canal.
     *
     * 1 = Controle 1
     * 2 = Controle 2
     * 3 = Controle 3
     *
     * Padrão:
     * - canais 1, 4, 5 e superiores → Controle 1
     * - canal 2 → Controle 2
     * - canal 3 → Controle 3
     */
    var controlSource by mutableIntStateOf(
        when (channel) {
            1 -> 2
            2 -> 3
            else -> 1
        }
    )

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