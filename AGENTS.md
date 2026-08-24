# AGENTS.md — Proyecto Kotlin Multiplatform (KMP)

## Nombre del proyecto

### citoVision


## 0. Regla principal

Antes de tocar código, el agente debe:

1. Leer este fichero completo.
2. Leer documentación derivada o relacionada según la tarea:
   - `DESIGN.md`: cambios visuales, UI, theming, componentes, UX.
   - `RULES.md`: funcionalidad nueva o modificación de comportamiento.
   - `ARCHITECTURE.md`: decisiones arquitectónicas aprobadas o decisiones que afecten directa o indirectamente a la arquitectura del proyecto.
   - `SECURITY_MOBILE`: decisiones que añadan, afecten o modifiquen parte del proyecto que puedan estar relacionadas con la seguridad de este.
   - `TESTING.md`: Decisiones acerca de implementación, modificación, adición o relacionados al testing de las partes del proyecto.
   - `docs/adr/*.md`: propuestas técnicas aprobadas e implementadads que han de ser documentadas.
   - `docs/rfc/*.md`: propuestas y documentación de decisiones de modificaciones técnicas, estructurales, de arquitectura, etc…
   - `.claude/skills/<nombre_del_skill>/SKILL.md`: procedimientos operativos específicos.
3. No inventar reglas de negocio.
4. No modificar specs, ADRs, RFCs, Ni ficheros de reglas (DESING, RULES, AGENTS, etc…) sin confirmación humana.
5. Si código y documentación discrepan, detenerse y reportar el conflicto.
6. Si las instrucciones de AGENTS.md discrepan con la de otros ficheros referifdos dentro del Agents.md, detenerse y reporatr el conflicto.

## 1. Jerarquía de instrucciones

Prioridad de mayor a menor:

1. Instrucción explícita del usuario en la tarea actual.
2. `AGENTS.md` local más cercano al archivo modificado.
3. `AGENTS.md` raíz.
4. Specs aprobadas.
5. ADRs aprobados.
6. `DESIGN.md`.
7. Convenciones existentes en el código.
8. Buenas prácticas generales.

Notas:

- Un RFC no aprobado no autoriza implementación definitiva.
- Una spec draft solo permite propuesta, prototipo o aclaración.
- Si una instrucción contradice seguridad o privacidad, pedir confirmación y explicar el riesgo.

## 2. Arquitectura del proyecto

### Clean architecture with MVVM

**Obligatorio:** Para cualquier cambio relacionado o que afecte directa o indirectamente a arquitectura, estructura u organización, leer `ARCHITECTURE.md` antes de implementar.

### Reglas de arquitectura por feature

Cada nueva feature debe mantener, siempre que tenga entidad suficiente, una estructura propia basada en:

- `presentation`

- `application`

- `domain`

- `infrastructure`

- `composition`

Debe evitarse crear carpetas globales enormes donde se mezclen responsabilidades de distintas features.

La lógica común de la feature deberá implementarse primero en `commonMain`.

Solo se usarán carpetas específicas de plataforma (`androidMain`, `iosMain`, `desktopMain`) cuando exista una dependencia real de plataforma, por ejemplo:

- Android SDK

- APIs Apple

- APIs JVM/Desktop

- drivers específicos

- almacenamiento seguro específico

- motores de red específicos, etc..

Las dependencias específicas de plataforma deberán entrar mediante:

- `expect/actual`

- ports definidos en `application`

