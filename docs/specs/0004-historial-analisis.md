# SPEC-0004 - Historial de análisis (persistencia local)

## Estado

Approved

## Contexto

La pestaña **Historial** (`HistoryScreen`) tiene hoy dos estados maquetados —vacío y con datos— alternados
por un `Switch` temporal, y alimentados por una lista `tempData` codificada en el propio composable. Esta
spec sustituye ese mock por **persistencia local real** con **Room Multiplatform**, y añade el **borrado**
de un análisis mediante pulsación larga.

Cada análisis representa el resultado de procesar una imagen de muestra citológica (la carga de imagen es
SPEC-0003). La **generación real** de esos datos (resumen y conteo celular producidos por la IA) queda
**fuera de esta spec**. Para poder probar el ciclo completo sin la IA, el botón **"Iniciar Escáner"** de la
pantalla de Análisis persistirá, **de forma temporal**, un análisis con datos mock usando la imagen que el
usuario acaba de cargar.

> **Conflicto documental resuelto**: `RULES.md` designa Room Multiplatform como persistencia oficial,
> mientras `ARCHITECTURE.md` menciona SQLDelight en varios puntos y el catálogo declara `sqlDelight = "2.0.2"`
> (sin usar). El usuario ha decidido **Room Multiplatform** y ha **autorizado** actualizar `ARCHITECTURE.md`
> y retirar `sqlDelight` del catálogo como tarea asociada.

## Objetivo

Que el Historial lea los análisis almacenados en una base de datos local, muestre el estado vacío cuando no
haya ninguno, permita ver el detalle de cada uno (incluido el conteo celular) y borrarlo con pulsación larga.

## No objetivos

- **Generar** análisis reales: el resumen y el conteo celular los producirá la IA (spec posterior). Aquí se
  insertan datos mock.
- El campo **paciente** introducido por el usuario en la pantalla de Análisis (spec posterior; el mock genera
  un código).
- Sincronización con backend, exportación o compartición.
- Entidad `Patient` con relación (decisión: el paciente es **texto simple**).
- Edición de un análisis ya guardado.
- Cifrado en reposo de la base de datos y las imágenes (ver Seguridad y privacidad).

## Usuarios / actores

- **Usuario autenticado** (cuenta o invitado) que realiza análisis y consulta su historial.

## Requisitos funcionales

### Historial

- **RF-1**: El Historial muestra los análisis persistidos, **ordenados por fecha descendente** (el más
  reciente primero).
- **RF-2**: Si no hay ningún análisis, se muestra el **estado vacío** actual ("No hay análisis previos").
  El `Switch` temporal que hoy alterna el estado **se elimina**.
- **RF-3**: Cada card muestra: la **imagen** del análisis, el título fijo **"Análisis"**, la **fecha y hora**
  del análisis, el **paciente**, el **resumen**, y un botón "Ver Detalle".
- **RF-4**: "Ver Detalle" abre un diálogo con: título "Análisis", **paciente**, **fecha y hora**, y el
  **Conteo Celular** completo (lista de pares nombre → valor, de longitud variable).
- **RF-5**: Una **pulsación larga** sobre una card ofrece **eliminar** ese análisis; la acción pide
  **confirmación** antes de borrar (acción destructiva).
- **RF-6**: Al eliminar, el análisis desaparece de la lista sin recargar la pantalla (la lista es observable);
  si era el último, se muestra el estado vacío.

### Inserción temporal desde "Iniciar Escáner"

- **RF-7 (TEMPORAL)**: Con una imagen cargada (SPEC-0003), pulsar **"Iniciar Escáner"** persiste un nuevo
  análisis: guarda la **imagen** como fichero en almacenamiento privado y crea la fila con **datos mock**
  (paciente, resumen y conteo celular generados localmente) y `performedAt` = **instante actual**.
- **RF-8 (TEMPORAL)**: Pulsar "Iniciar Escáner" **repetidamente** añade un análisis nuevo cada vez. La imagen
  seleccionada **permanece** para permitirlo.
- **RF-9 (TEMPORAL)**: Los conteos celulares de los mocks deben tener **distinta longitud** entre análisis,
  para ejercitar el tamaño variable.

> RF-7/RF-8/RF-9 son **andamiaje** y se sustituirán por la invocación real a la IA en su spec. Deben quedar
> aisladas en un caso de uso propio, fácil de retirar.

