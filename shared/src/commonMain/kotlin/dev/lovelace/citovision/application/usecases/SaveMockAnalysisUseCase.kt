package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.AnalysisImageStore
import dev.lovelace.citovision.application.ports.AnalysisRepository
import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.core.result.fold
import dev.lovelace.citovision.domain.entities.Analysis
import dev.lovelace.citovision.domain.entities.CellCount
import dev.lovelace.citovision.domain.entities.SelectedImage
import dev.lovelace.citovision.domain.errors.AnalysisError
import kotlin.random.Random
import kotlin.time.Clock

/**
 * ⚠️ **ANDAMIAJE TEMPORAL** (SPEC-0004 RF-7/RF-8/RF-9).
 *
 * Persiste un análisis con datos mock a partir de la imagen cargada, para poder ejercitar el ciclo completo
 * (fichero + fila + historial + borrado) sin tener todavía la IA. Cuando exista el análisis real, esta clase
 * se sustituye por el caso de uso que invoque al modelo. **No añadir aquí lógica de negocio duradera.**
 *
 * Si la fila no se puede insertar, se borra el fichero ya escrito para no dejar imágenes huérfanas.
 */
class SaveMockAnalysisUseCase(
    private val analysisRepository: AnalysisRepository,
    private val analysisImageStore: AnalysisImageStore,
) {
    suspend operator fun invoke(image: SelectedImage): Result<Unit, AnalysisError> {
        val id = generateId()
        val fileName = "$id.${image.mimeType.toFileExtension()}"

        return analysisImageStore.save(image.bytes, fileName).fold(
            onSuccess = { imagePath -> saveRow(id, imagePath) },
            onFailure = { error -> Result.Failure(error) },
        )
    }

    private suspend fun saveRow(
        id: String,
        imagePath: String,
    ): Result<Unit, AnalysisError> {
        val analysis =
            Analysis(
                id = id,
                patient = mockPatientCode(),
                performedAt = Clock.System.now(),
                summary = MOCK_SUMMARIES.random(),
                imagePath = imagePath,
                cellCounts = mockCellCounts(),
            )
        return analysisRepository.saveAnalysis(analysis).fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { error ->
                analysisImageStore.delete(imagePath) // no dejar la imagen huérfana
                Result.Failure(error)
            },
        )
    }

    /** Único por instante + azar; suficiente para uso local. */
    private fun generateId(): String =
        "${Clock.System.now().toEpochMilliseconds()}-${Random.nextInt(MIN_SUFFIX, MAX_SUFFIX)}"

    private fun mockPatientCode(): String = "PAC-2026-${Random.nextInt(1, 1000).toString().padStart(3, '0')}"

    /** Longitud variable entre análisis, para ejercitar el tamaño dinámico del conteo (RF-9). */
    private fun mockCellCounts(): List<CellCount> =
        MOCK_CELL_COUNTS.shuffled().take(Random.nextInt(MIN_COUNTS, MAX_COUNTS + 1))

    private fun String.toFileExtension(): String =
        when (this) {
            "image/png" -> "png"
            else -> "jpg"
        }

    private companion object {
        const val MIN_SUFFIX = 100_000
        const val MAX_SUFFIX = 999_999
        const val MIN_COUNTS = 2
        const val MAX_COUNTS = 5

        val MOCK_SUMMARIES =
            listOf(
                "Detección de glóbulos blancos completada satisfactoriamente.",
                "Presencia de anomalías en el conteo de plaquetas detectada.",
                "Muestra dentro de los rangos de referencia esperados.",
                "Se observa una ligera desviación en la proporción de linfocitos.",
            )

        val MOCK_CELL_COUNTS =
            listOf(
                CellCount("Leucocitos", "7.500/µL"),
                CellCount("Neutrófilos", "60%"),
                CellCount("Linfocitos", "30%"),
                CellCount("Monocitos", "6%"),
                CellCount("Eosinófilos", "3%"),
                CellCount("Plaquetas", "220.000/µL"),
                CellCount("Hematíes", "4.8M/µL"),
            )
    }
}
