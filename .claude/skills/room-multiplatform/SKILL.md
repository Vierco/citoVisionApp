---
name: room-multiplatform
description: "Definir o modificar el esquema de base de datos local (entidades, DAOs), configurar Room KMP, o implementar el LocalDataSource de un Repository."
---

# Skill: room-multiplatform

## Objetivo

Definir cómo configurar e implementar la persistencia local con **Room Multiplatform** (Android, iOS, Desktop) dentro de la capa **Infrastructure**, sirviendo como motor del `LocalDataSource` definido en `repository-pattern` (estrategia single source of truth / offline-first).

Este Skill se basa en **Room 2.8.4** + **sqlite 2.6.2** (Room soporta KMP desde la versión 2.7.0). No cubre Room 3.0 (en desarrollo, con cambios incompatibles de paquete y aún no estable a fecha de este Skill).

---

## Modelo conceptual

```text
infrastructure/persistence
        │
        ├── commonMain
        │     AppDatabase (@Database, @ConstructedBy)   ← entidades + DAOs
        │     TodoEntity (@Entity)
        │     TodoDao (@Dao — suspend / Flow)
        │     getRoomDatabase(builder)                   ← setDriver(BundledSQLiteDriver())
        │                                                   + setQueryCoroutineContext(Dispatchers.IO)
        │
        ├── androidMain  → getDatabaseBuilder(context)    (Context.getDatabasePath())
        ├── iosMain      → getDatabaseBuilder()            (NSFileManager / NSDocumentDirectory)
        └── desktopMain  → getDatabaseBuilder()            (java.io.File)
```

Solo la construcción del `Builder` cambia por plataforma (por las diferencias del sistema de ficheros); el resto — entidades, DAOs, configuración del driver — vive en `commonMain`.

---

## Cuándo usarlo

- Definir o modificar el esquema de base de datos local (entidades, DAOs).
- Configurar Room por primera vez en el módulo `shared`.
- Decidir qué driver SQLite usar.
- Implementar el `LocalDataSource` de un Repository (ver `repository-pattern`).
- Migrar una base de datos Room ya existente de Android-only a KMP.

## Cuándo NO usarlo

- Para diseñar el Repository en sí (combinación Remote/Local, single source of truth) → usar `repository-pattern`.
- Para tipar errores de persistencia → usar `result-pattern`; este Skill solo construye y expone el DAO, el mapeo de excepciones a `DomainError` vive en el Repository.
- Para inyectar `AppDatabase`/DAOs con Koin → usar `dependency-injection-koin`.
- Para llamadas de red → usar `ktor-client`.

---

## Dependencias

```text
- clean-architecture-kmp
- repository-pattern
- result-pattern
- dependency-injection-koin
```

---

## Entradas necesarias

- Entidades a persistir y sus campos (para `@Entity`).
- Consultas necesarias por entidad (para `@Dao`).
- Versión de esquema actual y si requiere migración.
- Targets de la app (Android, iOS, Desktop) para configurar KSP por target.

---

## Criterios arquitectónicos

- `AppDatabase`, las entidades `@Entity` y los `@Dao` viven en `commonMain` dentro de `infrastructure/persistence`; son compartidos al 100% entre plataformas.
- Solo la construcción del `RoomDatabase.Builder` es específica de plataforma, y se resuelve mediante una función por plataforma (`expect/actual` o inyectada vía el `platformModule` de Koin).
- El driver SQLite por defecto es `BundledSQLiteDriver`, para garantizar el mismo comportamiento de SQLite en todas las plataformas; solo se cambia a un driver nativo (`AndroidSQLiteDriver`, `NativeSQLiteDriver`) si hay una razón explícita (tamaño de binario, requisito de sistema operativo).
- Ninguna `@Entity` de Room sale de Infrastructure; el `LocalDataSource` (ver `repository-pattern`) la mapea siempre a la Entity de Domain antes de devolver nada al Repository.

---

## Reglas

