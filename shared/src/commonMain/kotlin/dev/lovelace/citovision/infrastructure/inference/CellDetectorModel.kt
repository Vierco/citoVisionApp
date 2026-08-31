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

/**
 * URI del mismo modelo dentro del paquete de la aplicación. La necesita iOS (ADR-0007), donde `ORTSession`
 * solo admite una **ruta de fichero**: resolverla evita copiar los 39 MB del modelo a disco. Vive junto a
 * [loadCellDetectorModel] para que el nombre del fichero tenga una única fuente.
 */
@OptIn(ExperimentalResourceApi::class)
fun cellDetectorModelUri(): String = Res.getUri(MODEL_PATH)
