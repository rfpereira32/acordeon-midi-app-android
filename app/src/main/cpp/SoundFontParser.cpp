//
// Created by rodri on 11/07/2026.
//
/*
 * Copyright (c) 2024 Robson Martins
 */

#include "SoundFontParser.h"

std::vector<PresetInfoNative> SoundFontParser::listPresets(
        fluid_synth_t* synth,
        int sfid)
{
    std::vector<PresetInfoNative> presets;

    if (synth == nullptr)
        return presets;

    fluid_sfont_t* sfont =
            fluid_synth_get_sfont_by_id(
                    synth,
                    sfid
            );

    if (sfont == nullptr)
        return presets;

    fluid_sfont_iteration_start(
            sfont
    );

    fluid_preset_t* preset;

    while ((preset =
                    fluid_sfont_iteration_next(
                            sfont
                    )) != nullptr)
    {
        PresetInfoNative info;

        const char* nome =
                fluid_preset_get_name(
                        preset
                );

        info.name =
                (nome != nullptr)
                ? nome
                : "";

        info.bank =
                fluid_preset_get_banknum(
                        preset
                );

        info.program =
                fluid_preset_get_num(
                        preset
                );

        presets.push_back(
                info
        );
    }

    return presets;
}