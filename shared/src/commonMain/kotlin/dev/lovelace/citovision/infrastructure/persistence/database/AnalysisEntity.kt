package dev.lovelace.citovision.infrastructure.persistence.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Fila de un análisis. `performedAt` se guarda como epoch millis. `priority` guarda el nombre del enum
 * `Priority` (SPEC-0006); el `defaultValue` cubre las filas migradas desde v2 (ver `MIGRATION_2_3`). No sale
 * de Infrastructure.
 */
@Entity(tableName = "analyses")
data class AnalysisEntity(
    @PrimaryKey val id: String,
    val patient: String,
    val performedAt: Long,
    val summary: String,
    val imagePath: String?,
    @ColumnInfo(defaultValue = "BAJA") val priority: String = "BAJA",
)
