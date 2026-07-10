package com.robsonsmartins.androidmidisynth.soundfont

import androidx.compose.runtime.mutableStateListOf

class SoundFontManager {

    // Lista observável pelo Compose
    private val soundFonts = mutableStateListOf<SoundFontInfo>()

    init {

        // Temporário: lista fictícia para desenvolvimento da interface

        soundFonts.add(
            SoundFontInfo(
                id = 1,
                nome = "Acordeoes.sf2",
                caminho = "",
                carregada = true
            )
        )

        soundFonts.add(
            SoundFontInfo(
                id = 2,
                nome = "Baixos.sf2",
                caminho = "",
                carregada = true
            )
        )

        soundFonts.add(
            SoundFontInfo(
                id = 3,
                nome = "GeneralUser.sf2",
                caminho = "",
                carregada = false
            )
        )

        soundFonts.add(
            SoundFontInfo(
                id = 4,
                nome = "Strings.sf2",
                caminho = "",
                carregada = false
            )
        )
    }

    fun listar(): List<SoundFontInfo> = soundFonts

    fun adicionar(soundFont: SoundFontInfo) {
        soundFonts.add(soundFont)
    }

    fun remover(soundFont: SoundFontInfo) {
        soundFonts.remove(soundFont)
    }

    fun carregar(id: Int) {
        soundFonts.find { it.id == id }?.carregada = true
    }

    fun descarregar(id: Int) {
        soundFonts.find { it.id == id }?.carregada = false
    }

    fun alternar(id: Int) {

        soundFonts.find { it.id == id }?.let {

            it.carregada = !it.carregada

        }

    }

    fun carregadas(): List<SoundFontInfo> =
        soundFonts.filter { it.carregada }

    fun disponiveis(): List<SoundFontInfo> =
        soundFonts.filter { !it.carregada }

}