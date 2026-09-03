# SPEC-0003 - Carga de imagen de muestra

## Estado

Approved

## Contexto

La pestaña **Análisis** (`AnalysisScreen`) muestra hoy una zona de carga y un botón "Iniciar Escáner"
deshabilitado, ambos maquetados sin lógica (`onClick = { /* TODO */ }`). Esta spec da vida a la
**selección de imagen**: el usuario elige una imagen de muestra citológica desde las fuentes de su
dispositivo y, una vez cargada, se habilita el botón "Iniciar Escáner".

En **móvil, fototeca y explorador de ficheros son dos selectores nativos distintos y ninguno ve el
contenido del otro**: el selector de fotos solo lista lo indexado como fototeca, y el de documentos
llega a carpetas, descargas y proveedores externos, pero no a la fototeca. Como la app no puede adivinar
dónde guarda cada laboratorio sus muestras, **la fuente la elige el usuario en Ajustes**. En Desktop la
distinción no existe: un único diálogo de fichero lo abarca todo.

La selección se apoya en **FileKit** (`io.github.vinceglb`), librería KMP para Compose Multiplatform
(Android/iOS/Desktop). La previsualización de la imagen elegida usa **Coil 3** (librería oficial de
imágenes según `RULES.md`). Sigue el patrón puerto (`application/ports`) + inyección desde
`composition/di`, coherente con la feature de autenticación.

El **envío de la imagen a una API** y la **acción de escanear** quedan **fuera de esta spec** (se
abordarán en specs posteriores); aquí solo se selecciona la imagen, se retiene en memoria y se habilita
el botón.

## Objetivo

Desde la pestaña Análisis, el usuario puede seleccionar una imagen de muestra; la app la previsualiza y
habilita el botón "Iniciar Escáner" (verde secundario), dejando la imagen lista para su uso posterior.

## No objetivos

- Ejecutar el escáner / análisis (solo se habilita el botón; el `onClick` real es de otra spec).
- Enviar la imagen a ninguna API o backend (se retiene en memoria para uso futuro).
- Persistir la imagen en disco o base de datos.
- Captura directa con **cámara**: descartada. Las muestras se toman de **microscopios** y se cargan como
  fichero; no tiene sentido usar la cámara del dispositivo. Solo selector del sistema.
- Validación clínica del contenido de la imagen (que sea una muestra citológica válida).
- Soporte de formato **DICOM** (el string de maqueta lo menciona; ver Casos borde / Notas abiertas).

## Usuarios / actores

- **Usuario autenticado** (cuenta o invitado) que quiere analizar una muestra.

## Requisitos funcionales

- **RF-1**: El botón "Seleccionar Imagen" abre el selector nativo de la plataforma correspondiente a la
  fuente elegida (RF-8): en **Android**, el selector de fotos del sistema o el explorador de documentos;
  en **iOS**, la fototeca o la app Archivos; en **Desktop**, el diálogo de selección de fichero, único
  para ambas fuentes.
- **RF-2**: El selector filtra por imágenes; formatos aceptados **JPG y PNG**.
- **RF-3**: Al elegir una imagen, la app lee sus bytes y su nombre y los retiene en el estado.
- **RF-4**: Tras una selección válida, se muestra una **previsualización** de la imagen (Coil 3) en la
  zona de carga, junto al nombre del fichero.
- **RF-5**: Con una imagen cargada, el botón "Iniciar Escáner" se **habilita** y adopta el color verde
  secundario (`secondary`); sin imagen permanece deshabilitado (estado actual).
- **RF-6**: El usuario puede **sustituir** la imagen (volver a seleccionar) o **quitarla** (vuelve al
  estado vacío y el botón se deshabilita).
- **RF-7**: Si el usuario cancela el selector, el estado no cambia.
- **RF-8**: En Ajustes hay una sección **"Origen de las imágenes"** con dos opciones excluyentes,
  *Galería de fotos* y *Archivos*, que decide qué selector abre RF-1. La elección **persiste** entre
  ejecuciones. La sección **no se muestra en Desktop**, donde no hay nada que elegir.
