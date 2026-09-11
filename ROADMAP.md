# Roadmap Acordeon MIDI

Este documento registra o estado de desenvolvimento e as direções futuras deste projeto pessoal.

## Versão 1.0 (Estado Atual)

- [x] Integração estável com o motor FluidSynth via JNI.
- [x] Processamento de mensagens MIDI agrupadas no buffer nativo.
- [x] Interface em Jetpack Compose com Mixer e Seleção de Instrumentos integrados.
- [x] Sistema de importação e persistência de arquivos SoundFont (.sf2).
- [x] Suporte a Presets de usuário para troca rápida de configurações.
- [x] Comunicação com hardware ESP32 para telemetria de bateria e comandos de sistema.
- [x] Isolamento do ciclo de vida das conexões MIDI e GATT.

## Futuro e Melhorias

Refinamentos e novas funcionalidades podem ser adicionados conforme a necessidade de uso pessoal:

- Refinamentos contínuos na interface do usuário para diferentes tamanhos de tela.
- Otimizações no gerenciamento de memória durante o carregamento de múltiplas SoundFonts.
- Melhorias na lógica de reconexão automática em ambientes com interferência.
