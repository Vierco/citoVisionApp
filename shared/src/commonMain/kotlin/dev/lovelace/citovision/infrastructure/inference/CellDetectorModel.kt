package dev.lovelace.citovision.infrastructure.inference

import citovision.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Ruta del modelo ONNX empaquetado en Compose Resources (SPEC-0006, ADR-0003).  */
private const val MODEL_PATH = "files/citovision_yolo11s_seg_univali_stratified_v1.onnx"

/**
 * Carga los bytes del modelo ONNX desde los recursos compartidos. Un único artefacto para las tres
 * plataformas; lo consume el `OnnxRunner` de cada plataforma para crear su `OrtSession`.
 */
@OptIn(ExperimentalResourceApi::class)
suspend fun loadCellDetectorModel(): ByteArray = Res.readBytes(MODEL_PATH)
