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

/**
 * Migración v2 → v3 (SPEC-0006): añade la columna `priority` a `analyses` (prioridad de revisión). No
 * destructiva (RULES.md §Persistencia): solo añade la columna; los análisis existentes toman `'BAJA'`. El
 * `DEFAULT` coincide con `@ColumnInfo(defaultValue = "BAJA")` de [AnalysisEntity], que Room valida al abrir.
 */
val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "ALTER TABLE `analyses` ADD COLUMN `priority` TEXT NOT NULL DEFAULT 'BAJA'",
            )
        }
    }

/**
 * Migración v3 → v4 (SPEC-0006 RF-2): `cell_count_entries` pasa de `value TEXT` ("N (P%)") a `count INTEGER`
 * + `confidences TEXT` (confianzas por célula en CSV). No destructiva (RULES.md §Persistencia): se **recrea**
 * la tabla preservando los datos —el recuento se extrae como prefijo entero de `value` (`CAST`), y las
 * confianzas de análisis antiguos, que no existían, quedan vacías—. Es el patrón *recreate* estándar de
 * Room; la nueva tabla reproduce el esquema que Room espera para [CellCountEntity] (lo valida al abrir).
 */
val MIGRATION_3_4 =
    object : Migration(3, 4) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `cell_count_entries_new` " +
                    "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `analysisId` TEXT NOT NULL, " +
                    "`position` INTEGER NOT NULL, `name` TEXT NOT NULL, `count` INTEGER NOT NULL, " +
                    "`confidences` TEXT NOT NULL, FOREIGN KEY(`analysisId`) REFERENCES `analyses`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            connection.execSQL(
                "INSERT INTO `cell_count_entries_new` " +
                    "(`id`, `analysisId`, `position`, `name`, `count`, `confidences`) " +
                    "SELECT `id`, `analysisId`, `position`, `name`, CAST(`value` AS INTEGER), '' " +
                    "FROM `cell_count_entries`",
            )
            connection.execSQL("DROP TABLE `cell_count_entries`")
            connection.execSQL("ALTER TABLE `cell_count_entries_new` RENAME TO `cell_count_entries`")
            connection.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_cell_count_entries_analysisId` " +
                    "ON `cell_count_entries` (`analysisId`)",
            )
        }
    }

/**
 * Migración v4 → v5 (SPEC-0006 RN-2): añade la columna `level` a `cell_count_entries` (`DetectionLevel`). No
 * destructiva (RULES.md §Persistencia): solo añade la columna. Las entradas existentes toman `'STANDARD'`, y
 * es lo correcto —se generaron con el umbral único de 0.25, así que ninguna puede ser un hallazgo de baja
 * confianza—. El `DEFAULT` coincide con `@ColumnInfo(defaultValue = "STANDARD")` de [CellCountEntity], que
 * Room valida al abrir.
 */
val MIGRATION_4_5 =
    object : Migration(4, 5) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "ALTER TABLE `cell_count_entries` ADD COLUMN `level` TEXT NOT NULL DEFAULT 'STANDARD'",
            )
        }
    }