- Debe fijar las versiones de Room y SQLite en `libs.versions.toml` (a fecha de este Skill: `room = 2.8.4`, `sqlite = 2.6.2`), junto con una versión de KSP compatible con la versión de Kotlin del proyecto.
- Debe aplicar el plugin `androidx.room` y el plugin `ksp` en el módulo `shared`.
- Debe declarar `AppDatabase`, las entidades y los DAOs en `commonMain`; nunca duplicarlos por plataforma.
- Toda función de un `@Dao` compilada para targets no-Android debe ser `suspend`, salvo que devuelva `Flow<T>` para datos observables.
- Debe usarse `expect object XxxConstructor : RoomDatabaseConstructor<XxxDatabase>` junto con `@ConstructedBy` en la clase `@Database`, dejando que el compilador de Room genere el `actual`.
- La construcción del `RoomDatabase.Builder` debe resolverse por plataforma: `Context.getDatabasePath()` en Android, `NSFileManager`/`NSDocumentDirectory` en iOS, `java.io.File` en Desktop.
- Debe configurarse `setDriver(BundledSQLiteDriver())`, salvo decisión explícita de usar un driver nativo; en ese caso, el driver se declara en el `platformModule` `actual` correspondiente (ver `dependency-injection-koin`).
- Debe configurarse `setQueryCoroutineContext(Dispatchers.IO)` explícitamente, ya que Room KMP no acepta `Executor` en `commonMain`.
- Las migraciones y callbacks deben implementarse con las APIs de `SQLiteConnection` (KMP), nunca con `SupportSQLiteDatabase` (Android-only).
- Las transacciones de escritura deben usar `useWriterConnection { it.immediateTransaction { ... } }`; las de solo lectura, `useReaderConnection { it.deferredTransaction { ... } }`. Nunca debe usarse `withTransaction { }` (API Android-only) en código de `commonMain`.
- Si el build está minificado/obfuscado, debe añadirse la regla ProGuard `-keep class * extends androidx.room.RoomDatabase { <init>(); }`.

---

## Decisiones automáticas

```text
Si la app necesita el mismo comportamiento de SQLite en Android, iOS y Desktop
    → BundledSQLiteDriver (driver por defecto de este Skill)

Si hay una razón explícita de tamaño de binario o de aprovechar el SQLite del sistema
    → driver nativo por plataforma (AndroidSQLiteDriver / NativeSQLiteDriver), declarado en platformModule

Si una función del DAO devuelve datos que cambian con el tiempo (listas, detalle observado)
    → Flow<T>, consistente con el single source of truth de repository-pattern

Si una función del DAO es una acción puntual (insert, update, delete, count)
    → suspend fun

Si se necesita NativeSQLiteDriver en iOS
    → añadir el linker option "-lsqlite3" en la configuración del framework iOS
```

---

## Proceso recomendado

1. Definir las entidades `@Entity` en `infrastructure/persistence` (`commonMain`).
2. Definir el/los `@Dao` con sus consultas (`suspend`/`Flow` según el caso).
3. Definir la clase `@Database` con `@ConstructedBy` y el `expect object Constructor`.
4. Implementar `getDatabaseBuilder()` en `androidMain`/`iosMain`/`desktopMain` según la API de cada plataforma.
5. Implementar `getRoomDatabase(builder)` en `commonMain`, configurando el driver y el `CoroutineContext`.
6. Registrar `AppDatabase` y los DAOs en el `platformModule`/módulos de Koin correspondientes (ver `dependency-injection-koin`).
7. Implementar el `LocalDataSource` (ver `repository-pattern`) inyectando el DAO y mapeando `@Entity` ↔ Entity de Domain.
8. Configurar `schemaDirectory` y, si aplica, las migraciones.

---

## Checklist

- [ ] `AppDatabase`, entidades y DAOs viven en `commonMain`.
- [ ] Las funciones del DAO para targets no-Android son `suspend` o devuelven `Flow`.
- [ ] El driver configurado es `BundledSQLiteDriver`, salvo decisión explícita en contra.
- [ ] La construcción del `Builder` está resuelta por plataforma (Android/iOS/Desktop).
- [ ] Las migraciones usan `SQLiteConnection`, no `SupportSQLiteDatabase`.
- [ ] Ninguna `@Entity` sale de Infrastructure sin mapear a la Entity de Domain.
- [ ] El esquema (`schemaDirectory`) está configurado para poder versionar migraciones.

---

## Definition of Done

- La base de datos Room funciona de forma idéntica en Android, iOS y Desktop con el mismo `commonMain`.
- El `LocalDataSource` que usa Room cumple el contrato de `repository-pattern` (Flow para lo observable, suspend para lo puntual).
- No existen referencias a APIs Android-only (`SupportSQLiteDatabase`, `LiveData`, `withTransaction`) en `commonMain`.
- El proyecto compila y ejecuta consultas en los tres targets sin código condicional fuera de la construcción del `Builder`.

---

## Riesgos

- Usar APIs Android-only (`SupportSQLiteDatabase`, DAO blocking, `LiveData`) en código pensado para `commonMain`, rompiendo la compilación en iOS/Desktop.
- Mezclar `BundledSQLiteDriver` en una plataforma y un driver nativo en otra sin razón, generando comportamientos de SQLite distintos entre plataformas.
- Asumir disponibles APIs no soportadas en KMP (query callbacks, auto-close por timeout, base de datos pre-empaquetada, multi-instance invalidation).
- Olvidar la regla ProGuard en builds minificados, rompiendo Room en producción.
- DAOs devolviendo entidades de Room directamente a Application/Presentation.