- **RF-9**: La **primera vez** que se pulsa "Seleccionar Imagen" se muestra, en lugar del selector, un
  aviso que explica que las muestras se buscan en la fototeca y que el origen se cambia en Ajustes. Se
  muestra **una sola vez**; a partir de ahí el botón abre el selector directamente.
  - Donde la plataforma lo permite (**Android y Desktop**), cerrar el aviso **abre el selector en la
    misma acción**: un solo toque.
  - Donde no (**iOS**, ver Casos borde), el aviso indica que hay que **volver a pulsar** el botón, y no
    encadena nada.

## Requisitos no funcionales

- **RNF-1**: Puerto `ImagePicker` en `application/ports` (commonMain); implementación basada en FileKit.
  La UI/ViewModel no dependen directamente de FileKit.
- **RNF-2**: La selección es `suspend` y devuelve `Result<SelectedImage?, ImageError>` (`null` =
  cancelado); sin excepciones como flujo de control (`RULES.md`).
- **RNF-3**: Estado gestionado por `AnalysisViewModel` (`AnalysisUiState`); `AnalysisScreen` stateless
  con `onEvent`, coherente con `LoginScreen`/`SettingsScreen`.
- **RNF-4**: Textos desde recursos (ES/EN); colores/spacing/radios desde tokens de `DESIGN.md` (sin
  hardcodear; se corrige la maqueta actual que usa hex directos en lo que toque esta feature).
- **RNF-5**: Logging con Napier; **nunca** loguear los bytes ni el contenido de la imagen (dato médico
  sensible, ver Seguridad).
- **RNF-6**: La imagen se mantiene solo en memoria durante la sesión de la pantalla.

## Reglas de negocio

- **RN-1 (formatos)**: se aceptan `image/jpeg` y `image/png`. Otros tipos se rechazan con error de UI.
- **RN-2 (tamaño máximo)**: se rechaza una imagen mayor de **10 MB** (`ImageError.TooLarge`) para
  proteger memoria y el envío futuro.
- **RN-3 (una imagen a la vez)**: solo hay una imagen seleccionada; una nueva selección reemplaza la
  anterior.
- **RN-4 (origen por defecto)**: *Galería de fotos*, que es el comportamiento que tenía la app antes de
  existir la preferencia. Ante un valor persistido ilegible o desconocido se vuelve a él sin error.
- **RN-5 (filtro del explorador)**: cuando la fuente es *Archivos*, el selector se restringe a las
  extensiones admitidas por RN-1 (`jpg`, `jpeg`, `png`), para que el usuario no pueda elegir algo que
  después se rechazaría.

## Estados de UI

Zona de carga de `AnalysisScreen`:

- **Vacío (empty)**: maqueta actual (icono + "Seleccionar Imagen" + formatos soportados). Botón escáner
  deshabilitado.
- **Seleccionando (loading)**: mientras se abre el selector y se leen los bytes (deshabilita el botón de
  selección para evitar doble apertura).
- **Cargada (success)**: previsualización + nombre + opción de quitar/cambiar. Botón escáner habilitado
  (verde secundario).
- **Error**: mensaje (formato no soportado, demasiado grande, error de lectura). Se mantiene el estado
  previo (vacío o la imagen anterior).
- **Aviso de origen (RF-9)**: diálogo informativo con un único botón de cierre, sobre el estado *Vacío*.
  Solo aparece la primera vez.

En Ajustes, la sección **"Origen de las imágenes"** (RF-8) reutiliza el mismo patrón de opciones con
radio que la sección Tema.

## Contratos de datos

