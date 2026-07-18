package dev.lovelace.citovision.application.ports

import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.Detection
import dev.lovelace.citovision.domain.entities.SelectedImage
import dev.lovelace.citovision.domain.errors.InferenceError

/**
 * Puerto de detección celular on-device (SPEC-0006, ADR-0003). Su implementación orquesta el decodificado de
 * imagen (por plataforma) + preprocesado + ejecución ONNX + postprocesado, y devuelve las detecciones que
 * superan el umbral de confianza. Devuelve siempre [Result] (RULES.md §Manejo de errores).
 */
interface CellDetector {
    suspend fun detect(image: SelectedImage): Result<List<Detection>, InferenceError>
}
