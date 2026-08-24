# Acordeon MIDI — Instruções para agentes

## Fonte de verdade

- O código atualmente presente no projeto é a fonte de verdade.
- Arquivos de documentação existentes, incluindo ARCHITECTURE.md, DECISÕES.md,
  ROADMAP.md e README.md, podem estar desatualizados.
- Não presuma que a documentação representa o estado atual do código.
- Antes de propor alterações, analise os arquivos atualmente relacionados à tarefa.

## Segurança das alterações

- Não altere arquivos sem autorização explícita para a tarefa.
- Não faça commits ou push automaticamente.
- Não use git reset, git restore ou descarte alterações sem autorização.
- Preserve alterações existentes feitas pelo usuário.
- Não modifique arquivos fora do escopo da tarefa sem explicar o motivo.

## Antes de alterar código

- Primeiro identifique onde o comportamento relevante está implementado.
- Considere as relações entre as classes e módulos existentes antes de propor
  uma nova implementação.
- Prefira alterações pequenas e compatíveis com o código existente.
- Não substitua uma implementação existente por outra abordagem apenas por
  preferência arquitetural.

## Compatibilidade

- Preserve comportamentos e funcionalidades existentes que não fazem parte
  da tarefa solicitada.
- Não altere APIs, protocolos, formatos de dados, canais MIDI, configurações
  ou dependências existentes sem autorização.
- Não adicione bibliotecas ou altere versões de dependências sem explicar
  a necessidade.

## Testes

- Após alterações de código, compile ou execute os testes relevantes quando
  possível.
- Informe claramente o resultado dos testes.
- Se algo não puder ser testado, informe o que ficou sem validação.

## Git

- Verifique o estado do Git antes de alterações importantes.
- Nunca descarte alterações do usuário para deixar o repositório limpo.
- Não faça commit automaticamente.