- dependency injection desde `composition/di`
```

## 3. Reglas KMP

- `commonMain`: lógica compartida, modelos, use cases, contratos, validaciones, serialización común.
- `commonTest`: tests de dominio, use cases, mappers comunes y lógica compartida.
- `androidMain`: implementaciones Android, storage Android, APIs Android, lifecycle si aplica.
- `iosMain`: implementaciones iOS, Foundation, Keychain, wrappers nativos.
- Usar `expect/actual` solo si hay una diferencia real de plataforma.
- No usar `expect/actual` para ocultar mala arquitectura.
- Antes de añadir dependencia: verificar targets KMP, mantenimiento, tamaño binario e impacto en iOS/Android.
- Preferir `kotlinx.coroutines`; evitar scopes globales; exponer `Flow`/`StateFlow` inmutables.

## 5. Presentation / MVVM / Compose

- MVVM vive en `presentation`.
- Composables preferentemente stateless.
- Estado en `UiState`; acciones como `UiEvent` o callbacks.
- ViewModel procesa eventos, llama a use cases y traduce errores a estado UI.
- No llamadas de red, DB ni lógica de negocio dentro de composables.
- No pasar ViewModel a componentes reutilizables.
- No hardcodear colores, spacing, radius, tipografía o motion si existen tokens en `DESIGN.md`.
- Añadir previews para componentes relevantes.
- Navegación fuera de casos de uso.
- No pasar objetos complejos por rutas si basta con IDs.
- Consta skill relacionado llamado mvvm-compose-kmp

## 6. DESIGN.md

**Obligatorio:** Para cualquier cambio visual, leer `DESIGN.md` antes de implementar.

Debe contener:

- Identidad visual.
- Design keywords.
- Principios de UI.
- Tokens: color, typography, spacing, radius, elevation, motion, iconography.
- Componentes base.
- Estados: loading, empty, error, success, offline.
- Reglas mobile: safe areas, tamaños táctiles mínimos, orientación, responsive/adaptive layout, dark mode, accesibilidad.
- Ejemplos correctos e incorrectos.

Si falta un token, proponerlo; no inventar valores permanentes.

## 7. Spec-Driven Development

Toda feature nueva debe partir de una spec aprobada o crear primero una spec.

Ubicación:

```text
docs/specs/<feature-id>.md
```

La IA puede proponer mejoras, inconsistencias y casos borde, pero no modifica specs aprobadas sin confirmación humana.

Plantilla mínima:

```markdown
# SPEC-0000 - <Nombre>

## Estado
Draft | Approved | Deprecated

