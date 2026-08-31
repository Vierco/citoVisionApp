# ADR-0004 - Bring-up de iOS (Fase 1: arranque mínimo / vertical slice)

## Estado

Aceptada — 2026-08-25

## Contexto

El MVP de citoVision se entregó para **Android** y **Desktop (macOS)**. iOS quedó como *target de
biblioteca compilable pero sin app ejecutable*:

- Los tres targets (`iosX64`, `iosArm64`, `iosSimulatorArm64`) ya compilan a un **framework estático
  `shared`** (`shared/build.gradle.kts`).
- `shared/src/iosMain` tiene `actual` **reales** para Room, DataStore, Ktor/Darwin, `IosUrlOpener`,
  dispatchers y el almacén de imágenes (Okio). El KSP de Room está declarado para los tres targets.
- **No existe** ningún proyecto Xcode, ni carpeta `iosApp/`, ni punto de entrada
  `MainViewController()`. Por eso no hay ningún `.xcodeproj` en el repo.
- Son **stub** en iOS: Auth (`StubAuthService`/`StubAuthTokenProvider`/`StubGoogleSignInLauncher`),
  inferencia ONNX (`OnnxRunnerImpl` lanza `NotImplementedError`) e `ImageDecoder` (devuelve `null`).
  `SystemBarsAppearance` es un no-op intencional.

`ARCHITECTURE.md` ya prevé la estructura `iosApp/` (líneas 15-18) con `iosApp.xcodeproj` +
`iOSApp.swift`. Existe, además, una **discrepancia** menor a reconciliar: `ARCHITECTURE.md:394`
menciona "iOS SwiftUI" para la capa de presentación, pero la UI de la app es **Compose Multiplatform
compartida** (`App()` en `commonMain`), común a las tres plataformas.

Condicionantes de esta fase (decididos con el owner):

- Alcance **Fase 1 = vertical slice**: conseguir que la app **corra en iOS** con lo que ya funciona
  (UI Compose, Room, DataStore local), dejando **Auth, ONNX e ImageDecoder en stub**.
- Destino: **simulador y dispositivo físico**. El **signing lo gestiona el desarrollador** (cuenta
  Apple Developer, certificados, provisioning); el andamiaje del proyecto deja esos huecos sin tocar
  certificados (AGENTS.md §22).
- App **KMP** (AGENTS.md §2, §3): la lógica y la UI compartidas viven en `commonMain`; iOS solo
  aporta el entry point específico de plataforma.

## Decisión

Crear el **punto de entrada iOS** y un **proyecto Xcode `iosApp/`** que hospede la UI Compose ya
existente, manteniendo las piezas de negocio dependientes de plataforma en stub durante esta fase.

- **UI:** Compose Multiplatform compartida. iOS lleva un **host fino** (SwiftUI
  `UIViewControllerRepresentable` → `ComposeUIViewController { App() }`), **no** pantallas SwiftUI.
  Esto **reconcilia** `ARCHITECTURE.md:394`: la presentación es Compose compartido en las tres
  plataformas; SwiftUI es únicamente el contenedor de arranque.
- **Entry point Kotlin** en `shared/src/iosMain`: `fun MainViewController(): UIViewController` +
  `fun initialize()` que ejecuta **una sola vez** `initKoin()` + `Napier.base(DebugAntilog())`,
  replicando el patrón de `desktopApp/Main.kt` y `androidApp/MainActivity.kt`. `initKoin()` sin
  propiedades es seguro: los consumidores de la Web API key usan `getProperty(..., "")` con default
  (`InfrastructureModule.kt`), así que el grafo stub de iOS no la necesita.
- **`iosApp/` es un proyecto Xcode puro, NO un módulo Gradle** (patrón estándar KMP): consume el
  framework `shared` mediante la tarea `embedAndSignAppleFrameworkForXcode` en un *build phase*. No
  se modifica `settings.gradle.kts` ni `shared/build.gradle.kts` (framework y KSP ya declarados).
- **Signing:** `CODE_SIGN_STYLE = Automatic`, `DEVELOPMENT_TEAM` vacío, `PRODUCT_BUNDLE_IDENTIFIER
  = dev.lovelace.citovision`. El simulador arranca sin firma; para dispositivo, el desarrollador
  selecciona su *Team* en Xcode. No se versionan certificados ni provisioning.
- **Auth/ONNX/ImageDecoder** permanecen como stub (trabajo **aditivo** de fases posteriores); la app
  funciona en **modo invitado** con persistencia local.

## Alternativas consideradas

1. **`iosApp` como módulo Gradle.** Descartada. El patrón KMP estándar es proyecto Xcode puro que
   consume el framework vía `embedAndSign`; convertirlo en módulo Gradle añade complejidad sin
   beneficio para un vertical slice.
2. **Pantallas nativas SwiftUI en iOS.** Descartada. Duplicaría la UI ya escrita en Compose
   compartido, rompiendo la estrategia KMP del proyecto y multiplicando el mantenimiento.
3. **Esperar a tener Auth + ONNX iOS antes de arrancar la app.** Descartada. Bloquea la verificación
   más básica (¿arranca y se ve la UI en iOS?) tras el trabajo más pesado (cinterop). El vertical
   slice desbloquea todo lo demás y reduce riesgo de integración temprano.

## Consecuencias

**Positivas**

- Primer hito verificable en iOS: la app arranca y muestra la UI Compose real, con Room y DataStore
  funcionando en local.
- Trabajo mínimo y aislado: sin cambios en Gradle ni en la lógica compartida; solo entry point +
  proyecto Xcode.
- Sienta la base para las fases aditivas (Auth, ONNX, ImageDecoder) sin re-arquitectura.
- Reconcilia la documentación (`ARCHITECTURE.md:394`) con la realidad del código.

**Negativas / deuda**

- **Auth en stub:** sin login real en iOS; solo modo invitado. Hay que verificar que ninguna pantalla
  deje la app en un callejón sin salida al no haber sesión.
- **Análisis no operativo en iOS:** ONNX lanza `NotImplementedError` e `ImageDecoder` devuelve
  `null`; el flujo de análisis debe fallar de forma controlada (o quedar deshabilitado en UI).
- **`project.pbxproj` hecho a mano** es frágil; podría requerir un ajuste puntual del desarrollador
  en la GUI de Xcode.
- **Signing de dispositivo** queda en manos del desarrollador (fuera de alcance del agente, §22).
- Piezas cosméticas pendientes: `SystemBarsAppearance` no-op; validar FileKit en iOS en runtime.

## Fuera de alcance (fases futuras)

- Firebase Auth iOS real (`GoogleService-Info.plist` + Firebase nativo).
- Inferencia ONNX iOS (cinterop `onnxruntime`, EP CoreML) e `ImageDecoder` con CoreGraphics.
- `iosTest`, screenshot tests iOS, secure storage / Keychain.
- Signing de producción, notarización y distribución TestFlight/App Store.

## Referencias

- SPEC-0006 - Análisis celular con modelo ONNX (marca la inferencia iOS como fase futura / cinterop).
- ADR-0003 - Inferencia on-device con ONNX Runtime (iOS "futuro" vía cinterop).
- ADR-0002 - Auth Desktop vía Identity Toolkit REST (tokens en memoria; sin Keychain).
- ARCHITECTURE.md (estructura `iosApp/` prevista; discrepancia línea 394 a reconciliar).
- AGENTS.md §2, §3 (KMP/arquitectura), §7 (spec-driven), §16 (no compilar), §22 (signing).
