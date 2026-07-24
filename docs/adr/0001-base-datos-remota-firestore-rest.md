# ADR-0001 - Base de datos remota mediante Firestore + Storage por API REST

## Estado

Aceptada — 2026-07-10

## Contexto

citoVision necesita una base de datos **remota** para los análisis, disponible **solo para usuarios con
cuenta iniciada** (correo o Google) y con **alcance por usuario** (un usuario solo ve sus propios
pacientes). Alimenta la escritura desde "Iniciar Escáner" y la consulta de la pestaña Pacientes
(ver SPEC-0005). La persistencia **local** ya existe con Room (SPEC-0004) y es independiente de la remota.

Condicionantes:

- La app tiene tres targets de entrega: **Android, iOS y Desktop (JVM)**.
- Firebase ya está integrado para **auth** vía el SDK **GitLive** (`dev.gitlive:firebase-*`), pero
  **GitLive no publica target Desktop/JVM** (solo Android, iOS, JS). Por eso la auth de Desktop se resuelve
  con un stub y su implementación real (REST/Identity Toolkit) es la **Fase 2**, inminente.
- La feature requiere una **consulta puntual** por código de paciente y una **escritura puntual** por
  análisis. **No** requiere realtime, listeners ni caché offline de la base remota.
- AGENTS.md §11 y SECURITY_MOBILE: *el cliente nunca es frontera de seguridad; toda autorización se valida
  en el servidor.* La autorización efectiva debe recaer en las reglas de seguridad del backend.

## Decisión

Usar **Cloud Firestore + Firebase Storage** como base de datos remota, accedidos mediante su **API REST**
con **Ktor Client** y una **única implementación en `commonMain`** (no el SDK GitLive).

- Metadatos del análisis → documento **Firestore** (`analyses/{analysisId}`).
- Imagen del análisis → objeto en **Firebase Storage**; su URL/ruta se guarda en el documento.
- Acceso a través de un puerto `RemoteAnalysisDataSource` (capa `application`), con DTOs `@Serializable`
  e implementación Ktor en `infrastructure`.
- La sincronización local→remoto se hace con un **patrón outbox** local (cola duradera en Room), no de
  forma bloqueante (ver SPEC-0005).

## Alternativas consideradas

1. **Firestore con el SDK GitLive.** Descartada: no cubre Desktop, target de entrega. Obligaría a dos
   implementaciones (SDK en móvil + REST en Desktop) para la misma funcionalidad. Aporta realtime y caché
   offline que esta feature no necesita.
2. **Backend propio (Ktor server) + base de datos relacional.** Descartada por ahora: control total pero
   coste desproporcionado para el alcance (hosting, auth propia, despliegue) en el marco del proyecto.
3. **Firestore REST en común (elegida).** Un solo camino para las tres plataformas, reaprovecha el stack
   oficial (Ktor + kotlinx.serialization), y el token de auth (GitLive en móvil, REST en Desktop tras la
   Fase 2) alimenta al mismo cliente.

## Consecuencias

**Positivas**

- Una sola implementación de acceso remoto, válida en Android, iOS y Desktop.
- Coherente con el stack oficial (RULES.md): `HttpClient` único inyectado, `kotlinx.serialization`,
  interceptors centralizados (el de **autenticación** inyectará el Firebase ID token al cerrar reglas).
- Sin realtime/offline remoto que mantener.

**Negativas / deuda**

- **Autorización en el servidor: resuelta el 24-jul-2026.** Durante el desarrollo las reglas de
  Firestore/Storage fueron **públicas** y solo con **datos ficticios** (desviación de SECURITY_MOBILE
  aceptada explícitamente por el owner, documentada en SPEC-0005). Ya están cerradas a
  `request.auth != null` + `ownerUid == request.auth.uid`, versionadas en el repositorio, y el **ID token**
  viaja en cada petición vía el cliente Ktor autorizado. Queda asumido que las URL de descarga de Storage
  llevan *download token* y no pasan por las reglas.
- **Dependencia de la Fase 2 de auth Desktop** para el `ownerUid` real y el ciclo end-to-end en Desktop.
- Se pierde la caché offline/realtime del SDK (no necesaria aquí).
- La API REST de Firestore obliga a mapear el formato de documento (typed values) en los DTOs.

## Referencias

- SPEC-0005 - Base de datos remota de análisis y consulta de pacientes.
- SPEC-0004 - Historial de análisis (persistencia local, Room).
- RULES.md §Networking, §Interceptors. SECURITY_MOBILE §Autenticación, §Networking. AGENTS.md §8, §11.
