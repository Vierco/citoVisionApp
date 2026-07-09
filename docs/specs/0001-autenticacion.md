    # SPEC-0001 - Autenticación

## Estado

Approved

## Contexto

citoVision es una app KMP (Android, iOS, Desktop) para profesionales del sector médico.
La pantalla de login ya está maquetada (`LoginScreen`) con tres vías de acceso: email/contraseña,
Google e invitado. Falta la lógica real de autenticación.

La autenticación se apoyará en **Firebase Auth vía GitLive** (`dev.gitlive:firebase-auth`), que
**solo tiene target Android/iOS/JS, no Desktop-JVM**. Por eso la feature se implementa con el patrón
**puerto + `expect/actual` + DI** (AGENTS.md §2): un contrato común en `commonMain`, implementación
real por plataforma y stub en Desktop. Ver memoria del proyecto sobre la integración de Firebase.

## Objetivo

Permitir que un usuario inicie sesión en citoVision mediante:
1. **Email + contraseña** (cuenta Firebase existente).
2. **Cuenta de Google** (proveedor Google de Firebase Auth).
3. **Acceso como invitado** (sesión local sin cuenta, sin backend).

Tras una autenticación correcta, navegar a `MainRoute`. La sesión persiste entre reinicios. La lógica
de validación y de sesión vive en el `LoginViewModel` y en servicios de dominio; la UI queda *stateless*.

## No objetivos

- Registro de nuevos usuarios (sign-up) y recuperación de contraseña ("¿Olvidaste tu contraseña?"
  queda como TODO en la maqueta). Se abordará en una spec posterior.
- Autorización / control de acceso a recursos concretos (roles, permisos).
- Persistencia manual de tokens en almacenamiento cifrado (Firebase gestiona sus propios tokens; la
  persistencia de fase 1 se limita al flag no sensible de invitado — ver Seguridad).
- Implementación **real** de auth en Desktop con cuenta Firebase (fase 2: solo stub en fase 1).

## Usuarios / actores

- **Profesional médico** con cuenta (email/contraseña o Google).
- **Invitado**: usuario que accede sin cuenta para explorar la app; sin datos en la nube.

## Requisitos funcionales

- **RF-1**: El usuario puede iniciar sesión con email y contraseña válidos.
- **RF-2**: El usuario puede iniciar sesión con su cuenta de Google.
- **RF-3**: El usuario puede continuar como invitado (sesión local, sin cuenta).
- **RF-4**: Tras login correcto (cualquier vía), la app navega a la pantalla principal (`MainRoute`)
  y elimina el login de la pila (`popUpTo<LoginRoute> { inclusive = true }`).
- **RF-5**: Los errores de autenticación se muestran al usuario mediante diálogo, con texto desde
  recursos (ES/EN), sin exponer detalles internos ni stack traces.
- **RF-6**: Existe la operación `signOut()` que cierra la sesión y elimina cualquier credencial/estado
  de sesión en memoria y el flag de invitado persistido.
- **RF-7**: La validación de formato (email y longitud de contraseña) ocurre en cliente **antes** de
  llamar al backend, para feedback inmediato; no sustituye la validación del backend.
- **RF-8**: La sesión persiste entre reinicios de la app. Al arrancar, la Splash comprueba si hay
  sesión activa (cuenta Firebase o invitado) y navega directamente a `MainRoute`; si no, a `LoginRoute`.
  La sesión de invitado se persiste con **DataStore** (flag no sensible); la sesión de cuenta Firebase
  la mantiene el propio SDK de Firebase.

## Requisitos no funcionales

- **RNF-1**: Contrato de auth (`AuthService`) definido en `commonMain`; implementaciones por
  plataforma (Firebase en `androidMain`/`iosMain`, stub en `desktopMain`).
- **RNF-2**: Toda operación de auth es `suspend` y devuelve `Result` con error de dominio tipado
  (nunca excepción como flujo normal). Dispatchers inyectados vía Koin.
- **RNF-3**: DI con Koin, un módulo `authModule`; el binding de `AuthService` lo aporta cada módulo
  de plataforma. `LoginViewModel` se resuelve por Koin.
- **RNF-4**: Ningún tipo de Firebase (`FirebaseUser`, `FirebaseAuthException`…) cruza a `commonMain`;
  se mapea a modelos/errores de dominio (`AuthUser`, `AuthError`).
