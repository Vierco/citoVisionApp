# ADR-0006 - Autenticación en iOS: Identity Toolkit REST + Keychain + Google Sign-In desde Swift

## Estado

Aceptada — 2026-08-26

## Contexto

La Fase 1 del bring-up de iOS (ADR-0004) dejó la autenticación **en stub**: `StubAuthService`,
`StubAuthTokenProvider` y `StubGoogleSignInLauncher`. La app arranca en modo invitado, pero no hay login
real ni ID token para autorizar Firestore/Storage. Esta fase cierra la autenticación en iOS.

Lo que exige la normativa del proyecto:

- **SPEC-0001**: RF-1 (email/contraseña), **RF-2 (Google)**, RF-3 (invitado), RF-6 (`signOut` que borra
  credenciales), **RF-8 (la sesión persiste entre reinicios)**; RNF-1 (contrato en `commonMain`),
  **RNF-4 (ningún tipo de Firebase cruza a `commonMain`)**, RNF-5 (nunca loguear tokens).
- **SECURITY_MOBILE §Tokens y credenciales**: *"Refresh Tokens: almacenamiento seguro. **iOS: Keychain**"*.

Estado actual por plataforma:

| | Android | Desktop | iOS (antes de este ADR) |
|---|---|---|---|
| Motor | GitLive (SDK nativo Firebase) | Identity Toolkit REST (ADR-0002) | Stub |
| Sesión persistente | Sí (SDK) | **No** (solo memoria, desviación de RF-8) | — |
| Google Sign-In | Sí (Credential Manager) | No (`NotSupportedOnPlatform`) | — |

Restricciones fijadas por el owner en esta conversación:

1. **CocoaPods queda descartado** como mecanismo de integración (Apple lo está deprecando; SPM es el
   camino).
2. **Google Sign-In debe estar presente en iOS** y se resuelve **en esta fase**, para dejar la
   autenticación cerrada.

### Por qué NO se usa GitLive (el motor de Android) en iOS

Sería la opción de paridad, pero con CocoaPods descartado queda atrapada entre restricciones
incompatibles (verificado, no supuesto):

- GitLive **no** enlaza el Firebase iOS SDK de forma transitiva: lo debe aportar la app.
- El import nativo de Swift Packages en KMP (`swiftPMDependencies {}`) requiere **Kotlin 2.4.20-Beta2**;
  el proyecto acaba de estabilizarse en **2.2.20** (ADR-0005).
- Los klibs de GitLive arrastran *linker options* de la era CocoaPods (`-framework FirebaseCore`…) que,
  con SPM, apuntan a rutas por producto que el linker de Kotlin/Native no busca.
- **Conflicto estático/dinámico sin salida limpia:** Firebase vía SPM empuja a framework **estático**,
  pero el estático es justo lo que Xcode 26 rompe en este proyecto (`cannot link directly with
  'SwiftUICore'`, ADR-0005), y el **dinámico** tiene problemas conocidos de símbolos indefinidos con
  Firebase.

## Decisión

En iOS, **Kotlin no enlaza contra ningún SDK nativo de Firebase ni de Google**. La autenticación se
resuelve por **REST** y el SDK de Google vive **solo en Swift**.

1. **`AuthService` de iOS = Identity Toolkit REST.** Se promueve la implementación de Desktop
   (`DesktopFirebaseAuthService`, hoy en `desktopMain` pero Kotlin puro sin APIs de Desktop) a
   `commonMain` como implementación compartida, parametrizando lo único que difiere: **dónde se guardan
   los tokens**. Desktop e iOS pasan a compartir motor.
2. **Google Sign-In (RF-2)** en tres piezas:
   - El **SDK GoogleSignIn de iOS se añade por SPM únicamente al target de Xcode**; Kotlin no hace
     cinterop ni enlaza contra él.
   - `GoogleSignInLauncher` de iOS es un **puente**: Kotlin expone un punto de registro que Swift rellena
     al arrancar; Swift lanza el flujo nativo y devuelve el `idToken` de Google.
   - Ese `idToken` se canjea por sesión Firebase con un método nuevo **`signInWithIdp`** en
     `IdentityToolkitAuthDataSource` (endpoint `accounts:signInWithIdp`). El puerto ya está diseñado así:
     `AuthService.signInWithGoogle(idToken: String)`.
3. **Persistencia de sesión (RF-8)** mediante un puerto nuevo **`TokenStore`**:
   - **iOS**: implementación sobre **Keychain**, usando `platform.Security` (ya incluido en
     Kotlin/Native; sin dependencias nuevas). Cumple SECURITY_MOBILE §Tokens.
   - **Desktop**: implementación en memoria, que preserva el comportamiento actual (ADR-0002) sin
     regresión.
4. **Android no se toca**: sigue con GitLive.
5. Ningún token se loguea ni se persiste fuera del almacén seguro (RNF-5, AGENTS.md §11).

### Prerrequisito operativo