```kotlin
// domain/entities
data class SelectedImage(
    val bytes: ByteArray,   // contenido en memoria, para uso/envío posterior
    val fileName: String,
    val mimeType: String,   // "image/jpeg" | "image/png"
    val sizeBytes: Long,
)

// domain/errors
sealed interface ImageError {
    data object UnsupportedFormat : ImageError
    data object TooLarge : ImageError
    data object ReadFailed : ImageError
    data class Unknown(val cause: String?) : ImageError
}

// domain/settings
enum class ImageSourcePreference { GALLERY, FILES }

// application/ports
interface ImagePicker {
    /** `true` donde fototeca y ficheros son selectores distintos (Android, iOS); `false` en Desktop. */
    val hasDistinctSources: Boolean

    /** `true` si el selector puede abrirse en la misma acción que cierra un diálogo; `false` en iOS. */
    val canOpenPickerAfterDialog: Boolean

    /** Abre el selector nativo. Devuelve null si el usuario cancela. */
    suspend fun pickImage(source: ImageSourcePreference): Result<SelectedImage?, ImageError>
}

interface ImageSourceRepository {
    fun imageSource(): Flow<ImageSourcePreference>
    suspend fun setImageSource(preference: ImageSourcePreference)
    /** `true` mientras el aviso de RF-9 siga pendiente de mostrarse. */
    fun isSourceNoticePending(): Flow<Boolean>
    suspend fun markSourceNoticeShown()
}
```

Las dos capacidades del puerto describen **diferencias reales de plataforma**, no decisiones de
producto, así que se resuelven con `expect/actual` y llegan a Presentation como use cases: el ViewModel
no conoce la plataforma y las dos ramas quedan cubiertas por tests.

La preferencia y la marca del aviso son datos **no sensibles** y viven en DataStore Preferences, junto
al tema y al último código de paciente.

*(Nota: `data class` con `ByteArray` requiere `equals`/`hashCode` a medida o marcar el campo como no
estructural; se resolverá en implementación.)*

## Errores

| Situación | Origen | Resultado UI |
|---|---|---|
| Usuario cancela el selector | plataforma | Sin cambios (RF-7) |
| Formato no soportado (no JPG/PNG) | validación | `ImageError.UnsupportedFormat` → mensaje |
| Imagen > 10 MB | validación | `ImageError.TooLarge` → mensaje |
| Fallo al leer los bytes | plataforma | `ImageError.ReadFailed` → mensaje |
| Error inesperado | plataforma | `ImageError.Unknown` → mensaje genérico |

## Casos borde

- Doble pulsación en "Seleccionar Imagen" → ignorar mientras está en estado *Seleccionando*.
- Fichero con extensión de imagen pero contenido corrupto → `ReadFailed` o falla el render de Coil
  (mostrar error, no crash).
- DICOM (`.dcm`): fuera de alcance en esta spec pese a aparecer en el string de maqueta; se actualizará
  el texto de "formatos soportados" a JPG/PNG (ver Notas abiertas).
- Imagen muy grande en píxeles (no en bytes) → Coil la decodifica con submuestreo; vigilar memoria.
- **iOS: el selector no puede abrirse justo después de cerrar un diálogo** (motivo de la segunda rama de
  RF-9). Los diálogos de Compose no se dibujan en la escena: viven en una `UIWindow` aparte a nivel
  alerta, y FileKit resuelve el controlador sobre el que presentar recorriendo la *key window*. Pedirle
  el selector mientras esa ventana se desmonta lo deja presentado sobre algo que desaparece, así que no
  llega a verse; y como presenta con `topMostViewController()?.present(...)`, si no hay controlador **no
  hace nada y su corrutina no se reanuda jamás**, dejando el botón deshabilitado para siempre. FileKit no
  permite indicar el controlador, así que la única secuencia segura es esperar a otra pulsación.
- El usuario elige *Archivos* pero sus muestras están en la fototeca (o al revés) → el selector abrirá
  una vista sin sus imágenes. Es el caso que RF-9 existe para prevenir, y tiene arreglo en Ajustes.

## Telemetría / analytics

Fuera de alcance.

## Seguridad y privacidad

- La imagen es un **dato médico potencialmente sensible**: no loguear su contenido ni bytes (RNF-5).
- No se persiste en disco ni se cachea fuera de memoria en esta fase.
- El envío a API (con su HTTPS, tamaño, cifrado en tránsito) se define en la spec de escáner; aquí solo
  se retiene en memoria.
