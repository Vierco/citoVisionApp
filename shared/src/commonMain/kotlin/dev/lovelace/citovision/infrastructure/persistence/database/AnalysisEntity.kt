package dev.lovelace.citovision.infrastructure.persistence.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Fila de un análisis. `performedAt` se guarda como epoch millis. No sale de Infrastructure. */
@Entity(tableName = "analyses")
data class AnalysisEntity(
    @PrimaryKey val id: String,
    val patient: String,
    val performedAt: Long,
    val summary: String,
    val imagePath: String?,
)
