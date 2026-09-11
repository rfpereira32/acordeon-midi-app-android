# Acordeon MIDI

Projeto pessoal de aplicativo Android nativo desenvolvido em Kotlin para uso com acordeões MIDI.

O aplicativo recebe mensagens MIDI do instrumento através de uma conexão Bluetooth Low Energy (BLE), processa os eventos de nota em nível nativo (C++) e utiliza o motor **FluidSynth** para síntese sonora em tempo real.

## Funcionalidades Atuais

- **Conexão BLE MIDI:** Escaneamento automático e conexão com dispositivos MIDI Bluetooth.
- **Síntese Sonora:** Utiliza a biblioteca FluidSynth com driver Oboe para áudio de baixa latência.
- **Gerenciamento de SoundFonts:** Importação e utilização de arquivos de instrumentos no formato `.sf2`.
- **Mixer MIDI de 16 canais:** Controle individual de volume, mute e seleção de instrumentos para os canais MIDI.
- **Gestão de Presets:** Salvamento e recuperação de configurações de timbres e volumes da sessão.
- **Telemetria:** Exibição do nível de bateria do instrumento conectado (via protocolo SysEx).
- **Atualização OTA:** Comando para iniciar a atualização de firmware do hardware via Wi-Fi Access Point.

## Créditos e Origem

Este projeto é uma versão especializada e estendida do código original [Android MIDI Synth](https://github.com/robsonsmartins/android-midi-synth) de Robson Martins, adaptado para as necessidades específicas de um músico acordeonista.

## Licença

Este software é distribuído sob a licença MIT. Consulte o arquivo `LICENSE` para mais detalhes.