## Contexto
## Objetivo
## No objetivos
## Usuarios / actores
## Requisitos funcionales
## Requisitos no funcionales
## Reglas de negocio
## Estados de UI
## Contratos de datos
## Errores
## Casos borde
## Telemetría / analytics
## Seguridad y privacidad
## Criterios de aceptación
## Tests requeridos
## Dependencias
## Notas abiertas
```

## 8. ADRs y RFCs

### ADR

Registra una decisión ya tomada. Usar para arquitectura, DI, navegación, DB, seguridad, offline, librerías críticas, backend o cloud.
La IA no modificará en ningún caso este tipo de ficheros excepto por petción expresa del desarrollador.


```text
docs/adr/0001-short-title.md
```

### RFC

Propone una decisión antes de implementarla. Usar cuando hay alternativas, impacto transversal, coste de migración o riesgos.
La IA no modificará en ningún caso este tipo de ficheros excepto por petción expresa del desarrollador.

```text
docs/rfc/0001-short-title.md
```


## 9. Índice de skills

> **Instrucción para la IA:** los Skills listados a continuación **no se cargan por defecto**. Cada Skill debe cargarse únicamente cuando la tarea en curso lo requiera de forma directa, según la columna "Cuándo cargarlo". No cargues un Skill solo porque el nombre de una librería aparece mencionado de pasada en la conversación: carga `room-multiplatform` solo si la tarea implica trabajar con persistencia local Room, no por el mero hecho de que la palabra "Room" aparezca en el contexto. Si una tarea requiere varios Skills a la vez (por ejemplo, crear un Repository implica `repository-pattern` + `result-pattern` + posiblemente `room-multiplatform`/`ktor-client`), carga todos los que apliquen, pero ninguno que no aporte valor directo a esa tarea concreta. Ante la duda, prioriza no cargar un Skill innecesario antes que cargar varios sin necesidad real.

Cada skill vive en `.claude/skills/<nombre_del_skill>/SKILL.md` y Claude Code las descubre de forma nativa. **Excepción:** `android-official` es una colección externa de Google (Apache 2.0), **no incluida en el repo**; se instala aparte con Android CLI.

| Skill | Ruta | Archivo | Cuándo cargarlo |
|---|---|---|---|
| Skill Creator | `.claude/skills/skill-creator` | `SKILL.md` | Crear nuevos Skills a partir de funciones, tareas o librerías del proyecto que aún no tengan uno. |
| Clean Architecture KMP | `.claude/skills/clean-architecture-kmp` | `SKILL.md` | Diseñar la arquitectura de una nueva feature, decidir en qué capa (Presentation/Application/Domain/Infrastructure/Composition) vive una clase, o revisar si una PR respeta los límites entre capas. |
| MVVM Compose KMP | `.claude/skills/mvvm-compose-kmp` | `SKILL.md` | Crear o revisar un ViewModel, su `UiState`/`UiEvent`, o conectar un Composable con la capa Application. |
| Dependency Injection Koin | `.claude/skills/dependency-injection-koin` | `SKILL.md` | Configurar Koin, registrar un nuevo Repository/UseCase/ViewModel en un módulo, o resolver una dependencia específica de plataforma (Context, Keychain, drivers). |
| Result Pattern | `.claude/skills/result-pattern` | `SKILL.md` | Definir la firma de un método que puede fallar (Repository, UseCase, DataSource), tipar un error de Domain/Application, o decidir si algo debe lanzar excepción o devolver `Result`. |
| Repository Pattern | `.claude/skills/repository-pattern` | `SKILL.md` | Crear o revisar un Repository, decidir cómo combina un `RemoteDataSource` y un `LocalDataSource`, o decidir si una operación debe ser `Flow` o `suspend`. |
| Ktlint KMP code style | `.claude/skills/ktlint-kmp-code-style` | `SKILL.md` | Reglas que se han de seguir para respetar el KtlintCheck al escribir código para que no se generen cientos de puntos a corregir en cada PullRequest. |
| Room Multiplatform | `.claude/skills/room-multiplatform` | `SKILL.md` | Definir o modificar el esquema de base de datos local (entidades, DAOs), configurar Room KMP, o implementar el `LocalDataSource` de un Repository. |
| Ktor Client | `.claude/skills/ktor-client` | `SKILL.md` | Configurar el cliente HTTP, implementar un `RemoteDataSource`, decidir plugins (logging, timeout, auth) o mapear una excepción de red a un error de dominio. |
| Testing KMP | `.claude/skills/testing-kmp` | `SKILL.md` | Escribir o revisar los tests de cualquier capa, decidir qué mockear con Mokkery, o verificar el grafo de Koin antes de mergear. |
| Navigation Compose KMP | `.claude/skills/navigation-compose-kmp` | `SKILL.md` | Definir cómo navegar entre pantallas de una nueva feature, elegir entre Navigation 3 y Navigation 2 Compose Multiplatform, o implementar el paso de parámetros entre pantallas. |
| Theming Compose KMP | `.claude/skills/theming-compose-kmp` | `SKILL.md` | Crear el sistema de tema por primera vez, crear un nuevo componente reutilizable de UI (botón, card, estado de pantalla), o decidir qué color/spacing/radio usar en una pantalla. |
| Compose Resources KMP | `.claude/skills/compose-resources-kmp` | `SKILL.md` | Añadir una imagen, icono o fuente compartida entre plataformas, o añadir/traducir un string de interfaz. |
| DataStore Multiplatform | `.claude/skills/datastore-multiplatform` | `SKILL.md` | Guardar preferencias de usuario no sensibles (tema, idioma, onboarding, flags), o implementar el Repository de configuración de la app. |
| Napier Logging KMP | `.claude/skills/napier-logging-kmp` | `SKILL.md` | Inicializar el logging del proyecto, decidir el nivel de log adecuado para un mensaje, o loguear una excepción capturada en un UseCase/Repository/ViewModel. |
| Gradle Conventions KMP | `.claude/skills/gradle-conventions-kmp` | `SKILL.md` | Configurar o auditar la estructura de módulos del proyecto, añadir una dependencia/plugin nuevo al catálogo, o decidir si introducir convention plugins (`build-logic`). |
| Build Config KMP | `.claude/skills/build-config-kmp` | `SKILL.md` | Definir la URL base de la API por entorno, gestionar claves de API u otros secretos de compilación, o definir feature flags fijados en tiempo de compilación. |
| Android Official | _Colección externa (Google, Apache 2.0)_ — se instala aparte con Android CLI; **no incluida en el repo** | — | Consultar guías o convenciones oficiales específicas de Android no cubiertas por los Skills multiplataforma anteriores. |

---

### Documentos de referencia (no son Skills, pero los Skills los citan)

| Documento | Ruta | Uso |
|---|---|---|
| Architecture | raíz del proyecto / `ARCHITECTURE.md` | Estructura de carpetas y reglas de dependencias entre capas del proyecto. |
| Design | raíz del proyecto / `DESIGN.md` | Sistema de diseño (colores, tipografía, spacing, componentes) de citoVision. |
| Testing | raíz del proyecto / `TESTING.md` | Framework de testing, pirámide de testing y métricas de cobertura del proyecto. |


Formato de skill habitual, no obstante, dependiendo del autor, el formato puede variar:

```markdown
# Skill: <nombre>

