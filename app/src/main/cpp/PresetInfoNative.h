/*
 * Copyright (c) 2024 Robson Martins
 */

#pragma once

#include <string>

/**
 * @brief Representa um preset existente em uma SoundFont.
 *
 * Esta estrutura é utilizada apenas na camada nativa.
 * Posteriormente será convertida para PresetInfo
 * no Kotlin através do JNI.
 */
struct PresetInfoNative
{
    /**
     * Banco MIDI.
     */
    int bank = 0;

    /**
     * Programa (Preset).
     */
    int program = 0;

    /**
     * Nome do preset.
     */
    std::string name;
};