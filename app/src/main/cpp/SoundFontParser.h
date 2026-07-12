/*
 * Copyright (c) 2024 Robson Martins
 */

#ifndef ANDROID_MIDI_SYNTH_SOUNDFONTPARSER_H
#define ANDROID_MIDI_SYNTH_SOUNDFONTPARSER_H

#include <vector>

#include <fluidsynth.h>

#include "PresetInfoNative.h"

/**
 * Responsável por inspecionar uma SoundFont carregada
 * no FluidSynth.
 *
 * Esta classe não produz áudio.
 * Ela apenas consulta informações da SoundFont.
 */
class SoundFontParser
{
public:

    static std::vector<PresetInfoNative> listPresets(
            fluid_synth_t* synth,
            int sfid
    );
};

#endif