## Requisitos no funcionales

- **RNF-1**: Persistencia con **Room Multiplatform** (`RULES.md`). `AppDatabase`, `@Entity` y `@Dao` viven en
  `commonMain`, dentro de `infrastructure/persistence`; solo la construcción del `RoomDatabase.Builder` es
  específica de plataforma (Android/Desktop/iOS), inyectada desde `composition/di`.
- **RNF-2**: Driver `BundledSQLiteDriver` y `setQueryCoroutineContext(Dispatchers.IO)` (skill `room-multiplatform`).
- **RNF-3**: Puerto `AnalysisRepository` en `application/ports`; implementación en `infrastructure/repositories`.
  **Ninguna `@Entity` de Room sale de Infrastructure**: se mapea siempre a la entidad de Domain.
- **RNF-4**: Puerto `AnalysisImageStore` en `application/ports` para escribir/borrar el fichero de imagen; la
  ruta base es específica de plataforma (`expect/actual`, como `dataStorePath()` de SPEC-0001B).
- **RNF-5**: La lista observable se expone como `Flow<List<Analysis>>`; las acciones puntuales (guardar, borrar)
  son `suspend` y devuelven `Result<_, AnalysisError>`. Sin excepciones como flujo de control.
- **RNF-6**: MVVM: `HistoryViewModel` + `HistoryUiState` + `HistoryUiEvent`; `HistoryScreen` stateless.
- **RNF-7**: Textos desde recursos (ES/EN); colores/spacing/radios desde tokens de `DESIGN.md`.
- **RNF-8**: Logging con Napier; **nunca** loguear la imagen, el resumen ni el código de paciente.
- **RNF-9**: Esquema versionado (`schemaDirectory`) para poder migrar en el futuro.
- **RNF-10**: El código respeta el skill `ktlint_kmp_code_style` (pasa `ktlintCheck` a la primera).

## Reglas de negocio

- **RN-1 (título)**: el título mostrado es la palabra fija **"Análisis"**; **no se persiste** (recurso de
  string, no dato del análisis).
- **RN-2 (fecha)**: se persiste el **instante** en que se realizó el análisis (fecha + hora). Card y diálogo
  muestran **fecha y hora**.
- **RN-3 (paciente)**: **siempre un código seudonimizado** (p. ej. `PAC-2023-001`). **Nunca** nombres reales ni
  documentos de identidad. Es una regla de negocio con efecto directo en el análisis de privacidad (ver Seguridad).
- **RN-4 (imagen)**: la imagen se guarda como **fichero en almacenamiento privado de la app**; en la base de
  datos solo se persiste su **ruta**. Cada análisis posee **su propio** fichero.
- **RN-5 (imagen ausente)**: si la ruta es nula o el fichero no existe, la card muestra un **placeholder gris**
  y se registra una **advertencia con Napier**: en el flujo real, un análisis sin imagen es una **anomalía**.
- **RN-6 (conteo celular)**: lista **ordenada** de pares `nombre → valor`, de **longitud variable** por
  análisis, en **tabla hija** relacionada. Valores como texto (`"7.500/µL"`, `"60%"`), pues llevan unidad embebida.
- **RN-7 (borrado)**: eliminar un análisis borra **también sus entradas de conteo celular** (cascada) y su
  **fichero de imagen**. Es irreversible → requiere confirmación.

## Estados de UI

`HistoryScreen`:

- **Loading**: mientras llega la primera emisión de la base de datos.
- **Vacío (empty)**: no hay análisis → maqueta actual (icono + "No hay análisis previos").
- **Con datos (success)**: lista de cards ordenada por fecha desc.
- **Detalle**: diálogo con paciente, fecha/hora y conteo celular (desplazable).
- **Confirmación de borrado**: diálogo destructivo (Cancelar / Eliminar).
- **Error**: fallo al leer o borrar → mensaje controlado, sin crash.

`AnalysisScreen` (añadidos temporales):

- **Guardando**: mientras se escribe el fichero y la fila (deshabilita "Iniciar Escáner").
- **Guardado**: confirmación breve; la imagen permanece seleccionada (RF-8).
- **Error**: fallo al guardar imagen o fila → mensaje; no se inserta nada.

## Contratos de datos

