# SPEC-0002 - Recuperación de contraseña

## Estado

Approved

## Contexto

Extiende SPEC-0001. citoVision es un MVP: los usuarios se crean **manualmente** desde la consola de
Firebase (el owner controla quién prueba la app), por lo que **el registro/sign-up sigue fuera de
alcance**. Esta spec añade únicamente la **recuperación de contraseña**, que SPEC-0001 había diferido
a "una spec posterior".

Se apoya en `Firebase Auth: sendPasswordResetEmail`. Firebase envía el correo, genera el enlace y
gestiona su caducidad; la app no manipula tokens. Sigue el patrón puerto + `expect/actual` de la fase 1
(Firebase en Android; stub en Desktop/iOS).

## Objetivo

Desde la pantalla de login, un usuario que ha olvidado su contraseña puede solicitar un correo de
restablecimiento introduciendo su email en un **popup**.

## No objetivos

- Registro / alta de usuarios (se crean manualmente en consola).
- Cambio de contraseña dentro de la app (estando logueado).
- Completar el restablecimiento dentro de la app (deep link): se usa el flujo web estándar de Firebase.
- Recuperación en Desktop/iOS con cuenta real (stub → `NotSupportedOnPlatform`, fase posterior).

## Usuarios / actores

- **Usuario con cuenta** (creada por el owner) que ha olvidado su contraseña.

## Requisitos funcionales

- **RF-1**: El `TextButton` "¿Olvidaste tu contraseña?" del login abre un popup (AlertDialog) con un
  campo de email y acciones Cancelar / Enviar.
- **RF-2**: Si el usuario ya escribió un email en el formulario de login, el popup lo pre-rellena.
- **RF-3**: Se valida el formato del email en cliente **antes** de llamar a Firebase.
- **RF-4**: Al enviar, se llama a `AuthService.sendPasswordReset(email)`.
- **RF-5**: Tras el envío se muestra un mensaje de confirmación **genérico** y se cierra el popup.
- **RF-6**: En Desktop/iOS (stub) la operación devuelve `NotSupportedOnPlatform` y se informa de forma
  controlada (sin crash).

## Requisitos no funcionales

- **RNF-1**: Nuevo método en el puerto `AuthService` (commonMain); impl Firebase en androidMain, stub
  común para Desktop/iOS.
- **RNF-2**: Operación `suspend` que devuelve `Result<Unit, AuthError>`; sin excepciones como flujo.
- **RNF-3**: Estado del popup gestionado por `LoginViewModel` (dentro de `LoginUiState` o un estado
  anidado); la UI es stateless.
- **RNF-4**: Textos desde recursos (ES/EN); sin hardcodear.
- **RNF-5**: Logging con Napier sin registrar el email como PII innecesaria.

## Reglas de negocio

- **RN-1 (email — formato)**: mismo regex que SPEC-0001
  (`^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[a-z]{2,}$`).
- **RN-2 (anti-enumeración de usuarios)**: el mensaje de confirmación es **el mismo** tanto si el
  email corresponde a una cuenta como si no. Un `UserNotFound` de Firebase se trata como **éxito** de
  cara a la UI. Así no se revela qué correos están registrados (OWASP / SECURITY_MOBILE).

## Estados de UI

Popup de recuperación (parte de `LoginUiState`):

- **Cerrado**: no visible.
- **Idle**: email editable, botón Enviar activo.
- **Loading**: enviando (deshabilita acciones).
- **Enviado**: cierra el popup y muestra confirmación genérica (diálogo simple o snackbar).
- **Error de formato**: mensaje dentro del popup, no se llama a Firebase.

## Contratos de datos

Ampliación del puerto de SPEC-0001:

```kotlin
interface AuthService {
    // … métodos de SPEC-0001 …
    suspend fun sendPasswordReset(email: String): Result<Unit, AuthError>
}
```

## Errores

| Situación | Origen | Resultado UI |
|---|---|---|
| Email con formato inválido | cliente | Mensaje en el popup; no llama a Firebase |
| Email no registrado (`UserNotFound`) | backend | **Tratado como éxito** (confirmación genérica, RN-2) |
| Sin conexión / timeout | backend | `AuthError.Network` → aviso de reintento |
| Demasiadas solicitudes | backend | `AuthError.TooManyRequests` |
| Desktop/iOS | stub | `AuthError.NotSupportedOnPlatform` → aviso controlado |

## Casos borde

- Doble pulsación de Enviar → ignorar mientras `Loading`.
- Campo vacío → validación de formato lo bloquea.
- Cerrar el popup a mitad → no envía nada, estado limpio.

## Telemetría / analytics

Fuera de alcance.

## Seguridad y privacidad

- **No enumeración de usuarios**: confirmación genérica siempre (RN-2).
- Firebase gestiona el enlace de restablecimiento y su caducidad; la app no maneja ni persiste tokens.
- No loguear el email como PII innecesaria.
- Revisar OWASP MASVS / Mobile Top 10 (recuperación de credenciales).

## Criterios de aceptación

- **CA-1**: Con email válido de una cuenta existente, Firebase envía el correo y la UI muestra la
  confirmación genérica.
- **CA-2**: Con un email **no registrado** con formato válido, se muestra **la misma** confirmación
  (no se revela que no existe).
- **CA-3**: Con email de formato inválido, se muestra error en el popup y **no** se llama a Firebase.
- **CA-4**: En Desktop, la acción informa "no disponible en escritorio" sin romper.
- **CA-5**: Ningún tipo de Firebase aparece en `commonMain`.

## Tests requeridos

- **commonTest**: validador de email; transiciones del estado del popup en `LoginViewModel` con un
  `FakeAuthService` (éxito, `UserNotFound` → éxito de UI, `Network` → error).
- **Android**: prueba manual del envío real (llega el correo a una cuenta de prueba).

## Dependencias

- `dev.gitlive:firebase-auth` (ya integrado), SPEC-0001.

## Notas abiertas

- (Ninguna) Decisiones cerradas: anti-enumeración OWASP (RN-2) y confirmación mediante **diálogo
  simple** (coherente con los avisos actuales del login).