---

## Anti-patrones

- DAO con una función blocking (no `suspend`, no `Flow`) compilada para `commonMain`.
- `RoomDatabase.withTransaction { }` usado en código compartido (API Android-only).
- `@Entity` de Room expuesta como tipo de retorno de un método público fuera de Infrastructure.
- Construcción del `RoomDatabase.Builder` duplicada manualmente en cada feature en lugar de una única función compartida por módulo.
- Migraciones definidas con `SupportSQLiteDatabase` en código que debe compilar para iOS/Desktop.

---

## Comandos útiles

```toml
# libs.versions.toml
[versions]
room = "2.8.4"
sqlite = "2.6.2"
ksp = "<version-compatible-con-kotlin>"

[libraries]
androidx-sqlite-bundled = { module = "androidx.sqlite:sqlite-bundled", version.ref = "sqlite" }
androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
androidx-room = { id = "androidx.room", version.ref = "room" }
```

```kotlin
// shared/build.gradle.kts
plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspIosX64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspDesktop", libs.androidx.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}
```

---

## Salida esperada

```text
infrastructure/
  persistence/
    AppDatabase.kt           → @Database + expect object Constructor (commonMain)
    TodoEntity.kt              → @Entity (commonMain)
    TodoDao.kt                 → @Dao (commonMain)
    DatabaseBuilder.kt         → getRoomDatabase(builder) (commonMain)
    DatabaseBuilder.android.kt → getDatabaseBuilder(context) (androidMain)
    DatabaseBuilder.ios.kt     → getDatabaseBuilder() (iosMain)
    DatabaseBuilder.desktop.kt → getDatabaseBuilder() (desktopMain)
```

---

## Ejemplos

### Correcto — Entidad, DAO y Database en commonMain

```kotlin
// infrastructure/persistence/TodoEntity.kt — commonMain
@Entity
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String
)

// infrastructure/persistence/TodoDao.kt — commonMain
@Dao
interface TodoDao {
    @Insert
    suspend fun insert(item: TodoEntity)

    @Query("SELECT * FROM TodoEntity")
    fun observeAll(): Flow<List<TodoEntity>>

    @Query("DELETE FROM TodoEntity WHERE id = :id")
    suspend fun deleteById(id: Long)
}

// infrastructure/persistence/AppDatabase.kt — commonMain
@Database(entities = [TodoEntity::class], version = 1)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

// infrastructure/persistence/DatabaseBuilder.kt — commonMain
fun getRoomDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
```

### Correcto — Builder específico por plataforma

```kotlin
// infrastructure/persistence/DatabaseBuilder.android.kt — androidMain
fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("app.db")
    return Room.databaseBuilder<AppDatabase>(context = appContext, name = dbFile.absolutePath)
}
```

```kotlin
// infrastructure/persistence/DatabaseBuilder.ios.kt — iosMain
fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFilePath = documentDirectory() + "/app.db"
    return Room.databaseBuilder<AppDatabase>(name = dbFilePath)
}

private fun documentDirectory(): String {
    val dir = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )
    return requireNotNull(dir?.path)
}
```

```kotlin
// infrastructure/persistence/DatabaseBuilder.desktop.kt — desktopMain
fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), "app.db")
    return Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath)
}
```

### Correcto — LocalDataSource consumiendo el DAO (ver repository-pattern)

```kotlin
// infrastructure/datasources/RoomTodoLocalDataSource.kt
class RoomTodoLocalDataSource(private val dao: TodoDao) : TodoLocalDataSource {
    override fun observeItems(): Flow<List<TodoEntity>> = dao.observeAll()
    override suspend fun insert(item: TodoEntity) = dao.insert(item)
    override suspend fun delete(id: Long) = dao.deleteById(id)
}
```

### Incorrecto

```kotlin
// ❌ Función blocking en un DAO compartido por commonMain
@Dao
interface TodoDao {
    @Query("SELECT * FROM TodoEntity")
    fun getAllTodos(): List<TodoEntity> // bloqueante: rompe en iOS/Desktop
}

// ❌ API Android-only en código común
suspend fun sync(database: AppDatabase) {
    database.withTransaction { /* ... */ } // withTransaction no existe en KMP
}

// ❌ Entity de Room expuesta fuera de Infrastructure
class TodoRepositoryImpl(private val dao: TodoDao) : TodoRepository {
    fun observeItems(): Flow<List<TodoEntity>> = dao.observeAll() // debería mapear a Item (Domain)
}
```

---

## Referencias

- clean-architecture-kmp
- repository-pattern
- result-pattern
- dependency-injection-koin
- Documentación oficial: https://developer.android.com/kotlin/multiplatform/room
