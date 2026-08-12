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
 * @file cpp/MidiManager.h
 * @brief Header of MidiManager class.
 *
 * @author Robson Martins (https://www.robsonmartins.com)
 */
// -----------------------------------------------------------------------------------------------

#ifndef ANDROID_MIDI_SYNTH_MIDIMANAGER_H
#define ANDROID_MIDI_SYNTH_MIDIMANAGER_H

#include <jni.h>
#include <pthread.h>
#include <amidi/AMidi.h>

#include <array>
#include <atomic>
#include <set>
#include <sstream>

#include "SynthManager.h"

/** @brief Thread routine to polling MIDI commands. */
static void* readThreadRoutine(void *context);

// -----------------------------------------------------------------------------------------------

/**
 * @brief MidiManager class.
 * @details The MidiManager encapsulates a native MIDI listener.
 */
class MidiManager {
public:
    /**
     * @brief Get an unique MidiManager instance.
     * @param env JNI Env pointer
     * @param midiManagerObj MidiManager (Java) object
     * @param midiDeviceObj MidiDevice (Java) object.
     * @param portNumber The index of the "output" port to open.
     * @return A MidiManager instance.
     */
    static MidiManager* getInstance(JNIEnv* env, jobject midiManagerObj,
                                    jobject midiDeviceObj, jint portNumber);

    /** @brief Free the unique MidiManager instance. */
    static void freeInstance();

    /**
     * @brief Define qual controle do acordeão controla um canal MIDI.
     *
     * @param channel Índice do canal MIDI (0..15).
     * @param controlSource Controle do acordeão (1..3).
     */
    static void setControlSource(int channel, int controlSource);

private:
    /*
     * @brief Constructor.
     * @param env JNI Env pointer.
     * @param midiDeviceObj Java MidiDevice object.
     * @param portNumber The index of the "output" port to open.
     */
    MidiManager(JNIEnv* env, jobject midiDeviceObj, jint portNumber);

    /* @brief Destructor. */
    ~MidiManager();

    /*
     * @brief Parse MIDI Data.
     * @param data Data to parse.
     * @param numBytes Size of data to parse.
     */
    void parseMidiData(const uint8_t *data, size_t numBytes);

    /*
     * @brief Parse MIDI Control Commands.
     * @param controller Controller number.
     * @param value Value of the command.
     */
    void parseMidiCmdControl(uint8_t controller, uint8_t value);

    /*
     * @brief Send data to callback method.
     * @param data Data to send.
     * @param size Size of data.
     */
    static void sendToCallback(uint8_t* data, int size);

    /*
     * @overload
     * @brief Send data to callback method.
     * @param str Data to send.
     */
    static void sendToCallback(const char *str);

    /*
     * @overload
     * @brief Send data to callback method.
     * @param oss StringStream with data to send.
     */
    static void sendToCallback(const std::ostringstream& oss);

private:
    /* @brief MidiManager unique instance. */
    static MidiManager *instance;

    /* @brief MIDI Native Receive Device. */
    AMidiDevice* nativeReceiveDevice;

    /* @brief MIDI Native Output Port. */
    AMidiOutputPort* midiOutputPort;

    /* @brief MIDI events read thread. */
    pthread_t readThread;

    /* @brief Status of the thread: reading or not. */
    std::atomic<bool> reading;

    /* @brief Status of the sustain. */
    bool sustain;

    /* @brief Set of playing notes. */
    std::set<int> playNotes;

    /* @brief Set of sustaining notes. */
    std::set<int> sustainNotes;

    /**
     * @brief Controle do acordeão associado a cada canal MIDI.
     *
     * Índice:
     *   0 = canal MIDI 1
     *   1 = canal MIDI 2
     *   ...
     *  15 = canal MIDI 16
     *
     * Valor:
     *   1 = Controle 1
     *   2 = Controle 2
     *   3 = Controle 3
     */
    static std::array<uint8_t, 16> controlSources;

    /* @brief SynthManager unique instance. */
    static SynthManager* synthManager;

    /* @brief Java Virtual Machine unique instance. */
    static JavaVM* jvm;

    /* @brief JNI callback data object. */
    static jobject callbackObj;

    /* @brief JNI callback method. */
    static jmethodID callback;

    /* @brief Friend function: Thread routine to polling MIDI commands. */
    friend void* readThreadRoutine(void *context);
};

#endif //ANDROID_MIDI_SYNTH_MIDIMANAGER_H