- **RNF-5**: Logging con Napier; **nunca** loguear contraseñas, tokens ni PII.
- **RNF-6**: Textos y colores desde recursos/tema (sin hardcodear).
- **RNF-7**: Tiempos de respuesta con feedback visual (estado `Loading`) para operaciones de red.
- **RNF-8**: La persistencia de sesión de invitado se accede mediante un repositorio/puerto
  (`SessionRepository`) en `commonMain`, con implementación DataStore (multiplataforma); nunca acceso
  directo a DataStore desde UI o ViewModel.

## Reglas de negocio

- **RN-1 (contraseña — longitud)**: Longitud mínima 6 (requisito de Firebase Auth), máxima 64.
- **RN-2 (email — formato)**: Debe cumplir `^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[a-z]{2,}$`.
- **RN-3 (invitado)**: El acceso como invitado crea una sesión **local** (no crea cuenta ni escribe
  en backend). Un invitado no tiene acceso a datos en la nube.
- **RN-4 (persistencia de invitado)**: El flag de sesión de invitado se guarda con DataStore (dato
  no sensible). Nunca se guardan tokens ni credenciales en DataStore.
- **RN-5**: Nunca se persiste la contraseña del usuario (SECURITY_MOBILE).

## Estados de UI

Modelo `LoginUiState` gestionado por `LoginViewModel`:

- **Idle**: formulario editable.
- **Loading**: operación de auth en curso (deshabilitar acciones, mostrar indicador).
- **Success**: autenticado → emite evento de navegación a `MainRoute`.
- **Error(messageRes)**: muestra diálogo con el mensaje correspondiente; vuelve a Idle al cerrar.

Estados de campo: error de formato de email y error de longitud de contraseña se señalizan en cliente
(diálogo o helper text), como ya hace la maqueta actual.

Arranque (Splash): estado transitorio de comprobación de sesión antes de decidir `MainRoute` vs
`LoginRoute`.

## Contratos de datos

Propuesta de contrato en `commonMain` (capa application/domain):

```kotlin
// Modelo de dominio (sin tipos de Firebase)
data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val isGuest: Boolean,
)

sealed interface AuthError {
    data object InvalidCredentials : AuthError   // email/contraseña incorrectos
    data object UserNotFound : AuthError
    data object Network : AuthError
    data object TooManyRequests : AuthError
    data object GoogleSignInCancelled : AuthError
    data object GoogleSignInFailed : AuthError
    data object NotSupportedOnPlatform : AuthError // stub Desktop (fase 1)
    data class Unknown(val cause: String?) : AuthError
}

interface AuthService {
    val currentUser: Flow<AuthUser?>            // sesión observable
    suspend fun signInWithEmail(email: String, password: String): Result<AuthUser>
    suspend fun signInWithGoogle(idToken: String): Result<AuthUser> // idToken lo obtiene el flujo nativo
    suspend fun signInAsGuest(): Result<AuthUser>                    // sesión local
    suspend fun signOut(): Result<Unit>
}

// Persistencia de sesión local (invitado) — DataStore
interface SessionRepository {
    fun isGuestSession(): Flow<Boolean>
    suspend fun setGuestSession(active: Boolean)
    suspend fun clear()
}
```

Nota: la obtención de la credencial de Google (selector de cuenta / `idToken`) es específica de
plataforma (Android: Credential Manager / Google Identity; Desktop: OAuth web — fase 2). El puerto
recibe el `idToken` ya resuelto.

## Errores

| Situación | Origen | Resultado UI |
|---|---|---|
| Email con formato inválido | cliente | Diálogo "formato de email" (no llama backend) |
| Contraseña < 6 | cliente | Diálogo longitud mínima |
| Credenciales incorrectas | backend | `AuthError.InvalidCredentials` → diálogo genérico |
| Sin conexión / timeout | backend | `AuthError.Network` → diálogo reintentar |
| Demasiados intentos | backend | `AuthError.TooManyRequests` |
| Google cancelado por el usuario | plataforma | `AuthError.GoogleSignInCancelled` (silencioso o aviso leve) |
| Google falla | plataforma | `AuthError.GoogleSignInFailed` |
| Login con cuenta en Desktop (fase 1) | stub | `AuthError.NotSupportedOnPlatform` |

Los mensajes de error de credenciales serán **genéricos** (no revelar si el email existe o no).

## Casos borde

