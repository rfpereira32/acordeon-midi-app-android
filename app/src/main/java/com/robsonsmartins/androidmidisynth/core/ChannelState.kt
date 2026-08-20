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
     * 1 = Teclado
     * 2 = Baixos fundamentais
     * 3 = Acordes
     * 4 = Baixos + acordes
     *
     * Padrão:
     * - canal 1 → Teclado
     * - canal 2 → Baixos fundamentais
     * - canal 3 → Acordes
     * - canais 4 e superiores → Baixos + acordes
     */
    var controlSource by mutableIntStateOf(
        when (channel) {
            0 -> 1
            1 -> 2
            2 -> 3
            else -> 4
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