```kotlin
// domain/entities
data class Analysis(
    val id: String,
    val patient: String,            // código seudonimizado (RN-3)
    val performedAt: Instant,       // fecha + hora (kotlinx-datetime)
    val summary: String,            // resumen automático del análisis
    val imagePath: String?,         // ruta al fichero; null → placeholder + warning (RN-5)
    val cellCounts: List<CellCount>,
)

data class CellCount(
    val name: String,               // "Leucocitos"
    val value: String,              // "7.500/µL"
)

// domain/errors
sealed interface AnalysisError {
    data object NotFound : AnalysisError
    data object StorageFailure : AnalysisError
    data class Unknown(val cause: String?) : AnalysisError
}

// application/ports
interface AnalysisRepository {
    /** Análisis persistidos, ordenados por fecha descendente. Se re-emite ante cualquier cambio. */
    fun observeAnalyses(): Flow<List<Analysis>>

    /** Persiste un análisis nuevo junto a su conteo celular. */
    suspend fun saveAnalysis(analysis: Analysis): Result<Unit, AnalysisError>

    /** Borra el análisis, sus entradas de conteo (cascada) y su fichero de imagen. */
    suspend fun deleteAnalysis(id: String): Result<Unit, AnalysisError>
}

interface AnalysisImageStore {
    /** Escribe los bytes en almacenamiento privado y devuelve la ruta del fichero creado. */
    suspend fun save(bytes: ByteArray, fileName: String): Result<String, AnalysisError>

    /** Borra el fichero. Un fichero ya inexistente no es un error. */
    suspend fun delete(path: String): Result<Unit, AnalysisError>
}
```

Esquema relacional (Infrastructure, no sale de esa capa):

```text
analyses                        cell_count_entries
├─ id            (PK)   ◄───────┤ analysisId  (FK, ON DELETE CASCADE)
├─ patient       TEXT           ├─ id         (PK)
├─ performedAt   INTEGER        ├─ position   INTEGER   ← preserva el orden
├─ summary       TEXT           ├─ name       TEXT
└─ imagePath     TEXT NULL      └─ value      TEXT
```

## Errores

| Situación | Origen | Resultado UI |
|---|---|---|
| Fallo al leer la base de datos | Room | Estado de error controlado; no crash |
| Fallo al guardar la fila | Room | Mensaje; se borra el fichero ya escrito (no dejar huérfanos) |
| Fallo al escribir el fichero de imagen | FS | Mensaje; **no** se inserta la fila |
| Fallo al borrar | Room | Mensaje de error; la card permanece |
| Análisis inexistente al borrar | Room | `AnalysisError.NotFound` → se trata como éxito (idempotente) |
| Fichero de imagen ausente al mostrar | FS | Placeholder gris + warning Napier (RN-5) |
| Fallo al borrar el fichero de imagen | FS | Se registra con Napier; el borrado en BD **sí** se confirma |

## Casos borde

- Conteo celular **vacío** (0 entradas) → el diálogo muestra el apartado sin filas, no crashea.
- Conteo celular **largo** → el diálogo debe poder desplazarse.
- Pulsación larga y luego cancelar → no borra nada.
- Borrar el **último** análisis → transición limpia a estado vacío.
- Dos pulsaciones rápidas sobre "Eliminar" → idempotente (`NotFound` = éxito).
- Dos pulsaciones rápidas sobre "Iniciar Escáner" → ignorar mientras está guardando.
- `imagePath` apuntando a un fichero borrado externamente → placeholder + warning.

## Telemetría / analytics

Fuera de alcance.

## Seguridad y privacidad

- Los análisis contienen **datos médicos**. El identificador de paciente es un **código seudonimizado**
  (RN-3): no contiene nombres ni documentos de identidad, por lo que **no es PII directa**. Esto reduce
  sustancialmente la sensibilidad del dato almacenado.
