package dev.lovelace.citovision.infrastructure.persistence.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entrada del conteo celular de un análisis. `position` preserva el orden original (SPEC-0004 RN-6).
 * `ON DELETE CASCADE` garantiza que borrar el análisis borra sus entradas (RN-7). `count` es el recuento del
 * tipo y `confidences` las confianzas por célula (0..1) serializadas como CSV, vacío en clases no celulares
 * (SPEC-0006 RF-2/RN-6). `level` es el `DetectionLevel` de la entrada; el `defaultValue` cubre las filas
 * migradas desde v4, que se generaron con umbral único de 0.25 y por tanto son todas normales
 * (ver `MIGRATION_4_5`).
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
    val count: Int,
    val confidences: String,
    @ColumnInfo(defaultValue = "STANDARD") val level: String = "STANDARD",
)