## Objetivo
## Modelo conceptual
## Cuándo usarlo
## Cuándo NO usarlo
## Dependencias
## Entradas necesarias
## Criterios arquitectónicos
## Reglas
## Decisiones automáticas
## Proceso recomendado
## Checklist
## Definition of Done
## Riesgos
## Anti-patrones
## Comandos útiles
## Salida esperada
## Ejemplos
## Referencias
```

## 10. Testing

**Obligatorio:** Antes de realizar cualquier nuevos test o modificaciones de exitentes, leer siempre el fichero "TESTING.md"

Regla: un cambio no está completo si no incluye tests proporcionales al riesgo.

Prioridad:

- `commonTest`: dominio, casos de uso, validadores, mappers comunes.
- Integración: repositorios, APIs fake, persistencia, sincronización.
- UI: flujos críticos y estados visuales.
- Manual: solo cuando no haya automatización razonable.

Mapa KMP:

```text
domain models      → commonTest
use cases          → commonTest
mappers comunes    → commonTest
android storage    → androidUnitTest / androidInstrumentedTest
ios actual impl    → iosTest
Compose UI         → Compose UI tests / screenshot tests si aplica
```

Compose testing:

- Usar semantics/test tags cuando sea necesario.
- Testear comportamiento, no implementación interna.
- Cubrir loading, empty, error, success.

Corrutinas/Flow:

- Usar dispatchers controlados.
- Evitar `delay` real.
- Testear emisiones y cancelación.

Antes de cerrar:

- Tests relevantes añadidos/actualizados.
- Tests existentes pasan.
- No se han eliminado tests sin justificar.
- Casos borde de la spec cubiertos.

## 11. Seguridad mobile

**Obligatorio:** Cuando se vayan a aplicar cambios, modificaciones, adiciones... Que incidan o puedan incidir sobre la seguridad del proyecto, leer el fichero SECURITY_MOBILE.md

Aparte, tener siempre presente:

- No hardcodear secrets, API keys privadas, tokens ni credenciales.
- No guardar tokens sensibles en texto plano.
- No loggear PII, tokens, authorization headers ni payloads sensibles.
- HTTPS obligatorio en producción.
- No desactivar la validación TLS.
- Certificate pinning únicamente con una estrategia de rotación definida.
- Todas las validaciones realizadas en cliente deberán repetirse en el servidor. El cliente nunca constituye una frontera de seguridad.
- Aplicar siempre el principio de mínimo privilegio.
- Solicitar únicamente los permisos Android/iOS estrictamente necesarios.
- Revisar OWASP Mobile Top 10 y MASVS cuando los cambios afecten a la seguridad siempre bajo petición del desarrollador.


## 12. Calidad Kotlin

- Kotlin idiomático.
- Preferir inmutabilidad.
- Evitar `!!` salvo justificación.
- Nombres claros y tipos explícitos cuando mejore legibilidad.
- Nullability controlada.
- No usar excepciones para flujo normal si el proyecto usa `Result`/`Either`.
- Funciones pequeñas y cohesionadas.
- Mappers explícitos entre DTO remoto, entidad local, dominio y UI.
- No filtrar DTOs hacia dominio salvo decisión aprobada.

## 13. Persistencia y offline

Antes de tocar persistencia:

1. En caso de base de datos, leer skill `.claude/skills/room-multiplatform`
2. En caso de almacenamiento local de datos en el dispositivo, leer skill `.claude/skills/datastore-multiplatform`
2. Clasificar dato: cache, estado local, crítico o sensible.
3. Definir expiración, sincronización, conflictos, migraciones y cifrado.
4. Añadir tests de migración si cambia esquema.
5. No borrar datos de usuario sin migración o confirmación.

## 14. Networking y APIs

- Contratos externos documentados.
- DTOs separados de dominio.
- Manejar timeout, offline, payload inválido y rate limiting.
- Reintentos solo para operaciones idempotentes o con estrategia explícita.
- No duplicar parsing/error handling por feature si existe capa común.

## 15. Accesibilidad

Todo cambio UI debe revisar:

- Labels accesibles.
- Tamaño táctil mínimo.
- Contraste.
- Escalado de fuente.
- Estados focus/selected/disabled.
- TalkBack/VoiceOver.
- No depender solo del color.
- Orden lógico de lectura.

## 16. Performance mobile

Revisar según aplique:

- Recomposición innecesaria.
- Carga de imágenes.
- Memoria.
- Startup.
- Main thread.
- Tamaño binario.
- Red y batería.
- Persistencia bloqueante.
- Flows calientes innecesarios.

Nunca compiles tú el proyecto, si es estrictamente necesario dímelo y si falla, paras y me lo dices.

No añadir polling si se puede resolver con eventos, push o sincronización controlada.

## 17. CI/CD y comandos

Actualizar según el proyecto real:

```bash
./gradlew clean
./gradlew build
./gradlew test
./gradlew check
./gradlew lint
./gradlew :shared:allTests
./gradlew :androidApp:assembleDebug
```

Compose Multiplatform, si aplica:

```bash
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:iosSimulatorArm64Test
```

Backend, si aplica:

```bash
./gradlew :backend:test
./gradlew :backend:build
```

Regla: no afirmar que compila o que pasan tests si no se han ejecutado. Indicar comandos y resultado.

## 18. Flujo de trabajo del agente

Antes:

- Objetivo exacto.
- Archivos afectados.
- Spec relacionada.
- Riesgos.
- Tests necesarios.
- Documentación afectada.

Durante:

- Cambios pequeños.
- Coherencia con estructura existente.
- No mezclar refactor no pedido con feature.
- No cambiar APIs públicas sin justificar.
- No añadir dependencias sin explicar.

Después:

- Resumir cambios.
- Indicar tests ejecutados.
- Documentar riesgos restantes.
- Indicar documentación actualizada.

## 19. Git y PRs

Commits recomendados:

```text
feat(player): add playback progress use case
fix(auth): refresh token expiration handling
test(profile): cover invalid birthdate
docs(adr): record database decision
refactor(shared): simplify result mapping
```

Checklist PR:

- [ ] Spec leída o creada.
- [ ] `DESIGN.md` leído si afecta UI.
- [ ] ADR/RFC revisado si afecta arquitectura.
- [ ] Tests añadidos/actualizados.
- [ ] Seguridad revisada si hay datos, auth, red o storage.
- [ ] Accesibilidad revisada si hay UI.
- [ ] CI pasa o se documenta por qué no.
- [ ] Sin secrets en código/logs.
- [ ] Sin cambios fuera de alcance.

## 20. IA generativa dentro del producto

Si la app usa IA:

- Separar UI prompt, system prompt, tools y datos de usuario.
- Prompts versionados si afectan comportamiento.
- Límites de coste, timeout y fallback.
- Manejo de errores del proveedor.
- Logs seguros sin datos sensibles.
- RAG: documentar chunking, embeddings, vector store, actualización y evaluación.
- Agentes: declarar tools permitidas, permisos, confirmación para acciones destructivas, logs auditables y protección contra prompt injection.

## 21. AGENTS.md anidados

Permitidos:

```text
shared/AGENTS.md
androidApp/AGENTS.md
iosApp/AGENTS.md
backend/AGENTS.md
```

Deben ser breves, locales y no contradecir seguridad global.

## 22. Qué no debe hacer el agente

- No inventar requisitos.
- No modificar specs aprobadas sin permiso.
- No introducir dependencias por comodidad.
- No mover archivos masivamente sin motivo.
- No romper APIs públicas sin migración.
- No ocultar errores de compilación.
- No borrar tests para hacer pasar CI.
- No usar secretos reales en ejemplos.
- No enviar datos privados a servicios externos.
- No implementar lógica de negocio en UI.
- No usar Android/iOS APIs desde `commonMain`.
- No tocar signing, provisioning, keystores o certificados sin instrucción explícita.



## 23. Fuentes y estándares de referencia

- OpenAI Codex — AGENTS.md: https://developers.openai.com/codex/guides/agents-md
- AGENTS.md open format: https://agents.md/
- Claude Code / CLAUDE.md: https://docs.anthropic.com/
- Google Labs DESIGN.md: https://github.com/google-labs-code/design.md
- Kotlin Multiplatform: https://kotlinlang.org/docs/multiplatform/
- Android app architecture: https://developer.android.com/topic/architecture
- Jetpack Compose testing: https://developer.android.com/develop/ui/compose/testing
- Compose Multiplatform testing: https://kotlinlang.org/docs/multiplatform/compose-test.html


## 24. Estado

```yaml
template_version: "0.1.0"
status: "draft"
intended_for: [Kotlin Multiplatform, Android, iOS, Compose, AI-assisted development]
owner: "<TEAM_OR_PERSON>"
last_reviewed: "<YYYY-MM-DD>"
```
