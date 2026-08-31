# ADR-0005 - Desbloquear el enlazado de iOS: Koin 4.1.0 y alineación del toolchain (Kotlin 2.2.20)

## Estado

Aceptada — 2026-08-25

## Contexto

Durante el bring-up de iOS (ADR-0004), el framework de iOS **compilaba pero no enlazaba**
(`./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`, y el build de la app en Xcode 26):

```
Undefined symbols for architecture arm64:
  "_kfun:androidx.lifecycle.viewmodel.compose#androidx_lifecycle_viewmodel_compose_
   LocalViewModelStoreOwner$stableprop_getter$artificial(){}kotlin.Int", referenced from:
      AppNavHost$$inlined$cache$1...invoke$1 / $invoke$2 / $invoke$4
      AnalysisScreen, HistoryScreen, PatientsScreen
```

### Causa raíz (confirmada con evidencia)

El culpable es **Koin 4.0.0**, no el compilador ni la versión de lifecycle:

- `koinViewModel()` (de `koin-compose-viewmodel`) es una **función `inline`**. Al compilar, su cuerpo
  —y con él la referencia al símbolo de estabilidad de `LocalViewModelStoreOwner`— **se incrusta en
  nuestro código**. De ahí el marcador `$$inlined$` en el error.
- **Koin 4.0.0 se compiló contra `lifecycle-viewmodel-compose` 2.8.0**; el proyecto enlaza **2.10.0**,
  donde ese símbolo sintético ya no existe → en Kotlin/Native, que resuelve los símbolos en **enlazado
  duro**, falta la definición. En la JVM (Android/Desktop) se resuelve dinámicamente y se tolera: por eso
  **solo rompía iOS**, y el fallo estuvo latente hasta que iOS se enlazó por primera vez.

Evidencia que lo confirma (no es una deducción):

1. Los **cuatro** ficheros del error (`AppNavHost`, `AnalysisScreen`, `HistoryScreen`, `PatientsScreen`)
   son **exactamente** los cuatro que llaman a `koinViewModel()`.
2. `AppNavHost` contiene **tres** llamadas (Splash, Login, Settings) y el error lista **tres** entradas
   `$$inlined$cache$1$invoke$…`.
3. POMs de Maven Central: `koin-compose-viewmodel:4.0.0` → `lifecycle-viewmodel-compose:2.8.0`;
   `koin-compose-viewmodel:4.1.0` → `2.9.0-beta01`. Koin 4.1 anuncia explícitamente "Compose 1.8 /
   Lifecycle 2.9 support".

### Hipótesis descartadas por el camino (quedan documentadas)

- **Desajuste de versión de `lifecycle`.** Subir `jetbrains-lifecycle` 2.9.6 → 2.10.0 **no** resolvió el
  error. (Aun así se conserva: 2.10.0 es la versión que Compose Multiplatform 1.10.3 empareja.)
- **Compilador Kotlin demasiado antiguo.** Se atribuyó el símbolo a un desajuste entre el compose-compiler
  2.2.10 y el 2.2.20 con el que se publicó CMP 1.10.3. Subir Kotlin **tampoco** resolvió el error. (También
  se conserva: CMP 1.10.3 **exige Kotlin ≥ 2.2.20 para targets native**, así que el proyecto estaba por
  debajo del mínimo documentado; el `kotlin = "2.1.0"` del catálogo era un marcador y el compilador
  efectivo era 2.2.10, arrastrado por AGP 9.2.1.)

### Problema independiente: framework estático vs. Xcode 26

Con framework **estático**, Xcode 26 rechaza el enlazado de la app contra frameworks privados que arrastra
Compose (`cannot link directly with 'SwiftUICore'... not an allowed client`,
`framework 'UIUtilities' not found`). En **dinámico** ese enlazado lo resuelve el propio framework y esos
errores desaparecen.

## Decisión

Desbloquear iOS con el cambio mínimo, y de paso alinear el toolchain con lo que CMP 1.10.3 documenta:

| Pin (`gradle/libs.versions.toml`) | Antes | Después | Papel |
|---|---|---|---|
| `koin` | `4.0.0` | **`4.1.0`** | **el arreglo real** del símbolo no definido |
| `kotlin` | `2.1.0` (efectivo 2.2.10) | **`2.2.20`** | mínimo exigido por CMP 1.10.3 en native |
| `ksp` | `2.2.10-2.0.2` | **`2.2.20-2.0.4`** | debe casar con la versión de Kotlin |
| `mokkery` | `2.9.0` | **`2.10.2`** | plugin de compilador; 2.9.0 solo llega a Kotlin 2.2.10 |
| `jetbrains-lifecycle` | `2.9.6` | **`2.10.0`** | versión que empareja con CMP 1.10.3 |
| framework iOS `isStatic` | `true` | **`false`** | evita el choque con Xcode 26 (ver arriba) |