- Doble pulsación del botón de login → ignorar mientras `Loading`.
- Campos vacíos → validación en cliente antes de llamar.
- Sesión ya activa al abrir la app (cuenta o invitado) → la Splash navega directamente a `MainRoute`.
- Invitado que luego quiere iniciar sesión con cuenta → permitido desde Ajustes (futuro).
- Rotación / recomposición → el estado sobrevive en el ViewModel.
- Desktop, fase 1: email/Google devuelven `NotSupportedOnPlatform`; **invitado sí funciona** (local).

## Telemetría / analytics

Fuera de alcance en esta spec. Si se añade, registrar solo eventos no sensibles (éxito/fallo de login,
método usado) sin PII ni credenciales.

## Seguridad y privacidad

- Auth **nunca** es frontera de seguridad solo-cliente; las autorizaciones se validan en backend
  (SECURITY_MOBILE). Firebase gestiona la verificación de credenciales.
- **Nunca** persistir la contraseña. Los tokens de sesión los gestiona el SDK de Firebase; si más
  adelante se persisten manualmente, usar Android Keystore / iOS Keychain vía `expect/actual`.
- **Persistencia con DataStore**: DataStore **no está cifrado**; por eso solo se guarda ahí el flag
  no sensible de sesión de invitado. Los tokens de cuenta Firebase los gestiona el SDK; nunca se
  colocan tokens ni credenciales en DataStore.
- En `signOut` eliminar credenciales y estado sensible en memoria y limpiar el flag de invitado.
- No loguear contraseñas, tokens ni PII (Napier).
- Revisar OWASP MASVS / Mobile Top 10 (auth) antes de cerrar la implementación.

## Criterios de aceptación

- **CA-1**: Con email y contraseña válidos de una cuenta existente, el login navega a `MainRoute`.
- **CA-2**: Con credenciales incorrectas, se muestra diálogo de error genérico y se permanece en login.
- **CA-3**: Una contraseña de menos de 6 caracteres se rechaza en cliente sin llamar al backend.
- **CA-4**: El login con Google válido navega a `MainRoute`; cancelarlo no rompe ni navega.
- **CA-5**: "Continuar como invitado" navega a `MainRoute` con sesión de invitado (funciona en las
  3 plataformas, Desktop incluido).
- **CA-6**: En Desktop, email/Google devuelven `NotSupportedOnPlatform` de forma controlada (sin crash).
- **CA-7**: Ningún tipo de Firebase aparece en `commonMain`.
- **CA-8**: No hay contraseñas ni tokens en logs.
- **CA-9**: Tras iniciar sesión (cuenta o invitado) y reiniciar la app, la Splash lleva directamente
  a `MainRoute` sin pasar por login. Tras `signOut`, el siguiente arranque lleva a `LoginRoute`.

## Tests requeridos

- **commonTest**: validadores de email y de longitud de contraseña; transiciones de
  `LoginUiState` en `LoginViewModel` usando un `FakeAuthService` (éxito, credenciales inválidas,
  red, invitado). Verificar que doble pulsación no dispara doble llamada.
- **commonTest (persistencia)**: `SessionRepository` sobre DataStore (guardar/leer/borrar el flag de
  invitado); lógica de la Splash que decide `MainRoute` vs `LoginRoute` según sesión.
- **Android**: verificación de que el grafo de Koin resuelve `AuthService` (impl Firebase). Prueba
  manual/instrumentada de login real contra Firebase (entorno de test).
- **UI (opcional)**: estados loading/error/success de `LoginScreen` con test tags.

## Dependencias

- `dev.gitlive:firebase-auth` (2.1.0) en `androidMain` (+ `iosMain` cuando se aborde iOS).
- `dev.gitlive:firebase-app` + Firebase BOM 33.15.0 (ya integrados).
- Plugin `com.google.gms.google-services` (ya aplicado en `androidApp`).
- `androidx.datastore:datastore-preferences` (multiplataforma, versión 1.1.1 ya en el catálogo) para
  persistir el flag de sesión de invitado.
- Koin (DI), Coroutines/Flow, Napier (logging).
- Flujo nativo de Google Sign-In por plataforma (Android: Credential Manager) — a detallar en
  implementación.

## Notas abiertas

1. **Registro y recuperación de contraseña**: spec futura.
2. **Google/Email en Desktop (fase 2)**: definir mecanismo (Firebase REST / Identity Toolkit / OAuth).
