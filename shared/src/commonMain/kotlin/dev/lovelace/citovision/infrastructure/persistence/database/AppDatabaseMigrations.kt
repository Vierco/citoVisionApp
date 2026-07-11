package dev.lovelace.citovision.infrastructure.persistence.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Migración v1 → v2 (SPEC-0005): añade la tabla `remote_upload_outbox` (transactional outbox).
 * No destructiva (RULES.md §Persistencia): solo crea la tabla nueva, no toca `analyses` ni
 * `cell_count_entries`. El `CREATE TABLE` reproduce exactamente el que genera Room para
 * [RemoteUploadOutboxEntity] (Room valida el esquema al abrir y falla si difiere).
 */
val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `remote_upload_outbox` " +
                    "(`analysisId` TEXT NOT NULL, `ownerUid` TEXT NOT NULL, `status` TEXT NOT NULL, " +
                    "`attempts` INTEGER NOT NULL, `lastError` TEXT, `createdAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`analysisId`))",
            )
        }
    }
