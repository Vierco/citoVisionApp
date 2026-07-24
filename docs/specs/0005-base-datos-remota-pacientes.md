# SPEC-0005 - Base de datos remota de análisis y consulta de pacientes

## Estado

Approved

## Contexto

Hoy los análisis se persisten **solo en local** con Room (SPEC-0004). La pestaña **Historial**
muestra esa base local. Esta spec añade una **base de datos remota** en la nube que:

1. Recibe una copia del análisis cuando se crea (además de guardarse en local).
2. Alimenta la pestaña **Pacientes**, donde el usuario consulta los análisis de un código de paciente.

La base remota está disponible **solo para usuarios con cuenta iniciada** (correo o Google). Un usuario
solo puede consultar sus propios pacientes; nunca los de otro usuario.

> **Decisión de plataforma (cloud):** se usa **Firestore + Firebase Storage a través de su API REST**
> con **Ktor Client** y una **única implementación en `commonMain`**, no el SDK GitLive. Motivo: GitLive
> **no tiene target Desktop/JVM** y Desktop es un target de entrega, por lo que el SDK obligaría a dos
> implementaciones (SDK móvil + REST Desktop). Esta feature no necesita realtime ni caché offline remota
> (consulta puntual + escritura puntual), así que REST-en-común es el camino único más simple. Esta
> decisión debe registrarse además como **ADR** (AGENTS.md §8).

> **Dependencia con la Fase 2 de auth (Desktop):** el alcance por usuario exige un `ownerUid` real,
> que proviene de la sesión autenticada. En Android/iOS ya existe (GitLive). En Desktop la auth real
> (Firebase vía REST/Identity Toolkit) llega en la **Fase 2**, inminente. Hasta entonces Desktop no
> tiene cuenta real y la feature se prueba end-to-end en Android/iOS; en Desktop se completa al aterrizar
> la Fase 2.

## Objetivo

- Al crear un análisis desde **"Iniciar Escáner"**, pedir el **código de paciente**, guardar la card en
  local (como hoy) y, si el usuario tiene cuenta, también en la base remota (metadatos en Firestore +
  imagen en Storage).
- En la pestaña **Pacientes**, permitir buscar por código: si existen análisis de ese paciente para el
  usuario, listarlos en orden cronológico con las mismas cards que el Historial; si no, informar; si no
  hay cuenta, informar de que la función requiere cuenta.

## No objetivos

- ~~**Reglas de seguridad definitivas de Firestore/Storage.**~~ → **cerradas el 24-jul-2026**
  (ver Seguridad y privacidad). Durante el desarrollo fueron públicas y solo se usaron datos ficticios.
- Sincronización bidireccional o resolución de conflictos local↔remoto. Son bases **independientes**.
- ~~Borrado remoto desde la app~~ → **movido a alcance (RF-10)** por decisión posterior del owner; el
  borrado local del Historial sigue siendo independiente (RF-9).
- Edición de un análisis ya guardado.
- Generación real de resumen/conteo por IA (sigue siendo mock temporal, SPEC-0004).
- Realtime, listeners o caché offline de la base remota.
- Paginación de resultados (se asume volumen bajo por paciente en esta fase).

## Usuarios / actores

- **Usuario con cuenta** (`SessionStatus.ACCOUNT`): escribe y consulta en remoto.
- **Usuario invitado** (`SessionStatus.GUEST`): solo local; al buscar en Pacientes ve el popup de
  "requiere cuenta".

## Requisitos funcionales

- **RF-1** Al pulsar **"Iniciar Escáner"** se muestra un diálogo que pide el **código de paciente**, con
  un texto de ayuda que indica que solo se admiten **números y el guion medio (`-`)**, con la forma
  mínima **`XX-YY`** (p. ej. `12-34`).
- **RF-2** El código se valida según RN-1; el botón de confirmar permanece deshabilitado mientras no sea
  válido.
- **RF-3** Al confirmar, se guarda la card en **local** (comportamiento actual de SPEC-0004, ahora con el
  código introducido en lugar del código mock autogenerado). La card aparece **de inmediato** en el
  Historial sin esperar a la red.
- **RF-3b** Si el usuario tiene cuenta, en la **misma operación** se registra una entrada en un **outbox
  local** (patrón transactional outbox) que representa la intención de sincronizar ese análisis con remoto
  (documento en Firestore + imagen en Storage). El guardado local y el alta en el outbox son **atómicos**.
