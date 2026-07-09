package dev.lovelace.citovision.infrastructure.persistence.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

/** Base de datos local de citoVision (SPEC-0004). El esquema se versiona en `shared/schemas`. */
@Database(
    entities = [AnalysisEntity::class, CellCountEntity::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun analysisDao(): AnalysisDao
}

/**
 * El compilador de Room genera el `actual` de este objeto para cada plataforma; por eso no existe una
 * declaración `actual` escrita a mano y hay que suprimir el aviso.
 */
@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
