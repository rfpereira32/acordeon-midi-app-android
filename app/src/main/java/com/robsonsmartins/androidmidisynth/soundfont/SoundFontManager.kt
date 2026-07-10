package com.robsonsmartins.androidmidisynth.soundfont

class SoundFontManager {

    private val soundFonts = mutableListOf<SoundFontInfo>()

    fun listar(): List<SoundFontInfo> =
        soundFonts

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

    fun carregadas(): List<SoundFontInfo> =
        soundFonts.filter { it.carregada }

    fun disponiveis(): List<SoundFontInfo> =
        soundFonts.filter { !it.carregada }
}