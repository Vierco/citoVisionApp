# SPEC-0003 - Carga de imagen de muestra

## Estado

Approved

## Contexto

La pestaña **Análisis** (`AnalysisScreen`) muestra hoy una zona de carga y un botón "Iniciar Escáner"
deshabilitado, ambos maquetados sin lógica (`onClick = { /* TODO */ }`). Esta spec da vida a la
**selección de imagen**: el usuario elige una imagen de muestra citológica desde las fuentes de su
dispositivo (galería/archivos en Android; selector de fichero en Desktop) y, una vez cargada, se
habilita el botón "Iniciar Escáner".

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

- **RF-1**: El botón "Seleccionar Imagen" abre el selector de imágenes de la plataforma (Android:
  selector del sistema — galería/archivos; Desktop: diálogo de selección de fichero).
- **RF-2**: El selector filtra por imágenes; formatos aceptados **JPG y PNG**.
- **RF-3**: Al elegir una imagen, la app lee sus bytes y su nombre y los retiene en el estado.
- **RF-4**: Tras una selección válida, se muestra una **previsualización** de la imagen (Coil 3) en la
  zona de carga, junto al nombre del fichero.
- **RF-5**: Con una imagen cargada, el botón "Iniciar Escáner" se **habilita** y adopta el color verde
  secundario (`secondary`); sin imagen permanece deshabilitado (estado actual).
- **RF-6**: El usuario puede **sustituir** la imagen (volver a seleccionar) o **quitarla** (vuelve al
  estado vacío y el botón se deshabilita).
- **RF-7**: Si el usuario cancela el selector, el estado no cambia.

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

// application/ports
interface ImagePicker {
    /** Abre el selector nativo. Devuelve null si el usuario cancela. */
    suspend fun pickImage(): Result<SelectedImage?, ImageError>
}
```

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

## Telemetría / analytics

Fuera de alcance.

## Seguridad y privacidad

- La imagen es un **dato médico potencialmente sensible**: no loguear su contenido ni bytes (RNF-5).
- No se persiste en disco ni se cachea fuera de memoria en esta fase.
- El envío a API (con su HTTPS, tamaño, cifrado en tránsito) se define en la spec de escáner; aquí solo
  se retiene en memoria.
- Android: usar el **selector del sistema** (Photo Picker), que **no requiere permiso de
  almacenamiento**. Aplicar mínimo privilegio (AGENTS.md §11).

## Criterios de aceptación

- **CA-1**: Pulsar "Seleccionar Imagen" abre el selector nativo en Android y Desktop.
- **CA-2**: Tras elegir un JPG/PNG válido, se ve la previsualización y el nombre del fichero.
- **CA-3**: Con imagen cargada, "Iniciar Escáner" queda habilitado y en verde secundario.
- **CA-4**: Cancelar el selector deja la pantalla como estaba.
- **CA-5**: Quitar la imagen vuelve al estado vacío y deshabilita el botón.
- **CA-6**: Un formato no soportado o una imagen > 10 MB muestran error y no habilitan el botón.
- **CA-7**: Ningún tipo de FileKit aparece en `commonMain` fuera de la implementación del puerto; ningún
  color/dimensión hardcodeada nueva fuera de tokens de `DESIGN.md`.

## Tests requeridos

- **commonTest**: `AnalysisViewModel` con un `FakeImagePicker` — selección válida (habilita botón,
  estado success), cancelación (sin cambios), error de formato/tamaño (estado error, botón deshabilitado),
  quitar imagen (vuelve a vacío). Validación de formato/tamaño (regla RN-1/RN-2).
- **Manual (Android/Desktop)**: apertura real del selector y previsualización.

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

Pendiente técnico (implementación):

- **iOS**: FileKit soporta iOS, pero la app aún no ejecuta ese flujo en iOS; la impl del puerto se deja
  preparada y se prueba en Android/Desktop.
