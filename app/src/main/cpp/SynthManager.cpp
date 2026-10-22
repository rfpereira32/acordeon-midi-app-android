
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
 * @file cpp/SynthManager.cpp
 * @brief Implementation of SynthManager class.
 *
 * @author Robson Martins (https://www.robsonmartins.com)
 */
// -----------------------------------------------------------------------------------------------

#include <jni.h>
#include <unistd.h>

#include "MidiSpec.h"
#include "SynthManager.h"
#include "SoundFontParser.h"

#include <android/log.h>
#include <cstdio>

// -----------------------------------------------------------------------------------------------

#define LOG_TAG "FluidSynth"

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/* @brief Default sample rate of the FluidSynth, in kHz. */
static const int kFluidSynthSampleRate = 48000;

/* @brief Default latency of the FluidSynth, in ms. */
static const int kFluidSynthLatency = 10;

/* @brief Calculate the buffer size based in latency value (ms). */
#define LATENCY_TO_BUFFER_SIZE(x) (kFluidSynthSampleRate * (x) / 1000.0)

// -----------------------------------------------------------------------------------------------
// Encaminha mensagens internas do FluidSynth para o Logcat.
// -----------------------------------------------------------------------------------------------

static void fluidSynthAndroidLog(int level, const char *message, void *) {
    if (message == nullptr) {
        return;
    }

    switch (level) {
        case FLUID_PANIC:
        case FLUID_ERR:
            __android_log_print(
                    ANDROID_LOG_ERROR,
                    "FluidSynth",
                    "Mensagem interna do FluidSynth: %s",
                    message
            );
            break;

        case FLUID_WARN:
            __android_log_print(
                    ANDROID_LOG_WARN,
                    "FluidSynth",
                    "Mensagem interna do FluidSynth: %s",
                    message
            );
            break;

        case FLUID_INFO:
            __android_log_print(
                    ANDROID_LOG_INFO,
                    "FluidSynth",
                    "Mensagem interna do FluidSynth: %s",
                    message
            );
            break;

        default:
            __android_log_print(
                    ANDROID_LOG_DEBUG,
                    "FluidSynth",
                    "Mensagem interna do FluidSynth: %s",
                    message
            );
            break;
    }
}

// -----------------------------------------------------------------------------------------------

SynthManager* SynthManager::instance = nullptr;

SynthManager::SynthManager() : soundfontId(-1) {
    LOGI("Inicializando SynthManager.");

    // Registra os diagnósticos do FluidSynth no Logcat.
    fluid_set_log_function(FLUID_PANIC, fluidSynthAndroidLog, nullptr);
    fluid_set_log_function(FLUID_ERR, fluidSynthAndroidLog, nullptr);
    fluid_set_log_function(FLUID_WARN, fluidSynthAndroidLog, nullptr);

    // Configura o sintetizador.
    settings = new_fluid_settings();

    if (settings == nullptr) {
        LOGE("Falha ao criar as configurações do FluidSynth.");
        synth = nullptr;
        driver = nullptr;
        return;
    }

//    fluid_settings_setstr(settings, "audio.driver", "opensles");
    fluid_settings_setstr(settings, "audio.driver", "oboe");
    fluid_settings_setint(settings, "audio.realtime-prio", 99);
    fluid_settings_setint(settings, "audio.periods", 2);
    fluid_settings_setint(settings, "synth.cpu-cores", 4);
    fluid_settings_setnum(settings, "synth.gain", 0.6);
    fluid_settings_setstr(settings, "audio.oboe.performance-mode", "LowLatency");
    fluid_settings_setstr(settings, "audio.oboe.sharing-mode", "Exclusive");
    fluid_settings_setnum(settings, "synth.sample-rate", kFluidSynthSampleRate);

    setLatency(kFluidSynthLatency);

    LOGI(
            "Configuração de áudio: sampleRate=%d, latency=%d ms.",
            kFluidSynthSampleRate,
            kFluidSynthLatency
    );

    synth = new_fluid_synth(settings);

    if (synth == nullptr) {
        LOGE("Falha ao criar o sintetizador FluidSynth.");

        delete_fluid_settings(settings);
        settings = nullptr;

        driver = nullptr;
        return;
    }

    LOGI("Sintetizador FluidSynth criado.");

    driver = new_fluid_audio_driver(settings, synth);

    if (driver == nullptr) {
        LOGE("Falha ao criar o driver de áudio Oboe.");
    } else {
        LOGI("Driver de áudio criado.");
    }
}

// -----------------------------------------------------------------------------------------------

SynthManager::~SynthManager() {
    LOGI("Liberando recursos do SynthManager.");

    if (synth != nullptr && soundfontId >= 0) {
        fluid_synth_sfunload(synth, soundfontId, 1);
    }

    if (driver != nullptr) {
        delete_fluid_audio_driver(driver);
        driver = nullptr;
    }

    if (synth != nullptr) {
        delete_fluid_synth(synth);
        synth = nullptr;
    }

    if (settings != nullptr) {
        delete_fluid_settings(settings);
        settings = nullptr;
    }
}

// -----------------------------------------------------------------------------------------------

SynthManager* SynthManager::getInstance() {
    if (instance == nullptr) {
        LOGD("Criando instância do SynthManager.");
        instance = new SynthManager();
    }

    return instance;
}

// -----------------------------------------------------------------------------------------------

void SynthManager::freeInstance() {
    if (instance != nullptr) {
        LOGD("Destruindo instância do SynthManager.");

        delete instance;
        instance = nullptr;
    }
}

// -----------------------------------------------------------------------------------------------

int SynthManager::loadSF(const char *soundfontPath, int program) {
    (void) program;

    LOGI("Solicitação de carregamento de SoundFont.");
    LOGI(
            "Caminho recebido: %s",
            soundfontPath != nullptr ? soundfontPath : "(null)"
    );

    if (synth == nullptr) {
        LOGE("Carregamento cancelado: sintetizador não inicializado.");
        return FLUID_FAILED;
    }

    if (soundfontPath == nullptr || soundfontPath[0] == '\0') {
        LOGE("Carregamento cancelado: caminho da SoundFont vazio ou nulo.");
        return FLUID_FAILED;
    }

    LOGD("Chamando fluid_synth_sfload().");

    /*
     * O terceiro argumento é 0 para não redefinir os presets
     * dos canais MIDI existentes durante o carregamento.
     */
    int sfid = fluid_synth_sfload(
            synth,
            soundfontPath,
            0
    );

    LOGI("Retorno de fluid_synth_sfload(): sfid=%d.", sfid);

    if (sfid == FLUID_FAILED) {
        LOGE(
                "Falha ao carregar a SoundFont. Caminho=%s",
                soundfontPath
        );

        return FLUID_FAILED;
    }

    soundfontId = sfid;

    LOGI(
            "SoundFont carregada com sucesso. sfid=%d, caminho=%s",
            sfid,
            soundfontPath
    );

    return sfid;
}

// -----------------------------------------------------------------------------------------------

void SynthManager::unloadSF(int sfid) {
    if (synth == nullptr) {
        LOGW("Não foi possível descarregar a SoundFont: sintetizador nulo.");
        return;
    }

    if (sfid < 0) {
        LOGW("Não foi possível descarregar a SoundFont: sfid=%d.", sfid);
        return;
    }

    LOGI("Descarregando SoundFont sfid=%d.", sfid);

    int result = fluid_synth_sfunload(
            synth,
            sfid,
            0
    );

    LOGI(
            "Retorno de fluid_synth_sfunload(): %d.",
            result
    );

    if (soundfontId == sfid) {
        soundfontId = -1;
    }
}

// -----------------------------------------------------------------------------------------------

void SynthManager::programChange(int channel, int bank, int program) {
    if (synth == nullptr) {
        LOGE("programChange ignorado: sintetizador nulo.");
        return;
    }

    fluid_synth_bank_select(synth, channel, bank);
    fluid_synth_program_change(synth, channel, program);
}

// -----------------------------------------------------------------------------------------------

void SynthManager::programSelect(
        int channel,
        int sfid,
        int bank,
        int program
) {
    if (synth == nullptr) {
        LOGE("programSelect ignorado: sintetizador nulo.");
        return;
    }

    int result = fluid_synth_program_select(
            synth,
            channel,
            sfid,
            bank,
            program
    );

    if (result != FLUID_OK) {
        LOGE(
                "Falha em fluid_synth_program_select(): channel=%d, sfid=%d, bank=%d, program=%d.",
                channel,
                sfid,
                bank,
                program
        );
    }
}

// -----------------------------------------------------------------------------------------------

fluid_synth_t* SynthManager::getSynth() const {
    return synth;
}

// -----------------------------------------------------------------------------------------------

std::vector<PresetInfoNative> SynthManager::listPresets(int sfid) {
    if (synth == nullptr) {
        LOGE("listPresets: sintetizador nulo.");
        return {};
    }

    LOGD("Listando presets da SoundFont sfid=%d.", sfid);

    return SoundFontParser::listPresets(synth, sfid);
}

// -----------------------------------------------------------------------------------------------

void SynthManager::noteOn(int channel, int note, int velocity) {
    if (synth == nullptr) {
        return;
    }

    fluid_synth_noteon(
            synth,
            channel,
            note,
            velocity
    );
}

// -----------------------------------------------------------------------------------------------

void SynthManager::noteOff(int channel, int note) {
    if (synth == nullptr) {
        return;
    }

    fluid_synth_noteoff(
            synth,
            channel,
            note
    );
}

// -----------------------------------------------------------------------------------------------

void SynthManager::reverb(int level) {
    if (synth == nullptr) {
        return;
    }

    fluid_synth_reverb_on(synth, -1, level > 0);
    fluid_synth_set_reverb_group_level(synth, -1, level / 127.0);
}

// -----------------------------------------------------------------------------------------------

void SynthManager::sendCC(int channel, int controller, int value) {
    if (synth == nullptr) {
        return;
    }

    fluid_synth_cc(
            synth,
            channel,
            controller,
            value
    );
}

// -----------------------------------------------------------------------------------------------

void SynthManager::setLatency(int ms) {
    if (settings == nullptr) {
        LOGE("Não foi possível ajustar a latência: settings nulo.");
        return;
    }

    double bufferSizeInSamples = LATENCY_TO_BUFFER_SIZE(ms);

    fluid_settings_setnum(
            settings,
            "audio.period-size",
            bufferSizeInSamples
    );

    fluid_settings_setint(settings, "audio.periods", 2);
}

// -----------------------------------------------------------------------------------------------

extern "C" {

/**
 * @brief Native implementation of FluidSynthManager.fluidsynthInit().
 */
JNIEXPORT void JNICALL
Java_com_robsonsmartins_androidmidisynth_FluidSynthManager_fluidsynthInit(
        JNIEnv *,
        jobject
) {
    SynthManager::getInstance();
}

/**
 * @brief Native implementation of FluidSynthManager.fluidsynthLoadSF().
 */
JNIEXPORT jint JNICALL
Java_com_robsonsmartins_androidmidisynth_FluidSynthManager_fluidsynthLoadSF(
        JNIEnv *env,
        jobject,
        jstring jSoundfontPath,
        jint program
) {
    if (jSoundfontPath == nullptr) {
        LOGE("JNI: o caminho da SoundFont recebido é nulo.");
        return FLUID_FAILED;
    }

    const char *soundfontPath = env->GetStringUTFChars(
            jSoundfontPath,
            nullptr
    );

    if (soundfontPath == nullptr) {
        LOGE("JNI: falha ao converter o caminho da SoundFont.");
        return FLUID_FAILED;
    }

    LOGI("JNI: caminho recebido: %s", soundfontPath);

    SynthManager *manager = SynthManager::getInstance();

    int sfid = FLUID_FAILED;

    if (manager == nullptr) {
        LOGE("JNI: não foi possível obter SynthManager.");
    } else {
        sfid = manager->loadSF(soundfontPath, program);
    }

    /*
     * Libera a string obtida por GetStringUTFChars()
     * antes de retornar ao Kotlin.
     */
    env->ReleaseStringUTFChars(
            jSoundfontPath,
            soundfontPath
    );

    LOGI("JNI: retorno do carregamento da SoundFont: sfid=%d.", sfid);

    return sfid;
}

// -----------------------------------------------------------------------------------------------

JNIEXPORT void JNICALL
Java_com_robsonsmartins_androidmidisynth_FluidSynthManager_fluidsynthUnloadSF(
        JNIEnv *,
        jobject,
        jint sfid
) {
    SynthManager::getInstance()->unloadSF(sfid);
}

// -----------------------------------------------------------------------------------------------

JNIEXPORT void JNICALL
Java_com_robsonsmartins_androidmidisynth_FluidSynthManager_fluidsynthFree(
        JNIEnv *,
        jobject
) {
    SynthManager::freeInstance();
}

// -----------------------------------------------------------------------------------------------

JNIEXPORT void JNICALL
Java_com_robsonsmartins_androidmidisynth_FluidSynthManager_fluidsynthNoteOn(
        JNIEnv *,
        jobject,
        jint note,
        jint velocity
) {
    SynthManager::getInstance()->noteOn(0, note, velocity);
}

// -----------------------------------------------------------------------------------------------

JNIEXPORT void JNICALL
Java_com_robsonsmartins_androidmidisynth_FluidSynthManager_fluidsynthNoteOff(
        JNIEnv *,
        jobject,
        jint note
) {
    SynthManager::getInstance()->noteOff(0, note);
}

// -----------------------------------------------------------------------------------------------

JNIEXPORT void JNICALL
Java_com_robsonsmartins_androidmidisynth_FluidSynthManager_fluidsynthCC(
        JNIEnv *,
        jobject,
        jint channel,
        jint controller,
        jint value
) {
    SynthManager::getInstance()->sendCC(
            channel,
            controller,
            value
    );
}

// -----------------------------------------------------------------------------------------------

JNIEXPORT void JNICALL
Java_com_robsonsmartins_androidmidisynth_FluidSynthManager_fluidsynthProgramChange(
        JNIEnv *,
        jobject,
        jint channel,
        jint bank,
        jint program
) {
    SynthManager::getInstance()->programChange(
            channel,
            bank,
            program
    );
}

// -----------------------------------------------------------------------------------------------

JNIEXPORT void JNICALL
Java_com_robsonsmartins_androidmidisynth_FluidSynthManager_fluidsynthProgramSelect(
        JNIEnv *,
        jobject,
        jint channel,
        jint sfid,
        jint bank,
        jint program
) {
    SynthManager::getInstance()->programSelect(
            channel,
            sfid,
            bank,
            program
    );
}

// -----------------------------------------------------------------------------------------------

JNIEXPORT void JNICALL
Java_com_robsonsmartins_androidmidisynth_FluidSynthManager_fluidsynthReverb(
        JNIEnv *,
        jobject,
        jint level
) {
    SynthManager::getInstance()->reverb(level);
}

// -----------------------------------------------------------------------------------------------

JNIEXPORT jobject JNICALL
Java_com_robsonsmartins_androidmidisynth_FluidSynthManager_criarPresetTeste(
        JNIEnv *env,
        jobject
) {
    jclass presetClass = env->FindClass(
            "com/robsonsmartins/androidmidisynth/soundfont/PresetInfo"
    );

    if (presetClass == nullptr) {
        return nullptr;
    }

    jmethodID ctor = env->GetMethodID(
            presetClass,
            "<init>",
            "(IILjava/lang/String;)V"
    );

    if (ctor == nullptr) {
        env->DeleteLocalRef(presetClass);
        return nullptr;
    }

    jstring nome = env->NewStringUTF("Accordion Test");

    if (nome == nullptr) {
        env->DeleteLocalRef(presetClass);
        return nullptr;
    }

    jobject preset = env->NewObject(
            presetClass,
            ctor,
            0,
            21,
            nome
    );

    env->DeleteLocalRef(nome);
    env->DeleteLocalRef(presetClass);

    return preset;
}

// -----------------------------------------------------------------------------------------------

JNIEXPORT jobjectArray JNICALL
Java_com_robsonsmartins_androidmidisynth_FluidSynthManager_fluidsynthListPresets(
        JNIEnv *env,
        jobject,
        jint sfid
) {
    LOGD("JNI: entrou em fluidsynthListPresets(), sfid=%d.", sfid);

    auto presets = SoundFontParser::listPresets(
            SynthManager::getInstance()->getSynth(),
            sfid
    );

    LOGD("JNI: quantidade de presets encontrada: %zu.", presets.size());

    jclass presetClass = env->FindClass(
            "com/robsonsmartins/androidmidisynth/soundfont/PresetInfo"
    );

    if (presetClass == nullptr) {
        LOGE("JNI: não foi possível localizar a classe PresetInfo.");
        return nullptr;
    }

    jmethodID ctor = env->GetMethodID(
            presetClass,
            "<init>",
            "(IILjava/lang/String;)V"
    );

    if (ctor == nullptr) {
        LOGE("JNI: não foi possível localizar o construtor de PresetInfo.");
        env->DeleteLocalRef(presetClass);
        return nullptr;
    }

    jobjectArray array = env->NewObjectArray(
            static_cast<jsize>(presets.size()),
            presetClass,
            nullptr
    );

    if (array == nullptr) {
        LOGE("JNI: não foi possível criar o array de presets.");
        env->DeleteLocalRef(presetClass);
        return nullptr;
    }

    for (jsize i = 0; i < static_cast<jsize>(presets.size()); i++) {
        const PresetInfoNative &preset = presets[i];

        jstring nome = env->NewStringUTF(preset.name.c_str());

        if (nome == nullptr) {
            LOGE("JNI: falha ao criar o nome do preset na posição %d.", i);
            break;
        }

        jobject presetObject = env->NewObject(
                presetClass,
                ctor,
                preset.bank,
                preset.program,
                nome
        );

        env->DeleteLocalRef(nome);

        if (presetObject == nullptr) {
            LOGE("JNI: falha ao criar o objeto preset na posição %d.", i);
            break;
        }

        env->SetObjectArrayElement(
                array,
                i,
                presetObject
        );

        env->DeleteLocalRef(presetObject);

        if (env->ExceptionCheck()) {
            LOGE("JNI: exceção ao preencher o array de presets.");
            break;
        }
    }

    env->DeleteLocalRef(presetClass);

    return array;
}

} // extern "C"
