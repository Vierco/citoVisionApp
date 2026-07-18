package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.AnalysisImageStore
import dev.lovelace.citovision.application.ports.AnalysisRepository
import dev.lovelace.citovision.application.ports.CellDetector
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.analysis.CellCountBuilder
import dev.lovelace.citovision.domain.analysis.PriorityCalculator
import dev.lovelace.citovision.domain.analysis.SampleSummaryBuilder
import dev.lovelace.citovision.domain.entities.Analysis
import dev.lovelace.citovision.domain.entities.Detection
import dev.lovelace.citovision.domain.entities.SelectedImage
import kotlin.random.Random
import kotlin.time.Clock

/**
 * Ejecuta el modelo sobre la imagen (SPEC-0006) y, si hay al menos una **célula real** (RN-7) —incluidos los
 * posibles hallazgos de baja confianza, que también son células y por sí solos justifican guardar el
 * análisis—, deriva conteo, resumen y prioridad de revisión y persiste el análisis en local (la imagen como
 * fichero + la fila).
 * Sustituye al andamiaje mock temporal. Devuelve un [AnalysisOutcome] para que el ViewModel
 * gestione cada caso (guardado, sin células, fallo de inferencia o de guardado). Si la fila falla, borra la
 * imagen ya escrita para no dejarla huérfana.
 */
class AnalyzeSampleUseCase(
    private val cellDetector: CellDetector,
    private val analysisRepository: AnalysisRepository,
    private val analysisImageStore: AnalysisImageStore,
) {
    suspend operator fun invoke(
        image: SelectedImage,
        patientCode: String,
    ): AnalysisOutcome {
        val detections =
            when (val result = cellDetector.detect(image)) {
                is Result.Success -> result.value
                is Result.Failure -> return AnalysisOutcome.InferenceFailed
            }
        if (detections.none { it.cellClass.isCell }) return AnalysisOutcome.NoCellsDetected

        val id = generateId()
        val fileName = "$id.${image.mimeType.toFileExtension()}"
        return when (val saved = analysisImageStore.save(image.bytes, fileName)) {
            is Result.Success -> saveRow(id, patientCode, saved.value, detections)
            is Result.Failure -> AnalysisOutcome.SaveFailed
        }
    }

    private suspend fun saveRow(
        id: String,
        patientCode: String,
        imagePath: String,
        detections: List<Detection>,
    ): AnalysisOutcome {
        val priority = PriorityCalculator.priorityOf(detections)
        val analysis =
            Analysis(
                id = id,
                patient = patientCode,
                performedAt = Clock.System.now(),
                summary = SampleSummaryBuilder.build(detections, priority),
                imagePath = imagePath,
                cellCounts = CellCountBuilder.build(detections),
                priority = priority,
            )
        return when (analysisRepository.saveAnalysis(analysis)) {
            is Result.Success -> AnalysisOutcome.Saved(id)
            is Result.Failure -> {
                analysisImageStore.delete(imagePath) // no dejar la imagen huérfana
                AnalysisOutcome.SaveFailed
            }
        }
    }

    private fun generateId(): String =
        "${Clock.System.now().toEpochMilliseconds()}-${Random.nextInt(MIN_SUFFIX, MAX_SUFFIX)}"

    private fun String.toFileExtension(): String =
        when (this) {
            "image/png" -> "png"
            else -> "jpg"
        }

    private companion object {
        const val MIN_SUFFIX = 100_000
        const val MAX_SUFFIX = 999_999
    }
}