- **RF-3c** Un **procesador del outbox** drena las entradas pendientes empujándolas a remoto. Ante un
  fallo (imagen o documento) **reintenta una vez automáticamente**; si el reintento también falla, la
  entrada queda **pendiente** en el outbox y se muestra un **popup** informando del error de sincronización
  con opción de **reintentar** (RN-8).
- **RF-4** La pestaña **Pacientes** presenta un campo de texto para el código de paciente. *(Ajuste tras
  feedback de la 1.0-beta: el campo deja de ser una búsqueda libre con botón y pasa a ser el **filtro**
  del listado de RF-4b; el botón "Buscar" desaparece. Ver RF-4b/RF-4c.)*
- **RF-4b** Debajo del campo se muestra el **listado de códigos de paciente del usuario**, obtenido del
  remoto y acotado a su `ownerUid` (RN-3), sin duplicados y en orden ascendente. Al escribir en el campo,
  el listado se **criba** en vivo y solo permanecen los códigos que contienen lo escrito. El listado se
  (re)carga al entrar en la pestaña y al volver a él con "Nueva búsqueda".
- **RF-4c** **Pulsar un código del listado** es la forma de consultar un paciente: abre sus análisis
  (RF-5). No existe forma de lanzar una consulta sobre un código inexistente. La acción "buscar" del
  teclado sobre el campo solo abre paciente si lo escrito identifica a **uno solo** (coincidencia exacta
  o criba con un único superviviente); en caso contrario no hace nada.
- **RF-5** Con cuenta iniciada y resultados: la pantalla muestra un texto superior **"Paciente: &lt;CÓDIGO&gt;"**,
  debajo las cards del paciente **en orden cronológico**, y un botón **"Nueva búsqueda"** que devuelve al
  estado de entrada de código.
- **RF-6** Con cuenta iniciada y sin resultados: se muestra un **popup** indicando que no se han encontrado
  resultados, con un **botón primario de cerrar**. *(Tras RF-4c este caso solo puede darse si los análisis
  del paciente se han borrado entre la carga del listado y la selección.)*
- **RF-7** Sin cuenta (invitado): la zona del listado informa **en línea** de que la función **requiere una
  cuenta de usuario**, sin listado ni consulta. *(Ajuste tras feedback de la 1.0-beta: antes era un popup
  al pulsar "Buscar", botón que ya no existe; el aviso pasa a mostrarse al entrar en la pestaña.)*
- **RF-8** Las cards remotas usan **el mismo componente y los mismos datos** que las locales, imagen
  incluida (cargada desde Storage con Coil).
- **RF-9** **Independencia de bases**: borrar en la local (Historial) **no** borra en remoto; borrar en
  remoto **no** borra en local.
- **RF-10** Una **pulsación larga** sobre una card en Pacientes ofrece **borrar el análisis del remoto**,
  con popup de confirmación. Solo afecta al documento de Firestore (RF-9); el fichero de imagen en Storage
  puede quedar huérfano (aceptable en esta fase). *(Añadido por decisión posterior del owner; inicialmente
  era No objetivo.)*

## Requisitos no funcionales

- Una **única instancia de `HttpClient`** (Ktor) configurada centralmente e inyectada por Koin, con
  engines por plataforma vía `expect/actual`, timeouts explícitos, `kotlinx.serialization`, logging
  Napier e interceptors centralizados (RULES.md §Networking, §Interceptors).
- El `RemoteAnalysisDataSource` devuelve siempre `Result` (RULES.md §Networking).
- Imágenes remotas cargadas **solo** con Coil, con estados de carga y error (RULES.md §Imágenes).
- Sin secretos en el repositorio: el `projectId`/config de Firebase se obtiene de la configuración de
  build por plataforma, no hardcodeado en fuentes.

## Reglas de negocio

- **RN-1** Código de paciente: compuesto **únicamente** por números y el guion medio, con estructura
  **mínima `XX-YY`** —al menos dos dígitos, guion, al menos dos dígitos—, admitiendo más dígitos y más
  segmentos (regex `^[0-9]{2,}(-[0-9]{2,})+$`). Válidos: `12-34`, `2026-001`, `12-34-56`. Inválidos:
  `1234`, `1-2`, `--`, vacío.
