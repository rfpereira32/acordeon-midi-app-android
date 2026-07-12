/*
 * Copyright (c) 2024 Robson Martins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions.
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
// -----------------------------------------------------------------------------------------------
/**
 * @file cpp/SynthManager.h
 * @brief Header of SynthManager class.
 *
 * @author Robson Martins (https://www.robsonmartins.com)
 */
// -----------------------------------------------------------------------------------------------

#ifndef ANDROID_MIDI_SYNTH_SYNTHMANAGER_H
#define ANDROID_MIDI_SYNTH_SYNTHMANAGER_H

#include <fluidsynth.h>
#include <fluidsynth/types.h>
#include <fluidsynth/settings.h>
#include <fluidsynth/synth.h>

#include <vector>
#include "PresetInfoNative.h"
// -----------------------------------------------------------------------------------------------

/**
 * @brief SynthManager class.
 * @details The SynthManager encapsulates a native C/C++ FluidSynth synthesizer.
 */
class SynthManager {
public:

    /**
     * @brief Get an unique SynthManager instance.
     * @return A SynthManager instance.
     */
    static SynthManager* getInstance();

    /** @brief Free the unique SynthManager instance. */
    static void freeInstance();

    /**
     * @brief Load a soundfont file.
     * @param soundfontPath Full soundfont filename path.
     * @param program Program number to select (default = 0).
     * @return SoundFont ID (sfid) ou FLUID_FAILED.
     */
    int loadSF(
            const char *soundfontPath,
            int program = 0
    );

    /**
     * @brief Remove uma SoundFont previamente carregada.
     * @param sfid Identificador retornado por loadSF().
     */
    void unloadSF(
            int sfid
    );

    /**
     * @brief API antiga.
     * Mantida temporariamente para compatibilidade.
     */
    void programChange(
            int channel,
            int bank,
            int program
    );

    /**
     * @brief Seleciona explicitamente uma SoundFont,
     * banco e preset para um canal MIDI.
     *
     * @param channel Canal MIDI.
     * @param sfid SoundFont ID retornado por loadSF().
     * @param bank Banco do instrumento.
     * @param program Preset.
     */
    void programSelect(
            int channel,
            int sfid,
            int bank,
            int program
    );

    /**
 * @brief Retorna a instância interna do FluidSynth.
 *
 * Utilizado por classes auxiliares, como o SoundFontParser.
 */
    fluid_synth_t* getSynth() const;

/**
 * @brief Lista todos os presets existentes em uma SoundFont.
 *
 * @param sfid Identificador da SoundFont carregada.
 * @return Vetor contendo todos os presets encontrados.
 */
    std::vector<PresetInfoNative> listPresets(
            int sfid
    );

    /**
     * @brief Play a note.
     */
    void noteOn(
            int channel,
            int note,
            int velocity
    );

    void noteOff(
            int channel,
            int note
    );

    void sendCC(
            int channel,
            int controller,
            int value
    );

    void reverb(
            int level
    );

private:

    SynthManager();

    ~SynthManager();

    /**
     * @brief Set the FluidSynth latency.
     * @param ms Latency value, in milliseconds.
     */
    void setLatency(int ms);

private:

    static SynthManager *instance;

    fluid_settings_t *settings;

    fluid_synth_t *synth;

    fluid_audio_driver_t *driver;

    /**
     * Última SoundFont carregada.
     *
     * Será removido quando toda a aplicação
     * utilizar sfid por canal.
     */
    int soundfontId;
};

#endif //ANDROID_MIDI_SYNTH_SYNTHMANAGER_H