# RFC-0001 - Motor de inferencia on-device para el modelo de detección celular

## Estado

Approved — 2026-07-14 (promovido a **ADR-0003**)

> Un RFC propone una decisión **antes** de implementarla (AGENTS.md §8). Aprobado por el owner el
> 2026-07-14 y promovido a **ADR-0003**; habilita la implementación de **SPEC-0006**.

## Contexto

citoVision debe pasar de un **conteo celular mock** (SPEC-0004: "Iniciar Escáner" persiste datos ficticios)
a la **generación real** mediante un modelo de visión. Ya existe un modelo **YOLO11s-seg** afinado
(fine-tuning) sobre un dataset de glóbulos blancos (UNIVALI), exportado a `.onnx` (39 MB, `imgsz` 640). Es
un modelo de **segmentación**, pero para contar y priorizar (SPEC-0006) solo se usa su **rama de detección**;
la elección de runtime es independiente del cabezal del modelo.

Decisión de despliegue ya tomada por el owner: el modelo **no vivirá en un servidor**, se ejecuta
**on-device** empaquetando el fichero del modelo en la app.

Condicionantes:

- Targets de entrega de esta fase: **Android** y **Desktop (JVM)**. **iOS** está en la hoja de ruta y se
  incorporará más adelante; la decisión debe **no cerrarle la puerta** ni encarecerlo de forma innecesaria.
- La app es **KMP**: la lógica compartida debe vivir en `commonMain` (AGENTS.md §2, §3); solo lo que
  dependa realmente de plataforma usa `expect/actual` o ports + DI.
- Stack oficial (RULES.md): Koin, Coroutines/Flow, `Result` propio para operaciones que pueden fallar.
- No hay binding Kotlin/KMP oficial para **ningún** runtime de inferencia; la ejecución nativa del tensor
  es, en todos los casos, código específico de plataforma.

## Decisión

Usar **ONNX Runtime** como motor de inferencia on-device, con el modelo exportado a **`.onnx`** desde
Ultralytics.

- **Android** y **Desktop (JVM)** comparten la **misma API Java** `ai.onnxruntime.*`
  (`OrtEnvironment`, `OrtSession`, `OnnxTensor`):
  - Android → `com.microsoft.onnxruntime:onnxruntime-android`.
  - Desktop → `com.microsoft.onnxruntime:onnxruntime` (incluye libs nativas CPU para Windows/macOS/Linux).
- **iOS (futuro)** → C/Obj-C API de ONNX Runtime vía `cinterop` (con *execution provider* CoreML cuando
  proceda). Queda **fuera del alcance de esta fase**, pero el diseño lo contempla.

### Forma arquitectónica (contrato, no implementación)

Todo lo "inteligente" y determinista vive en `commonMain`; solo la ejecución del tensor y la decodificación
de píxeles son de plataforma:

| Pieza | Capa / source set | Responsabilidad |
|---|---|---|
| `CellDetector` (puerto) | `application/ports`, commonMain | Contrato `suspend detect(image): Result<List<Detection>, InferenceError>` |
| `YoloPreprocessor` | `commonMain` (Kotlin puro) | Letterbox/resize + normalización → `FloatArray` de entrada (NCHW) |
| `YoloPostprocessor` | `commonMain` (Kotlin puro) | Decodificado de salida YOLO, umbral de confianza, **NMS**, mapeo clase→tipo celular |
| `ImageDecoder` (puerto/expect) | plataforma | Bytes de imagen → buffer de píxeles RGB + dimensiones |
| `OnnxCellDetector` (actual) | plataforma | Carga el modelo, crea `OrtSession`, envuelve `FloatArray`→tensor, ejecuta, devuelve salida cruda |

> **Ventaja clave de ONNX aquí:** como Android y Desktop usan la **misma API Java** (`ai.onnxruntime`), el
> **runtime** (`OnnxRunner`/`OnnxCellDetector`) puede compartirse entre ambos, ya sea con un **source set
> intermedio JVM** (`jvmShared`) o duplicando un fichero pequeño idéntico. El **`ImageDecoder`, en cambio,
> NO es compartible**: Android carece de `javax.imageio`, así que usa `BitmapFactory` y Desktop `ImageIO` —
> es `actual` por plataforma. Solo iOS añadirá sus `actual` (cinterop) en el futuro.

