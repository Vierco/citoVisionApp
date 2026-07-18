package dev.lovelace.citovision.application.ports

/**
 * Ejecuta el modelo ONNX sobre un tensor de entrada NCHW ya preprocesado (SPEC-0006, ADR-0003) y devuelve la
 * salida cruda de la rama de detección. Implementación **por plataforma** (`ai.onnxruntime` en Android y
 * Desktop; cinterop en iOS, futuro). Puede lanzar; el orquestador captura y traduce a `InferenceError`.
 */
interface OnnxRunner {
    suspend fun run(input: FloatArray): OnnxOutput
}