- **RN-2** El paciente es **siempre un código seudonimizado**, nunca nombres reales ni documentos de
  identidad (hereda RN de SPEC-0004).
- **RN-3** **Alcance por usuario**: cada análisis remoto lleva un `ownerUid`; una búsqueda solo devuelve
  análisis cuyo `ownerUid` coincide con el del usuario actual.
- **RN-4** El listado de Pacientes se ordena por `performedAt` **descendente** (del más reciente al más
  antiguo), igual que el Historial local.
- **RN-5** La escritura remota (alta en el outbox) **solo** ocurre para `SessionStatus.ACCOUNT`. Un
  invitado que escanea guarda **solo en local, de forma silenciosa** (sin outbox, sin popup); se preserva
  el comportamiento actual.
- **RN-6** El id del análisis local y el del documento remoto **coinciden** (mismo UUID), para
  correlacionarlos y hacer la escritura idempotente, aunque sus ciclos de vida sean independientes (RF-9).
- **RN-7** ~~Solo datos ficticios mientras las reglas remotas sean públicas.~~ **Obsoleta desde el
  24-jul-2026**: las reglas están cerradas y la autorización la impone el servidor (ver Seguridad y
  privacidad).
- **RN-8** Política del outbox: al fallar el empuje de una entrada, **un reintento automático**; si persiste,
  la entrada permanece pendiente y se notifica al usuario con un popup que permite **reintentar**
  manualmente. Las entradas pendientes son duraderas (sobreviven al cierre de la app) y se reintentan
  también al reabrirla. El empuje es **idempotente** (RN-6): reintentar no duplica documentos.

## Estados de UI

**Pantalla de Análisis (escaneo):**
- Diálogo de código: entrada + texto de ayuda; confirmar deshabilitado si el código es inválido.
- Guardando: indicador breve del guardado **local**; la imagen seleccionada se conserva (como SPEC-0004).
  La card local se confirma sin esperar a la red.
- **Popup de error de sincronización**: si el outbox no logra empujar tras el reintento automático, popup
  con el error y un botón **"Reintentar"** (además de cerrar). La card local permanece intacta.

**Pestaña Pacientes:**
- **Selección**: campo de filtro + listado de códigos del usuario (estado inicial y tras "Nueva búsqueda").
  La zona del listado resuelve por sí sola sus estados, todos excluyentes:
  - *Cargando el listado*: indicador de progreso.
  - *Sin cuenta*: aviso "requiere cuenta" (RF-7).
  - *Error al cargar el listado*: aviso con botón **"Reintentar"**.
  - *Sin pacientes*: aviso de que aún no hay pacientes con análisis en la nube.
  - *Sin coincidencias con el filtro*: aviso de que ningún código coincide.
- **Cargando**: consulta remota del paciente seleccionado en curso.
- **Resultados**: cabecera "Paciente: &lt;código&gt;" + lista de cards + "Nueva búsqueda".
- **Sin resultados**: popup con botón primario de cerrar → vuelve a Selección.
- **Error de red** al consultar un paciente: popup de error.

## Contratos de datos

**Firestore** — colección `analyses`, documento con id = id del análisis:

```
analyses/{analysisId}
  ownerUid:     String
  patientCode:  String
  performedAt:  Long        // epochMillis
  summary:      String
  imageUrl:     String?     // ruta/URL de descarga en Storage, null si no hay imagen
  cellCounts:   [ { name: String, value: String, position: Int } ]
```

**Firebase Storage** — imagen en `analyses/{ownerUid}/{analysisId}.<ext>`; su URL/ruta de descarga se
guarda en `imageUrl`.

**Outbox local** (Room, tabla nueva; implica **subir la versión** de la BD local con migración no
destructiva — RULES.md §Persistencia):

```
remote_upload_outbox
  analysisId:  String   // referencia al análisis local (fuente de la verdad para el payload)
  ownerUid:    String
  status:      String   // PENDING | FAILED
  attempts:    Int
  lastError:   String?
  createdAt:   Long
```

El procesador lee el análisis y la imagen desde local (por `analysisId`), construye el payload remoto,
sube a Storage, escribe el documento Firestore y **elimina** la entrada del outbox al completarse.