### Empaquetado del modelo

El `.onnx` se empaqueta como recurso compartido (candidato: **Compose Resources**, `commonMain/composeResources/files/`,
leído con `Res.readBytes(...)`), de modo que exista **un único artefacto** para las tres plataformas. El
tamaño del modelo (orden de decenas de MB) y su impacto en el binario se evalúan en SPEC-0006.

### Ejecución

La inferencia es intensiva en CPU: se ejecuta **fuera del hilo principal** con un `CoroutineDispatcher`
**inyectado** (Koin), nunca en el hilo de UI (RULES.md §Concurrencia, AGENTS.md §16). El `OrtSession` se
crea una vez y se reutiliza (single en Koin).

## Alternativas consideradas

1. **TensorFlow Lite / LiteRT.** Descartada. Es excelente en **Android** (runtime nativo, delegados
   GPU/NNAPI), pero su soporte en **Desktop/JVM** —target de entrega de esta fase— es pobre e incómodo.
   Obligaría a **dos runtimes** (TFLite en Android + otro en Desktop) y **dos exports** del modelo
   (`.tflite` + otro), con la carga de **validar que ambos producen las mismas detecciones**. De cara a iOS
   arrastraría esa duplicidad de forma indefinida. Su ventaja (aceleración móvil premium) no compensa en un
   proyecto multiplataforma con Desktop.
2. **Modelo en servidor (inferencia remota).** Descartada por decisión del owner: el modelo se ejecuta
   on-device (sin dependencia de red para analizar, sin coste/hosting de GPU, datos no salen del dispositivo).
3. **ONNX Runtime (elegida).** **Un único `.onnx`** y **un único pre/post-procesado** en `commonMain` para
   las tres plataformas; Android y Desktop comparten además la **misma API Java** (código de runtime casi
   idéntico); iOS queda como trabajo **aditivo** (solo el `actual` de cinterop). Aceleración razonable vía
   *execution providers* (XNNPACK/NNAPI en Android, CoreML en iOS).

## Consecuencias

**Positivas**

- Un solo modelo y un solo pipeline compartido → menos superficie de mantenimiento y validación.
- Android + Desktop reutilizan el **runtime ONNX** (misma API Java) vía source set JVM intermedio o
  duplicación mínima. El `ImageDecoder` sí es por plataforma (Android sin `javax.imageio`).
- iOS se incorpora sin re-exportar modelo ni re-validar el pipeline (solo cinterop).
- Coherente con Clean Architecture: la lógica determinista (pre/post) es testeable en `commonTest` sin
  motor real.

**Negativas / deuda**

- En **Android puro** se renuncia a parte de la aceleración de TFLite (mitigable con XNNPACK/NNAPI EP).
- **Nueva dependencia nativa** por plataforma (tamaño de binario y libs nativas). A auditar (AGENTS.md §3,
  §16; Skill `gradle-conventions-kmp`).
- El **fichero de modelo empaquetado** aumenta el tamaño de la app (decenas de MB).
- **iOS vía cinterop** sigue siendo trabajo no trivial cuando llegue (inherente a iOS, no a ONNX).
- El **pre/post-procesado de YOLO** (letterbox, escalado inverso, NMS) es lógica propia a implementar y
  cubrir con tests; los parámetros exactos (tamaño de entrada, orden de canales, layout de salida) se fijan
  en SPEC-0006 a partir del export real.

## Referencias

- SPEC-0004 — Historial de análisis (deja la generación real por IA como "spec posterior").
- SPEC-0006 — Análisis celular con modelo ONNX on-device (feature que este RFC habilita).
- RULES.md §Concurrencia, §Inyección de Dependencias, §Manejo de errores. AGENTS.md §2, §3, §8, §16, §20.
- Ultralytics YOLO11 export (`format="onnx"`). ONNX Runtime (Java: `ai.onnxruntime`; iOS: C/Obj-C).
