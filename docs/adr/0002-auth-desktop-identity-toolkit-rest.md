# ADR-0002 - Autenticación en Desktop mediante Firebase Identity Toolkit (REST)

## Estado

Aceptada — 2026-07-10

## Contexto

SPEC-0001 (Autenticación, Approved) implementó la Fase 1 dejando **Desktop con `StubAuthService`**: el
acceso con cuenta (email/Google) devuelve `NotSupportedOnPlatform` y solo funciona el invitado local. La
**Nota abierta 2** de esa spec dejó pendiente *"definir el mecanismo de auth con cuenta en Desktop"*.

Motivos para abordarlo ahora (Fase 2):

- **Desktop es target de entrega** y debe permitir login real con cuenta.
- SPEC-0005 (base remota) necesita un **`ownerUid` real** en las tres plataformas; en Desktop solo existe
  con auth real.
- El SDK **GitLive no tiene target Desktop/JVM**, así que la implementación de Android/iOS no sirve.

## Decisión

Implementar `AuthService` en `desktopMain` (`DesktopFirebaseAuthService`) contra la **API REST de Firebase
Auth (Identity Toolkit)** usando **Ktor**:

- `accounts:signInWithPassword` para email/contraseña.
- `accounts:sendOobCode` (PASSWORD_RESET) para el reset de SPEC-0002 (la anti-enumeración sigue en el VM).
- `securetoken.googleapis.com/v1/token` para refrescar el `idToken` **en memoria** cuando se necesite
  (relevante al usar el token contra Firestore con reglas cerradas; con reglas públicas aún no se usa).
- Invitado: local, igual que hoy.
- Errores REST mapeados a `AuthError` (RNF-4 de SPEC-0001); ningún tipo específico cruza a `commonMain`.

Decisiones de alcance y seguridad tomadas por el owner:

1. **Google Sign-In en Desktop: DIFERIDO.** Desktop soporta solo **email/contraseña** (+ invitado + reset).
   El flujo OAuth de escritorio (loopback + navegador) queda fuera de esta fase; el botón de Google en
   Desktop se oculta o devuelve `NotSupportedOnPlatform`.
2. **Sin persistencia de sesión en Desktop.** La sesión (idToken/refreshToken/uid) vive **solo en memoria**;
   al reiniciar la app se requiere **volver a iniciar sesión**. Es una **desviación consciente de RF-8**
   (persistencia de sesión) limitada a Desktop y **solo de UX**. A cambio, **no** se almacena un refresh
   token (credencial de larga vida) en un fichero no cifrado, respetando SECURITY_MOBILE (§Tokens: los
   refresh tokens exigen almacenamiento seguro, del que Desktop-JVM carece sin Keychain/Keystore). Si en el
   futuro se requiere persistencia, se abordará con almacenamiento seguro del SO (Keychain / Credential
   Manager / libsecret) vía `expect/actual`.
3. **Web API key** de Firebase obtenida por **configuración de build** (no hardcodeada en fuentes).

Esta fase introduce además el **`HttpClient` de Ktor central** y el módulo Koin de networking (RULES.md
§Networking), que **reutilizará SPEC-0005**.

## Alternativas consideradas

1. **SDK GitLive en Desktop** — imposible: sin target JVM.
2. **Backend propio que envuelva la auth** — coste desproporcionado para el alcance.
3. **OAuth de escritorio + Google ahora** — mayor superficie y trabajo; se difiere (decisión 1).
4. **Persistir refresh token en fichero (cifrado o no)** — descartada ahora por el riesgo de credencial de
   larga vida en reposo sin almacenamiento seguro real (decisión 2).

## Consecuencias

**Positivas**
- Login real con cuenta en Desktop (email/contraseña), desbloqueando la entrega y el `ownerUid` de
  SPEC-0005 en Desktop.
- Se levanta la infraestructura Ktor de networking, reutilizada por SPEC-0005.
- Ningún credencial de larga vida en reposo en Desktop.

**Negativas / deuda**
- La sesión de cuenta en Desktop **no sobrevive** al reinicio (re-login cada arranque) — desviación de RF-8
  documentada, solo Desktop.
- **Google no disponible en Desktop** hasta una fase posterior.
- Gestión de la **Web API key** por build config (a proveer por el owner).
- Mantener el mapeo de errores REST → `AuthError` alineado con el de Android.

## Referencias

- SPEC-0001 §Notas abiertas (2), §No objetivos (Fase 2). SPEC-0002 (reset). SPEC-0005 (usa el `ownerUid`).
- RULES.md §Networking, §Interceptors. SECURITY_MOBILE §Tokens y credenciales, §Networking. AGENTS.md §8.
