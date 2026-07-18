# ADR-0003 - Inferencia on-device con ONNX Runtime para la detección celular

## Estado

Aceptada — 2026-07-14

## Contexto

citoVision debe sustituir el **conteo celular mock** (SPEC-0004) por la **generación real** a partir de un
modelo de visión, y añadir una **prioridad de revisión** para cribado morfológico (SPEC-0006). Existe un
modelo **YOLO11s-seg** afinado sobre glóbulos blancos (dataset UNIVALI), exportado a `.onnx` (39 MB,
`imgsz` 640, 14 clases). Decisión del owner: el modelo se ejecuta **on-device** (no en servidor).

Condicionantes:

- Targets de entrega de esta fase: **Android** y **Desktop (JVM)**; **iOS** en la hoja de ruta.
- App **KMP**: la lógica compartida vive en `commonMain`; solo lo dependiente de plataforma usa
  `expect/actual` o ports + DI (AGENTS.md §2, §3).
- No hay binding Kotlin/KMP oficial para **ningún** runtime de inferencia: la ejecución del tensor es
  siempre código específico de plataforma.

## Decisión

Usar **ONNX Runtime** como motor de inferencia on-device, con el modelo en formato **`.onnx`**.

- **Android** y **Desktop (JVM)** comparten la **misma API Java** `ai.onnxruntime.*`
  (`com.microsoft.onnxruntime:onnxruntime-android` y `:onnxruntime`), reutilizable vía un **source set JVM
  intermedio** (`jvmShared`).
- **iOS (futuro)** → C/Obj-C API vía `cinterop` (EP CoreML cuando proceda); fuera del alcance de esta fase.
- Puerto `CellDetector` en `application`; **pre/post-procesado** (letterbox, NMS, decodificado YOLO, mapeo
  clase→célula, `PriorityCalculator`) en `commonMain` (Kotlin puro, testeable sin motor); solo la ejecución
  del tensor y el decodificado de imagen son de plataforma.
- Modelo empaquetado como **Compose Resources** (`files/citovision_yolo11s_seg_v1.onnx`): un único artefacto
  para las tres plataformas.
- El modelo es de **segmentación**, pero se usa **solo su rama de detección** para contar y priorizar; las
  máscaras se ignoran en esta fase.

## Alternativas consideradas

1. **TensorFlow Lite / LiteRT.** Descartada. Excelente en Android, pero soporte **pobre en Desktop/JVM**
   (target de entrega). Obligaría a **dos runtimes** y **dos exports** (`.tflite` + otro) con doble
   validación, y arrastraría esa duplicidad hacia iOS.
2. **Inferencia en servidor.** Descartada por decisión del owner: ejecución on-device (sin red para
   analizar, sin coste de GPU, los datos no salen del dispositivo).
3. **ONNX Runtime (elegida).** Un único `.onnx` y un único pre/post en `commonMain`; Android y Desktop
   comparten la misma API Java; iOS queda como trabajo **aditivo** (solo el `actual` de cinterop).

## Consecuencias

**Positivas**

- Un solo modelo y un solo pipeline compartido → menor superficie de mantenimiento y validación.
- Android + Desktop reutilizan el **runtime ONNX** (misma API Java `ai.onnxruntime`) vía source set JVM
  intermedio o duplicación mínima; el `ImageDecoder` es por plataforma (Android carece de `javax.imageio`).
- iOS se incorpora sin re-exportar modelo ni re-validar el pipeline (solo cinterop).
- Lógica determinista (pre/post + priorización) testeable en `commonTest` sin motor real.
- Inferencia on-device → la imagen no sale del dispositivo (privacidad).

**Negativas / deuda**

- En Android puro se renuncia a parte de la aceleración de TFLite (mitigable con XNNPACK/NNAPI EP).
- Nueva dependencia nativa por plataforma y **modelo de 39 MB empaquetado** → impacto en tamaño de binario
  (AGENTS.md §16) y decisión operativa **Git LFS vs commit directo** (el repo no usa LFS hoy).
- **iOS vía cinterop** sigue siendo trabajo no trivial cuando llegue (inherente a iOS, no a ONNX).
- El pre/post-procesado de YOLO (letterbox, escalado inverso, NMS) y los umbrales **conf 0.25 / IoU 0.45**
  (no embebidos en la metadata) son lógica propia a cubrir con tests y validar contra la referencia
  Ultralytics.

## Referencias

- RFC-0001 - Motor de inferencia on-device (fuente de esta decisión).
- SPEC-0006 - Análisis celular con modelo de detección on-device (ONNX) + priorización de cribado.
- SPEC-0004 (mock a sustituir), SPEC-0005 (sincronización remota del resultado).
- RULES.md §Concurrencia, §Inyección de Dependencias, §Manejo de errores. AGENTS.md §2, §3, §8, §16, §20.