- No se loguean imagen, resumen ni código de paciente (RNF-8).
- La imagen vive en **almacenamiento privado de la app** (sandbox), nunca en galería o almacenamiento compartido.
- El borrado elimina el fichero de imagen, para no dejar datos médicos huérfanos en disco (RN-7).
- **Cifrado en reposo — desviación documentada**: `SECURITY_MOBILE.md` §Persistencia exige *"cifrar únicamente
  la información sensible"*. En esta spec la base de datos y las imágenes **no se cifran** a nivel de aplicación.
  Justificación y alcance del riesgo:
  - **Android/iOS**: el sandbox de la app y el cifrado del sistema (File-Based Encryption / Data Protection)
    protegen los datos en reposo con el dispositivo bloqueado. Riesgo residual: dispositivo rooteado/jailbroken.
  - **Desktop (JVM)**: **no hay cifrado**. El fichero SQLite y las imágenes quedan en claro en el `home` del
    usuario, legibles por cualquier proceso de esa cuenta. **Este es el hueco real.**
  - Mitigación de partida: RN-3 (seudonimización) elimina la PII directa.
  - Se propone abordar el cifrado (y la gestión de clave por plataforma) en una **spec de seguridad** propia.

## Criterios de aceptación

- **CA-1**: Con la base vacía, el Historial muestra "No hay análisis previos" y el `Switch` ya no existe.
- **CA-2**: Cargar una imagen y pulsar "Iniciar Escáner" crea una card en Historial; pulsar de nuevo crea otra.
- **CA-3**: Cada card muestra la imagen cargada, "Análisis", fecha+hora, paciente y resumen.
- **CA-4**: "Ver Detalle" muestra el conteo celular completo, con el número de filas propio de ese análisis
  (distinto entre análisis).
- **CA-5**: Pulsación larga → confirmación → al aceptar, la card desaparece y **no reaparece al reiniciar la app**.
- **CA-6**: Borrando todos los análisis se vuelve al estado vacío.
- **CA-7**: Tras borrar, el fichero de imagen asociado ya no existe en disco.
- **CA-8**: Ninguna `@Entity`/`@Dao` de Room aparece fuera de `infrastructure`.
- **CA-9**: La app arranca en Android y Desktop con la base creada correctamente.

## Tests requeridos

- **commonTest**: `HistoryViewModel` con un `FakeAnalysisRepository` — estado vacío, estado con datos y orden,
  abrir/cerrar detalle, confirmar borrado (invoca al repositorio), cancelar borrado (no invoca), error al borrar.
  Mappers `@Entity` ↔ `Analysis` (orden de `cellCounts`, `imagePath` nulo). Caso de uso temporal de inserción mock.
- **desktopTest** (integración): `AnalysisDao` contra una base Room **real en fichero temporal** — insertar,
  observar, **borrado en cascada** de `cell_count_entries`, orden por fecha desc.
- **Grafo Koin**: `AppModulesGraphTest` resuelve repositorio, image store y use cases nuevos.
- **Manual**: pulsación larga + confirmación, e inserción repetida desde "Iniciar Escáner", en Android/Desktop.

## Dependencias

- **Room Multiplatform** (`androidx.room`, skill: `room = 2.8.4`, `sqlite = 2.6.2`) + plugin **KSP**.
  ⚠️ **Riesgo**: KSP debe casar con el **Kotlin efectivo 2.2.10** (no el `2.1.0` que declara el catálogo).
  Se verificará contra Maven antes de fijar versiones, como se hizo con Coil (donde 3.5.0 rompía el build).
- **kotlinx-datetime** — `Instant` y formateo de fecha/hora multiplataforma. **Dependencia nueva.**
- Koin (DI), Napier (logging), Coil 3 (imagen de la card), okio (escritura de ficheros) — ya integrados.
- **Tarea asociada autorizada**: actualizar `ARCHITECTURE.md` (SQLDelight → Room) y retirar `sqlDelight` del
  catálogo.

## Notas abiertas

Decisiones cerradas por el usuario: **Room Multiplatform**; **imagen = fichero + ruta en BD**; **conteo celular
= tabla hija relacional**; **paciente = texto simple y siempre código seudonimizado**; **título = literal no
persistido**; **sin siembra automática**: los datos entran pulsando "Iniciar Escáner" (andamiaje temporal);
**estado vacío = "No hay análisis previos"**, sin `Switch`; **autorizado** modificar `ARCHITECTURE.md` y el catálogo.

**Cifrado en reposo**: el usuario **acepta explícitamente la desviación documentada** de `SECURITY_MOBILE.md`
§Persistencia — no se cifra a nivel de aplicación en esta spec, apoyándose en el cifrado del sistema operativo
en Android/iOS y asumiendo el hueco conocido en Desktop, mitigado por la seudonimización (RN-3). Queda pendiente
abrir una **spec de seguridad** que aborde el cifrado y la gestión de clave por plataforma.
