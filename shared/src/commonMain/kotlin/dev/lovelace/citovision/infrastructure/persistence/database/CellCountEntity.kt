package dev.lovelace.citovision.infrastructure.persistence.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entrada del conteo celular de un análisis. `position` preserva el orden original (SPEC-0004 RN-6).
 * `ON DELETE CASCADE` garantiza que borrar el análisis borra sus entradas (RN-7).
 */
@Entity(
    tableName = "cell_count_entries",
    foreignKeys = [
        ForeignKey(
            entity = AnalysisEntity::class,
            parentColumns = ["id"],
            childColumns = ["analysisId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("analysisId")],
)
data class CellCountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val analysisId: String,
    val position: Int,
    val name: String,
    val value: String,
)