- **DTOs** (`@Serializable`, envueltos en la estructura de documento de la API REST de Firestore) viven en
  `infrastructure` *(nota: RULES.md los nombra "capa `data`"; el proyecto usa `infrastructure` desde
  SPEC-0001; se mantiene esa convención)*. Mappers explícitos DTO ↔ dominio (`Analysis`/`CellCount`, ya
  existentes).
- **Endpoints** centralizados: Firestore REST (`:runQuery` para la consulta por `ownerUid`+`patientCode`,
  `PATCH`/`POST` para la escritura) y Storage REST (upload/download). URL base por `projectId`.
- **Listado de códigos (RF-4b)**: `:runQuery` filtrando solo `ownerUid` y con **proyección** (`select`) del
  campo `patientCode`, de modo que la respuesta no arrastra resúmenes, conteos ni URLs. Los duplicados y el
  orden ascendente se resuelven en cliente. Se asume volumen bajo por usuario, coherente con el
  no-objetivo de paginación.

## Errores

`RemoteAnalysisError` (application/domain):
- `Network` — fallo de conectividad/timeout.
- `Unauthorized` — sin sesión válida o token rechazado (relevante al cerrar reglas).
- `NotFound` — la consulta no devuelve documentos (se traduce a estado "sin resultados", no a error visible).
- `Serialization` — payload inesperado.
- `Unknown(cause)`.

Las excepciones de Ktor se capturan en el DataSource y se transforman en estos errores (RULES.md
§Manejo de errores). Nunca se loguean tokens ni payloads sensibles (SECURITY_MOBILE §Logging).

## Casos borde

- **Invitado escanea**: se guarda solo local, de forma silenciosa, sin outbox ni popup (RN-5).
- **Escritura parcial remota** (sube la imagen pero falla el documento, o al revés): la entrada del outbox
  se mantiene y se reintenta; al ser idempotente (mismo `analysisId`/mismo path de Storage), el reintento
  no duplica. Persistiendo el fallo → popup de reintento (RN-8).
- **Sin conexión al escanear**: la card local se guarda igual; la entrada queda PENDING en el outbox y se
  empujará al recuperar red o al reabrir la app; si el intento inmediato falla, popup de reintento.
- **App cerrada a mitad de sincronización**: la entrada duradera del outbox se reintenta al reabrir.
- **Código inválido** (vacío, solo guiones `--`, o sin la forma mínima `XX-YY` como `1234` o `1-2`):
  rechazado por RN-1 en el diálogo del escáner (RF-2); el botón de confirmar permanece deshabilitado. En
  la pestaña Pacientes el campo es un **filtro** (RF-4), así que no valida: un texto que no case con
  ningún código simplemente vacía el listado cribado.
- **Imagen ausente**: `imageUrl` null; la card remota muestra el placeholder gris (como la local).
- **Mismo código buscado dos veces**: consulta idempotente, sin efectos secundarios.

## Telemetría / analytics

Fuera de alcance en esta fase.

## Seguridad y privacidad

- **Autorización en el servidor (cerrada el 24-jul-2026).** Las reglas exigen `request.auth != null` y que
  el dueño coincida con el usuario del token: en `analyses`, contra el campo `ownerUid` del documento
  (read/create/update/delete); en Storage, contra el `{ownerUid}` del path
  `analyses/{ownerUid}/{analysisId}`. Todo lo demás se deniega por defecto. Las reglas se versionan en el
  repositorio (`firestore.rules`, `storage.rules`, `firebase.json`).
- **ID token en cada petición.** El cliente Ktor autorizado adjunta `Authorization: Bearer <ID token>` a
  Firestore y Storage mediante un plugin centralizado (RULES.md §Interceptors), no petición a petición; el
  cliente sin credenciales queda reservado para Identity Toolkit, donde el token aún no existe. Lo aporta
  el puerto `AuthTokenProvider`: GitLive en Android y la sesión en memoria de Identity Toolkit en Desktop,
  renovada contra Secure Token antes de caducar (ADR-0002).
- **Desviación histórica (cerrada).** Mientras las reglas fueron públicas, "solo cuenta iniciada" y "solo
  tus pacientes" existían únicamente en la UI; la mitigación acordada fue usar solo datos ficticios (RN-7).