- **Ninguna de las dos fuentes exige permisos de almacenamiento**, y ese es el criterio que las
  gobierna (mínimo privilegio, AGENTS.md §11): en Android, el **selector de fotos del sistema** y el
  **explorador de documentos** (`ACTION_OPEN_DOCUMENT`, SAF) conceden acceso por URI a lo que el usuario
  elige, sin `READ_EXTERNAL_STORAGE`; en iOS, `PHPickerViewController` y `UIDocumentPickerViewController`
  se comportan igual. Si en el futuro se cambiara de mecanismo, esta propiedad debe conservarse.

## Criterios de aceptación

- **CA-1**: Pulsar "Seleccionar Imagen" abre el selector nativo en Android, iOS y Desktop.
- **CA-2**: Tras elegir un JPG/PNG válido, se ve la previsualización y el nombre del fichero.
- **CA-3**: Con imagen cargada, "Iniciar Escáner" queda habilitado y en verde secundario.
- **CA-4**: Cancelar el selector deja la pantalla como estaba.
- **CA-5**: Quitar la imagen vuelve al estado vacío y deshabilita el botón.
- **CA-6**: Un formato no soportado o una imagen > 10 MB muestran error y no habilitan el botón.
- **CA-7**: Ningún tipo de FileKit aparece en `commonMain` fuera de la implementación del puerto; ningún
  color/dimensión hardcodeada nueva fuera de tokens de `DESIGN.md`.
- **CA-8**: Cambiar el origen en Ajustes cambia el selector que abre RF-1, y la elección **sobrevive a
  cerrar y abrir la app**. En Desktop la sección no aparece.
- **CA-9**: El aviso de RF-9 se muestra **solo la primera vez**. En Android y Desktop, aceptarlo abre el
  selector; en iOS, aceptarlo deja el botón **habilitado** para volver a pulsarlo (nunca bloqueado).

## Tests requeridos

- **commonTest**: `AnalysisViewModel` con un `FakeImagePicker` — selección válida (habilita botón,
  estado success), cancelación (sin cambios), error de formato/tamaño (estado error, botón deshabilitado),
  quitar imagen (vuelve a vacío). Validación de formato/tamaño (regla RN-1/RN-2).
- **commonTest (RF-8/RF-9)**: que el aviso se muestre en lugar de abrir el selector la primera vez, y las
  **dos ramas de plataforma** al cerrarlo —encadena / espera otra pulsación—, forzando desde el doble del
  puerto la capacidad `canOpenPickerAfterDialog`. La preferencia, en `SettingsViewModel`.
- **Manual (Android/iOS/Desktop)**: apertura real del selector con cada origen y previsualización.

## Dependencias

- **FileKit** (`io.github.vinceglb`) — selección de fichero/imagen KMP. **Dependencia nueva a añadir al
  catálogo** (versión a fijar en implementación; verificar targets Android/iOS/Desktop).
- **Coil 3** (`io.coil-kt.coil3`) — previsualización (oficial en `RULES.md`). **Dependencia nueva a
  añadir al catálogo.**
- Koin (DI), Napier (logging) — ya integrados.

## Notas abiertas

Decisiones cerradas por el usuario al aprobar:

- **Cámara**: descartada (muestras de microscopio → solo selector del sistema).
- **DICOM**: fuera de alcance; se limita a **JPG/PNG** y se actualiza el string `analysis_supported_formats`.
- **Tamaño máximo (RN-2)**: **10 MB** confirmado.

Añadido al ampliar la spec (2-sep-2026), tras dar soporte a iOS:

- **Origen de las imágenes (RF-8, RF-9, RN-4, RN-5)**: la spec original daba por hecho un único selector
  por plataforma. Al ejecutarse en iOS se vio que fototeca y ficheros son mundos separados y que las
  muestras exportadas de un microscopio suelen vivir como ficheros, invisibles para el selector de fotos.
- **iOS**: implementado y verificado. Queda superada la nota previa, que decía que la implementación del
  puerto se dejaba preparada sin ejecutarse en esa plataforma.

Pendiente técnico (implementación):

- **DESIGN.md** no define token de *scrim*; hoy no hace falta, pero quedó anotado al evaluar un aviso con
  velo que finalmente se descartó.