Subir `kotlin` arrastra por `version.ref` los plugins `kotlin.compose` (compose-compiler),
`kotlin.android` y `kotlin.serialization`. Se actualizan también los comentarios del catálogo que decían
"compilador efectivo 2.2.10".

**Nota de ejecución (runtime):** una vez enlazada, la app abortaba al arrancar con
`IllegalStateException` desde `PlistSanityCheck`. Compose Multiplatform **exige** la clave
`CADisableMinimumFrameDurationOnPhone = true` en `Info.plist` (sin ella iOS limita la app a 60 FPS en
pantallas ProMotion). Se añadió la clave, en lugar de desactivar la comprobación con
`enforceStrictPlistSanityCheck = false`.

## Alternativas consideradas

1. **Bajar `lifecycle` a 2.8.0 para casar con Koin 4.0.0.** Descartada: sería una regresión de la base de
   UI y contradice el emparejamiento documentado de CMP 1.10.3 (lifecycle 2.10.0). Subir Koin es la
   dirección correcta.
2. **Mantener el framework estático.** Descartada: choca con el *allowed-client* de `SwiftUICore` en
   Xcode 26.
3. **Bajar CMP + lifecycle + navigation a un set compilado con Kotlin 2.2.10.** Descartada: regresión
   mayor frente a un bump de patch de Kotlin.
4. **Desactivar `enforceStrictPlistSanityCheck`.** Descartada: ocultaría un problema real de rendimiento
   (60 FPS en ProMotion) en vez de corregirlo.

## Consecuencias

**Positivas**

- **Desbloquea iOS**: el framework enlaza y la app arranca en el simulador (verificado).
- El compilador **sube** (2.2.10 → 2.2.20): sigue leyendo todos los klibs actuales (FileKit 0.11.0,
  Coil 3.3.0, Firebase 2.1.0). Es la **dirección segura** de compatibilidad de klibs; no toca los límites
  documentados de FileKit 0.12+/Coil 3.4+ (que son el caso contrario).
- El proyecto queda alineado con los requisitos publicados de CMP 1.10.3 para native.
- Koin 4.1 mantiene toda la API usada (`KoinContext`, `koinInject`, `koinViewModel`, `startKoin`,
  `factoryOf`/`singleOf`/`viewModelOf`, `bind`/`binds`, `androidContext`): sin cambios de código.

**Negativas / deuda**

- **Cambio transversal**: obliga a reverificar Android y Desktop, no solo iOS.
- **Lección aprendida (aplicable al futuro):** una función `inline` de una librería incrusta en nuestro
  binario referencias a símbolos de *sus* dependencias. Un desajuste así es **invisible en JVM** y solo
  aparece al enlazar Native. Al subir librerías de UI/DI conviene comprobar contra qué versión de sus
  dependencias se publicaron.
- El framework dinámico se embebe en el bundle (frente al estático); impacto despreciable aquí.

## Verificación

1. ✅ `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` enlaza sin `Undefined symbols`.
2. ✅ Xcode 26 / simulador iPhone: la app arranca y muestra la UI Compose en **modo invitado**.
3. ✅ El análisis degrada de forma controlada (ONNX es stub en iOS): informa de que no se puede leer la
   muestra, sin tumbar la app.
4. ⏳ Pendiente: reverificar **Android** (`:androidApp:assembleDebug`) y **Desktop**, y los tests
   (`:shared:allTests`, que valida Mokkery 2.10.2).

## Referencias

- ADR-0004 - Bring-up de iOS (Fase 1): origen de este bloqueo.
- Koin 4.1 release notes (Compose 1.8 / Lifecycle 2.9 support); POMs de `koin-compose-viewmodel` 4.0.0 y
  4.1.0 en Maven Central.
- "What's new in Compose Multiplatform 1.10" (kotlinlang.org): Kotlin ≥ 2.2.20 para native;
  lifecycle 2.10.0 / navigation 2.9.2.
- JetBrains/compose-multiplatform-core PR #1451 (`CADisableMinimumFrameDurationOnPhone` obligatoria).
- KT-75781 (soporte de Xcode ≥ 16.3 desde Kotlin 2.1.21).
- AGENTS.md §3 (dependencias/KMP), §7, §16, §17.
