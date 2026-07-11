/*
 * Copyright (c) 2024 Robson Martins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
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
    int loadSF(const char *soundfontPath, int program = 0);

    /**
     * @brief Remove uma SoundFont previamente carregada.
     * @param sfid Identificador retornado por loadSF().
     */
    void unloadSF(int sfid);

    /**
     * @brief Seleciona banco e programa para um canal MIDI.
     *
     * @param channel Canal MIDI.
     * @param bank Banco do instrumento.
     * @param program Programa (Preset).
     */
    void programChange(
            int channel,
            int bank,
            int program
    );

    /**
     * @brief Play a note.
     * @param note Note number.
     * @param velocity The velocity of the note.
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

    /* @brief Constructor. */
    SynthManager();

    /* @brief Destructor. */
    ~SynthManager();

    /**
     * @brief Set the FluidSynth latency.
     * @param ms Latency value, in milliseconds.
     */
    void setLatency(int ms);

private:

    /* @brief SynthManager unique instance. */
    static SynthManager *instance;

    /* @brief FluidSynth settings. */
    fluid_settings_t *settings;

    /* @brief FluidSynth synth object. */
    fluid_synth_t *synth;

    /* @brief FluidSynth audio driver object. */
    fluid_audio_driver_t *driver;

    /* @brief Última SoundFont carregada (mantido para compatibilidade). */
    int soundfontId;
};

#endif //ANDROID_MIDI_SYNTH_SYNTHMANAGER_H