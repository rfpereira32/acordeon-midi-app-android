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
 * @file cpp/MidiManager.cpp
 * @brief Implementation of MidiManager class.
 *
 * @author Robson Martins (https://www.robsonmartins.com)
 */
// -----------------------------------------------------------------------------------------------

#include <unistd.h>

#include <cstdio>
#include <string>
#include <sstream>
#include <cstring>
#include <array>
#include <atomic>

#include "MidiSpec.h"
#include "MidiManager.h"
#include "SynthManager.h"

/* @brief Buffer size to receive data from MIDI, in bytes. */
static const size_t kMidiMaxBytesToReceive = 128;

// -----------------------------------------------------------------------------------------------

MidiManager* MidiManager::instance = nullptr;
JavaVM* MidiManager::jvm = nullptr;
SynthManager* MidiManager::synthManager = nullptr;
jobject MidiManager::callbackObj = nullptr;
jmethodID MidiManager::callback = nullptr;

/*
 * Controle do acordeão associado a cada canal MIDI do sintetizador.
 *
 * Índice:
 *
 * 0  = canal MIDI 1
 * 1  = canal MIDI 2
 * ...
 * 15 = canal MIDI 16
 *
 * Valor:
 *
 * 1 = Controle 1
 * 2 = Controle 2
 * 3 = Controle 3
 *
 * Todos começam associados ao Controle 1.
 */
std::array<uint8_t, 16> MidiManager::controlSources = {
        1, 1, 1, 1,
        1, 1, 1, 1,
        1, 1, 1, 1,
        1, 1, 1, 1
};

// -----------------------------------------------------------------------------------------------

MidiManager::MidiManager(
        JNIEnv* env,
        jobject midiDeviceObj,
        jint portNumber
) : reading(false), sustain(false) {

    synthManager =
            SynthManager::getInstance();

    AMidiDevice* device;

    AMidiDevice_fromJava(
            env,
            midiDeviceObj,
            &device
    );

    nativeReceiveDevice =
            device;

    AMidiOutputPort* port;

    AMidiOutputPort_open(
            nativeReceiveDevice,
            portNumber,
            &port
    );

    midiOutputPort =
            port;

    pthread_t thread;

    pthread_create(
            &thread,
            nullptr,
            readThreadRoutine,
            this
    );

    readThread =
            thread;

}

// -----------------------------------------------------------------------------------------------

MidiManager::~MidiManager() {

    reading =
            false;

    pthread_join(
            readThread,
            nullptr
    );

    AMidiDevice_release(
            nativeReceiveDevice
    );

}

// -----------------------------------------------------------------------------------------------

MidiManager* MidiManager::getInstance(
        JNIEnv* env,
        jobject midiManagerObj,
        jobject midiDeviceObj,
        jint portNumber
) {

    if (!instance) {

        // setup the receive data callback (into Java)

        env->GetJavaVM(
                &jvm
        );

        jclass objectClass =
                env->GetObjectClass(
                        midiManagerObj
                );

        callbackObj =
                env->NewGlobalRef(
                        midiManagerObj
                );

        callback =
                env->GetMethodID(
                        objectClass,
                        "onNativeMessageReceive",
                        "([B)V"
                );

        instance =
                new MidiManager(
                        env,
                        midiDeviceObj,
                        portNumber
                );

    }

    return instance;

}

// -----------------------------------------------------------------------------------------------

void MidiManager::freeInstance() {

    if (instance) {

        delete instance;

        instance =
                nullptr;

    }

}

// -----------------------------------------------------------------------------------------------

void MidiManager::setControlSource(
        int channel,
        int controlSource
) {

    if (
            channel < 0 ||
            channel >= 16
            ) {
        return;
    }

    if (
            controlSource < 1 ||
            controlSource > 3
            ) {
        return;
    }

    controlSources[channel] =
            static_cast<uint8_t>(
                    controlSource
            );

}

// -----------------------------------------------------------------------------------------------

void MidiManager::parseMidiData(
        const uint8_t *data,
        size_t numBytes
) {

    if (
            numBytes < 3
            ) {
        return;
    }

    uint8_t status =
            data[0] & 0xFF;

    uint8_t midiChannel =
            status & 0x0F;

    uint8_t note =
            data[1] & 0xFF;

    uint8_t velocity =
            data[2] & 0xFF;

    std::ostringstream oss;

    /*
     * O ESP envia somente três canais de controle:
     *
     * MIDI 0 -> Controle 1
     * MIDI 1 -> Controle 2
     * MIDI 2 -> Controle 3
     *
     * Qualquer outro canal recebido continua sendo
     * ignorado para o roteamento.
     */
    uint8_t controlSource =
            0;

    if (
            midiChannel < 3
            ) {

        controlSource =
                midiChannel + 1;

    }

    switch (
            (status & kMIDISysCmdChan) >> 4
            ) {

        // ================================================================
        // NOTE OFF
        // ================================================================

        case kMIDIChanCmd_NoteOff:

            if (
                    controlSource == 0
                    ) {
                break;
            }

            /*
             * Se o Sustain estiver desligado, desligamos a nota
             * em todos os canais que estavam respondendo
             * ao controle no momento do Note On.
             *
             * Isso é importante porque uma única nota pode ter
             * sido enviada para vários canais.
             */
            if (!sustain) {

                for (
                        int channel = 0;
                        channel < 16;
                        channel++
                        ) {

                    if (
                            controlSources[channel]
                            == controlSource
                            ) {

                        synthManager->noteOff(
                                channel,
                                note
                        );

                    }

                }

                sustainNotes.erase(
                        note
                );

            }

            playNotes.erase(
                    note
            );

            break;

            // ================================================================
            // NOTE ON
            // ================================================================

        case kMIDIChanCmd_NoteOn:

            /*
             * MIDI Note On com velocity 0 equivale a Note Off.
             */
            if (
                    velocity == 0
                    ) {

                if (
                        controlSource == 0
                        ) {
                    break;
                }

                if (!sustain) {

                    for (
                            int channel = 0;
                            channel < 16;
                            channel++
                            ) {

                        if (
                                controlSources[channel]
                                == controlSource
                                ) {

                            synthManager->noteOff(
                                    channel,
                                    note
                            );

                        }

                    }

                    sustainNotes.erase(
                            note
                    );

                }

                playNotes.erase(
                        note
                );

                break;

            }

            if (
                    controlSource == 0
                    ) {
                break;
            }

            if (sustain) {

                sustainNotes.insert(
                        note
                );

            }

            playNotes.insert(
                    note
            );

/*
 * A nota recebida do ESP é enviada para todos
 * os canais Android associados ao controle.
 */
            for (
                    int channel = 0;
                    channel < 16;
                    channel++
                    ) {

                if (
                        controlSources[channel]
                        == controlSource
                        ) {

                    synthManager->noteOn(
                            channel,
                            note,
                            velocity
                    );

                    /*
                     * Informa ao Android que este canal
                     * recebeu atividade MIDI.
                     *
                     * O canal é enviado como um único byte,
                     * evitando criar mensagens de texto.
                     */
                    uint8_t activityMessage =
                            static_cast<uint8_t>(channel);

                    sendToCallback(
                            &activityMessage,
                            1
                    );

                }

            }

            break;

            // ================================================================
            // CONTROL CHANGE
            // ================================================================

        case kMIDIChanCmd_Control:

            /*
             * Os controles MIDI enviados pelo ESP também
             * pertencem a um dos três controles físicos.
             */
            parseMidiCmdControl(
                    note,
                    velocity
            );

            break;

            // ================================================================
            // KEY PRESS
            // ================================================================

        case kMIDIChanCmd_KeyPress:

            oss.clear();

            oss
                    << "Key Press: "
                    << (int)note
                    << " vel: "
                    << (int)velocity
                    << " status: "
                    << (int)status;

            sendToCallback(
                    oss
            );

            break;

            // ================================================================
            // PROGRAM CHANGE
            // ================================================================

        case kMIDIChanCmd_ProgramChange:

            oss.clear();

            oss
                    << "Program Change: "
                    << (int)note
                    << " vel: "
                    << (int)velocity
                    << " status: "
                    << (int)status;

            sendToCallback(
                    oss
            );

            break;

            // ================================================================
            // CHANNEL PRESSURE
            // ================================================================

        case kMIDIChanCmd_ChannelPress:

            oss.clear();

            oss
                    << "Channel Press: "
                    << (int)note
                    << " vel: "
                    << (int)velocity
                    << " status: "
                    << (int)status;

            sendToCallback(
                    oss
            );

            break;

            // ================================================================
            // PITCH WHEEL
            // ================================================================

        case kMIDIChanCmd_PitchWheel:

            oss.clear();

            oss
                    << "Pitch Wheel: "
                    << (int)note
                    << " vel: "
                    << (int)velocity
                    << " status: "
                    << (int)status;

            sendToCallback(
                    oss
            );

            break;

            // ================================================================
            // DEFAULT
            // ================================================================

        default:

            oss.clear();

            oss
                    << "Unparsed: "
                    << (int)note
                    << " vel: "
                    << (int)velocity
                    << " status: "
                    << (int)status;

            sendToCallback(
                    oss
            );

            break;

    }

}

// -----------------------------------------------------------------------------------------------

void MidiManager::parseMidiCmdControl(
        uint8_t controller,
        uint8_t value
) {

    std::ostringstream oss;

    switch (
            controller
            ) {

        // ================================================================
        // SUSTAIN
        // ================================================================

        case kMIDIControl_Sustain:

            if (
                    value >= kMIDIControl_Sustain_level
                    ) {

                oss.clear();

                oss
                        << "Sustain ON: level: "
                        << (int)value;

                sendToCallback(
                        oss
                );

                sustain =
                        true;

                if (
                        !playNotes.empty()
                        ) {

                    sustainNotes.insert(
                            playNotes.begin(),
                            playNotes.end()
                    );

                }

            } else {

                oss.clear();

                oss
                        << "Sustain OFF: level: "
                        << (int)value;

                sendToCallback(
                        oss
                );

                sustain =
                        false;

                /*
                 * Stop all sustained notes that are no longer
                 * physically pressed.
                 *
                 * Como uma nota pode estar presente em vários
                 * canais, enviamos Note Off para todos eles.
                 */
                for (
                    const auto n : sustainNotes
                        ) {

                    if (
                            playNotes.find(n)
                            == playNotes.end()
                            ) {

                        for (
                                int channel = 0;
                                channel < 16;
                                channel++
                                ) {

                            synthManager->noteOff(
                                    channel,
                                    n
                            );

                        }

                    }

                }

                sustainNotes.clear();

            }

            break;

            // ================================================================
            // REVERB
            // ================================================================

        case kMIDIControl_Reverb:

            oss.clear();

            oss
                    << "Reverb: level: "
                    << (int)value;

            sendToCallback(
                    oss
            );

            synthManager->reverb(
                    value
            );

            break;

            // ================================================================
            // OUTROS CONTROL CHANGE
            // ================================================================

        default:

            oss.clear();

            oss
                    << "Unparsed command: controller: "
                    << (int)controller
                    << " value: "
                    << (int)value;

            sendToCallback(
                    oss
            );

            /*
             * Mantemos o comportamento anterior:
             * outros CC continuam sendo enviados ao canal 0.
             */
            synthManager->sendCC(
                    0,
                    controller,
                    value
            );

            break;

    }

}

// -----------------------------------------------------------------------------------------------

void MidiManager::sendToCallback(
        uint8_t* data,
        int size
) {

    JNIEnv* env;

    jvm->AttachCurrentThread(
            &env,
            nullptr
    );

    if (!env)
        return;

    // allocate the Java array and fill with received data

    jbyteArray ret =
            env->NewByteArray(
                    size
            );

    env->SetByteArrayRegion(
            ret,
            0,
            size,
            (jbyte*)data
    );

    // send it to the (Java) callback

    env->CallVoidMethod(
            callbackObj,
            callback,
            ret
    );

}

// -----------------------------------------------------------------------------------------------

void MidiManager::sendToCallback(
        const char *str
) {

    if (
            !str ||
            str[0] == 0
            ) {
        return;
    }

    sendToCallback(
            (uint8_t*)str,
            (int)strlen(str) + 1
    );

}

// -----------------------------------------------------------------------------------------------

void MidiManager::sendToCallback(
        const std::ostringstream& oss
) {

    sendToCallback(
            oss.str().c_str()
    );

}

// -----------------------------------------------------------------------------------------------

/*
 * This routine polls the input port and parses received data.
 */
static void* readThreadRoutine(
        void *context
) {

    auto *manager =
            (MidiManager*)context;

    manager->reading =
            true;

    AMidiOutputPort* outputPort =
            manager->midiOutputPort;

    uint8_t incomingMessage[
            kMidiMaxBytesToReceive
    ];

    int32_t opcode;

    size_t numBytesReceived;

    int64_t timestamp;

    ssize_t numMessagesReceived;

    while (
            manager->reading
            ) {

        numMessagesReceived =
                AMidiOutputPort_receive(
                        outputPort,
                        &opcode,
                        incomingMessage,
                        kMidiMaxBytesToReceive,
                        &numBytesReceived,
                        &timestamp
                );

        if (
                numMessagesReceived < 0
                ) {

            // failure receiving MIDI data: exit the thread

            manager->reading =
                    false;

        }

        /*
         * Omni Mode:
         *
         * Qualquer byte de nota vindo do Cordovox
         * passa diretamente para o parser.
         */
        if (
                numMessagesReceived > 0 &&
                numBytesReceived >= 0 &&
                opcode == AMIDI_OPCODE_DATA
                ) {

            manager->parseMidiData(
                    incomingMessage,
                    numBytesReceived
            );

        }

        // AMidiOutputPort_receive is non-blocking

        usleep(500);

    }

    return nullptr;

}

// -----------------------------------------------------------------------------------------------

extern "C" {

/**
 * @brief Native implementation of MidiManager.startReadingMidi() method.
 */
JNIEXPORT void JNICALL
Java_com_robsonmartins_androidmidisynth_MidiManager_startReadingMidi(
        JNIEnv* env,
        jobject midiManagerObj,
        jobject midiDeviceObj,
        jint portNumber
) {

    // starts the midi manager

    MidiManager::getInstance(
            env,
            midiManagerObj,
            midiDeviceObj,
            portNumber
    );

}

/**
 * @brief Native implementation of MidiManager.stopReadingMidi() method.
 */
JNIEXPORT void JNICALL
Java_com_robsonmartins_androidmidisynth_MidiManager_stopReadingMidi(
        JNIEnv*,
        jobject
) {

    MidiManager::freeInstance();

}

/**
 * @brief Native implementation of MidiManager.setNativeControlSource().
 */
JNIEXPORT void JNICALL
Java_com_robsonmartins_androidmidisynth_MidiManager_setNativeControlSource(
        JNIEnv*,
        jobject,
        jint channel,
        jint controlSource
) {

    MidiManager::setControlSource(
            channel,
            controlSource
    );

}

} // extern "C"

// -----------------------------------------------------------------------------------------------

/**
 * Registra explicitamente os métodos JNI da classe MidiManager.
 *
 * Isso evita depender da descoberta automática dos nomes
 * Java_com_... pelo Android Runtime.
 */
JNIEXPORT jint JNICALL
JNI_OnLoad(
        JavaVM* vm,
        void*
) {

    JNIEnv* env = nullptr;

    if (
            vm->GetEnv(
                    reinterpret_cast<void**>(&env),
                    JNI_VERSION_1_6
            ) != JNI_OK
            ) {

        return JNI_ERR;

    }

    jclass midiManagerClass =
            env->FindClass(
                    "com/robsonsmartins/androidmidisynth/MidiManager"
            );

    if (
            midiManagerClass == nullptr
            ) {

        return JNI_ERR;

    }

    static JNINativeMethod methods[] = {

            {
                    "startReadingMidi",
                    "(Landroid/media/midi/MidiDevice;I)V",
                    reinterpret_cast<void*>(
                            Java_com_robsonmartins_androidmidisynth_MidiManager_startReadingMidi
                    )
            },

            {
                    "stopReadingMidi",
                    "()V",
                    reinterpret_cast<void*>(
                            Java_com_robsonmartins_androidmidisynth_MidiManager_stopReadingMidi
                    )
            },

            {
                    "setNativeControlSource",
                    "(II)V",
                    reinterpret_cast<void*>(
                            Java_com_robsonmartins_androidmidisynth_MidiManager_setNativeControlSource
                    )
            }

    };

    if (
            env->RegisterNatives(
                    midiManagerClass,
                    methods,
                    sizeof(methods) / sizeof(methods[0])
            ) != JNI_OK
            ) {

        env->DeleteLocalRef(
                midiManagerClass
        );

        return JNI_ERR;

    }

    env->DeleteLocalRef(
            midiManagerClass
    );

    return JNI_VERSION_1_6;

}