- **Desviación vigente asumida:** la URL de descarga de Storage incorpora un *download token* y da acceso
  al objeto **sin evaluar las reglas**. Esa URL solo viaja dentro del documento Firestore del análisis, que
  sí está protegido, pero quien la obtenga puede ver la imagen. Cerrarlo exigiría firmar cada descarga y
  tocar el ImageLoader de Coil en las tres plataformas; se asume para el MVP.
- Paciente seudonimizado (RN-2).
- HTTPS obligatorio; timeouts explícitos; sin certificados autofirmados (SECURITY_MOBILE §Networking).
- Sin secretos en el repositorio (config de Firebase por build).

## Criterios de aceptación

- Escanear con cuenta pidiendo código → aparece card en Historial (local) de inmediato **y**, al drenar el
  outbox, documento en Firestore + imagen en Storage con `ownerUid` correcto.
- Escanear sin red → card local presente, entrada PENDING en el outbox y popup de reintento; al recuperar
  red y reintentar, se sincroniza **sin duplicar** (idempotencia).
- Entrar en Pacientes con cuenta → el listado muestra los códigos propios, sin duplicados y ordenados.
- Escribir en el campo → el listado se criba y solo quedan los códigos que contienen lo escrito; si no
  queda ninguno, se informa.
- Pulsar un código del listado → cabecera "Paciente: &lt;código&gt;", cards cronológicas y "Nueva búsqueda".
- Un paciente cuyos análisis se han borrado entre medias → popup de "sin resultados" con botón cerrar.
- Entrar en Pacientes como invitado → aviso en línea de "requiere cuenta", sin listado.
- Sin red al cargar el listado → aviso de error con "Reintentar" que vuelve a intentarlo.
- Borrar una card en Historial → el documento remoto **permanece**; y viceversa (RF-9).
- Consultar un análisis de otro `ownerUid` → lo deniega el servidor, no solo la UI.
- Card remota visualmente idéntica a la local, imagen incluida.
- Un usuario **no** ve pacientes de otro `ownerUid` (verificable en cliente ahora; en servidor al cerrar
  reglas).

## Tests requeridos

- **commonTest**: validación de RN-1; mappers DTO ↔ dominio; `RemoteAnalysisDataSource` con Ktor
  `MockEngine` (respuestas de `:runQuery`, vacío, error); **procesador del outbox** (reintento automático,
  idempotencia al reintentar, marca PENDING/eliminación al éxito, orden de subida imagen→documento); use
  cases (encolar en outbox, buscar por paciente, filtro por `ownerUid`, **listar códigos de paciente**);
  `PatientsViewModel` (estados selección/cargando/resultados/sin-resultados/sin-cuenta, **criba del
  listado y selección de un código**); ampliar el flujo de escaneo (código + guardado local + alta en
  outbox con cuenta, invitado solo local + popup de reintento en fallo).
- **DAO del outbox** (test de persistencia, estilo `AnalysisDaoTest` de SPEC-0004): alta atómica junto al
  análisis, consulta de pendientes, borrado al completar.
- **Grafo Koin**: el nuevo módulo de networking + DataSource + use cases + ViewModel resuelven.

## Dependencias

- **Ktor Client** (stack oficial) + engines por plataforma; `kotlinx.serialization`.
- **Firebase**: proyecto con **Firestore** y **Storage** habilitados (además de Auth ya integrado).
- **SPEC-0003** (imagen a subir) y **SPEC-0004** (card, dominio `Analysis`/`CellCount`, flujo de escáner).
- **Fase 2 de auth Desktop** para el ciclo end-to-end en Desktop (`ownerUid` real).
- **ADR** de la decisión cloud (pendiente de crear).

## Notas abiertas

*(Resueltas por el usuario; se conservan como registro de decisión.)*

1. **Orden cronológico** en Pacientes → **descendente** (RN-4).
2. **Invitado que escanea** → **guardado local silencioso** (RN-5).
3. **Escritura parcial remota** → **reintento automático** y, si persiste, **popup de error con reintento
   manual** (RN-8).
4. **Formato del código** → solo números y guion medio, con estructura mínima **`XX-YY`**
   (`^[0-9]{2,}(-[0-9]{2,})+$`, RN-1).
5. **Escritura remota** → **patrón outbox** (asíncrona, no bloqueante); fallo gestionado como el punto 3
   (RF-3b/3c, RN-8).
