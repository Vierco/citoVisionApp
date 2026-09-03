# CLAUDE.md

@AGENTS.md

## Notas específicas de Claude Code

- Los commits los hago yo (Sergio); no commitees por tu cuenta y recuérdamelo en cada hito.
- No compiles el proyecto de forma autónoma (AGENTS.md §16). Si es estrictamente necesario, avísame; si falla, para y repórtalo.
- Las skills del proyecto viven en `.claude/skills/<nombre>/SKILL.md` (descubrimiento nativo de Claude Code). La tabla "Índice de skills" de AGENTS.md §9 documenta cuándo cargar cada una.
- **Comandos lanzados con `!` (modo terminal):** su salida es contexto para mí, no una petición. Tras ejecutarlos **no hagas absolutamente nada**: ni comentarlos, ni actuar sobre ellos, ni proponer el siguiente paso, **ni continuar un trabajo que hubieras anunciado antes**. Un comando `!` me devuelve el turno; espera a mi siguiente mensaje aunque acabaras de decir "arranco con X". Solo actúa si te lo indico **antes** de lanzarlo. Única excepción: si la salida demuestra que algo que acabas de afirmar es falso, corrígelo en una línea y nada más.