En el proyecto Firebase `citovision-cf661` **no existe hoy ninguna app iOS registrada** (solo Android y
Web). Hay que **registrar la app iOS** con bundle id `dev.lovelace.citovision` para obtener el
**cliente OAuth de iOS** (`CLIENT_ID` / `REVERSED_CLIENT_ID`) que Google Sign-In necesita, y su URL
scheme en el `Info.plist`. Es una operación sobre el proyecto de producción y **la realiza el
desarrollador** (o la autoriza expresamente).

## Alternativas consideradas

1. **GitLive + Firebase iOS SDK vía CocoaPods.** Sería el camino documentado por GitLive y daría paridad
   con Android. **Descartada por decisión del owner**: CocoaPods está en vías de deprecación.
2. **GitLive + Firebase iOS SDK vía SPM manual.** Descartada: sin `swiftPMDependencies` (requiere Kotlin
   2.4.20-Beta2) hay que pelear *linkerOpts* y rutas de DerivedData, y persiste el conflicto
   estático/dinámico descrito arriba. Riesgo alto de bloqueo.
3. **REST sin almacenamiento seguro (calcar Desktop).** Descartada: incumple **RF-8** y contradice
   **SECURITY_MOBILE** (*iOS: Keychain*). Sería aceptable como desviación temporal en Desktop, no en una
   plataforma móvil.
4. **Subir Kotlin a 2.4.20-Beta2** para usar el import SPM nativo. Descartada: es Alpha/Beta y el
   toolchain acaba de estabilizarse en 2.2.20 tras el trabajo de ADR-0005.
5. **Implementar Google Sign-In con OAuth en navegador (`ASWebAuthenticationSession`).** Descartada para
   iOS: peor UX que el flujo nativo y no aprovecha la cuenta ya presente en el dispositivo. Se mantiene
   como candidata **para Desktop** en el futuro.

## Consecuencias

**Positivas**

- Cierra la autenticación de iOS cumpliendo SPEC-0001 (RF-1, RF-2, RF-3, RF-6, RF-8) y SECURITY_MOBILE.
- **Elimina el riesgo de enlazado**: Kotlin no depende de frameworks nativos, así que el framework puede
  seguir siendo **dinámico** (necesario por Xcode 26) sin conflicto.
- **Reutiliza código ya probado**: `IdentityToolkitAuthDataSource` y la lógica de sesión/refresh ya
  existen y tienen tests en `commonTest`/`desktopTest`.
- **Mejora Desktop indirectamente**: al extraer `TokenStore`, Desktop queda a un paso de tener sesión
  persistente si se decide más adelante.
- **Desbloquea Google Sign-In en Desktop** a futuro: `signInWithIdp` es compartido; solo faltaría obtener
  el `idToken` por otra vía (alternativa 5).
- RNF-4 se cumple por construcción: no hay tipos de Firebase en `commonMain`.

**Negativas / deuda**

- **Dos motores de auth que mantener**: GitLive en Android y REST en Desktop/iOS. Queda contenido tras el
  puerto `AuthService`, pero un cambio de contrato hay que reflejarlo en ambos.
- Código propio a escribir y testear: `signInWithIdp`, `TokenStore` + Keychain, y el puente Swift↔Kotlin.
- El **puente Swift→Kotlin** para el `idToken` es una pieza específica de plataforma más (registro de un
  callback al arrancar); si Swift no lo registra, Google Sign-In debe fallar de forma controlada.
- La revocación en servidor (usuario deshabilitado) no es inmediata: el ID token vigente sigue siendo
  válido hasta caducar. Es el mismo comportamiento que Desktop y lo acotan las reglas de seguridad.
- Refactor de alcance medio: mover `DesktopFirebaseAuthService` a `commonMain` toca el módulo Koin de
  Desktop y su test.

## Verificación

En simulador y, si es posible, en dispositivo:

1. Login con **email/contraseña** correcto e incorrecto (mensajes de error tipados, RF-5).
2. **Google Sign-In** completo: selector nativo → `idToken` → sesión Firebase.
3. **Invitado** y **`signOut`** (que borra el token del Keychain, RF-6).
4. **Recuperación de contraseña** (SPEC-0002).
5. **RF-8**: cerrar y reabrir la app → la sesión sigue activa sin volver a introducir credenciales.
6. Con sesión iniciada, que **Firestore y Storage respondan** (el ID token viaja en `Authorization`).
7. **Sin regresión**: Android y Desktop siguen compilando y autenticando igual.
8. Tests de `commonTest` verdes (incluye los nuevos de `signInWithIdp`).

## Referencias

- SPEC-0001 (autenticación) y SPEC-0002 (recuperación de contraseña).
- ADR-0002 (auth Desktop vía Identity Toolkit REST): motor que aquí se comparte.
- ADR-0004 (bring-up iOS Fase 1): dejó la auth en stub.
- ADR-0005 (toolchain y framework dinámico): origen del conflicto estático/dinámico.
- ADR-0001 (Firestore/Storage REST): consumidor del `AuthTokenProvider`.
- SECURITY_MOBILE §Tokens y credenciales (iOS: Keychain). AGENTS.md §3, §8, §11, §14.
- Firebase Auth REST: `accounts:signInWithIdp`. Documentación de import SPM en KMP (Kotlin 2.4.20-Beta2).
