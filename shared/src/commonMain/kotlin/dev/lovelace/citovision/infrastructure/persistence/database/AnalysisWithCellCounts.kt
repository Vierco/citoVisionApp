package dev.lovelace.citovision.infrastructure.persistence.database

import androidx.room.Embedded
import androidx.room.Relation

/** Proyección de un análisis con su conteo celular asociado (relación 1:N). */
data class AnalysisWithCellCounts(
    @Embedded val analysis: AnalysisEntity,
    @Relation(parentColumn = "id", entityColumn = "analysisId")
    val cellCounts: List<CellCountEntity>,
)
