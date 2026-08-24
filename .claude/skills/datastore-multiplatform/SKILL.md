---
name: datastore-multiplatform
description: "Guardar preferencias de usuario no sensibles (tema, idioma, onboarding, flags), o implementar el Repository de configuración de la app."
---

# Skill: datastore-multiplatform

## Objetivo

Definir cómo almacenar preferencias y configuración simple de usuario (clave-valor) con **Jetpack DataStore** en proyectos Kotlin Multiplatform (Android, iOS, Desktop), dentro de `infrastructure/persistence`, y delimitar qué datos pertenecen aquí frente a Room (`room-multiplatform`) o a un almacenamiento seguro (fuera del alcance de este Skill).

Este Skill se basa en **DataStore 1.2.x**. Solo cubre **Preferences DataStore**: es el único tipo de DataStore soportado en Kotlin Multiplatform a fecha de este Skill (Proto DataStore sigue siendo Android-only).

---

## Modelo conceptual

```text
infrastructure/persistence/preferences
        │
        ├── commonMain
        │     AppPreferenceKeys.kt          ← Preferences.Key<T> centralizados (evita strings sueltos)
        │     createDataStore(producePath)   ← PreferenceDataStoreFactory.createWithPath(...)
        │     SettingsRepositoryImpl(dataStore: DataStore<Preferences>)
        │
        ├── androidMain  → producePath(): context.filesDir.resolve("app.preferences_pb").absolutePath
        ├── iosMain      → producePath(): NSDocumentDirectory + "/app.preferences_pb"
        └── desktopMain  → producePath(): carpeta de datos de la app + "/app.preferences_pb"
```

El `DataStore<Preferences>` se crea una sola vez (singleton vía Koin); solo la ruta del fichero cambia por plataforma, igual que el `Builder` de Room.

---

## Cuándo usarlo

- Guardar preferencias de usuario (tema, idioma, onboarding completado, último filtro usado).
- Guardar flags de feature o configuración simple que no necesita consultas relacionales.
- Implementar el Repository de configuración de la app (no de entidades de negocio).
- Decidir si un dato va en DataStore, en Room o en almacenamiento seguro.

## Cuándo NO usarlo

- Para datos relacionales, con consultas complejas o que crecen en volumen (listas, históricos, caché de entidades) → usar `room-multiplatform`.
- Para tokens de sesión, contraseñas, claves de API o cualquier dato sensible → **no usar DataStore Preferences sin cifrar**; DataStore almacena en texto plano. Esto queda fuera del alcance de este Skill: requiere una solución de almacenamiento seguro (Android Keystore / iOS Keychain) todavía no definida en la biblioteca de Skills.
- Para datos estructurados con esquema fuerte (Proto DataStore) → no soportado en Kotlin Multiplatform a fecha de este Skill.
- Para definir el Repository Pattern en general → ver `repository-pattern`; este Skill es un caso particular más simple (solo almacenamiento local, sin fuente remota que sincronizar).

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

- Lista de preferencias a almacenar y su tipo (`String`, `Boolean`, `Int`, etc.).
- Confirmación de que ninguna de esas preferencias es sensible (token, contraseña, dato personal identificable).
- Valor por defecto de cada preferencia, para cuando no existe aún o la lectura falla.

---

## Criterios arquitectónicos

- DataStore Preferences es para configuración simple clave-valor, no para datos relacionales (eso es Room) ni para secretos (eso requiere cifrado, fuera de este Skill).
- Solo se soporta Preferences DataStore en KMP; Proto DataStore es Android-only y no debe usarse en `commonMain`.
- El `DataStore<Preferences>` se crea una única vez por aplicación (singleton vía Koin), igual que la base de datos Room.
- Las claves (`Preferences.Key<T>`) se centralizan en un único objeto por dominio de configuración, nunca como `String` sueltos repetidos en distintos ficheros.
- El Repository que envuelve el DataStore expone tipos de Domain/Application (`Boolean`, `String`, enums propios), nunca el tipo `Preferences` de DataStore fuera de Infrastructure.

---

## Reglas

- Debe declarar las dependencias `androidx.datastore:datastore` y `androidx.datastore:datastore-preferences` en `commonMain` (versión: línea estable `1.2.x`).
- Debe crear el `DataStore<Preferences>` mediante `PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })`, resolviendo `producePath()` de forma específica por plataforma (Android: `context.filesDir`; iOS: `NSDocumentDirectory`; Desktop: carpeta de datos de la app).
- Debe registrarse el `DataStore<Preferences>` como `single` en Koin, igual que la base de datos Room (ver `dependency-injection-koin`).
- Las claves deben definirse con los builders tipados (`booleanPreferencesKey`, `stringPreferencesKey`, `intPreferencesKey`, etc.) agrupadas en un único objeto (`AppPreferenceKeys`), nunca repetidas como literales en cada uso.
- Las lecturas deben aplicar `.catch { emit(emptyPreferences()) }` (o un valor por defecto explícito) sobre `dataStore.data`, para no propagar `IOException` directamente a Application/Presentation.
- Las escrituras se hacen con `dataStore.edit { preferences -> ... }`; no debe leerse y escribirse fuera de ese bloque para evitar condiciones de carrera.
- **Nunca debe guardarse un token de sesión, contraseña, clave de API o cualquier dato sensible en DataStore Preferences sin cifrar**; ese tipo de dato necesita almacenamiento seguro específico de plataforma, no cubierto por este Skill.
- El Repository que envuelve el DataStore debe exponer una API tipada por preferencia (ej. `val isOnboardingCompleted: Flow<Boolean>`, `suspend fun setOnboardingCompleted(value: Boolean)`), no un acceso genérico a `Preferences` desde fuera de Infrastructure.

---

## Decisiones automáticas

```text
Si el dato es una preferencia simple de usuario (flag, configuración, último valor usado)
    → DataStore Preferences

Si el dato es relacional, crece en volumen, o necesita consultas (listas, históricos, caché de entidades)
    → Room (ver room-multiplatform), no DataStore

Si el dato es sensible (token, contraseña, credencial, dato médico/personal identificable)
    → no usar DataStore Preferences sin cifrar; requiere almacenamiento seguro
      (fuera del alcance de este Skill, pendiente de un Skill dedicado)

Si la lectura de una preferencia falla (IOException, corrupción)
    → devolver el valor por defecto vía .catch{}, nunca propagar la excepción a Application

Si se necesita un dato estructurado con esquema fuerte
    → no usar Proto DataStore (no soportado en KMP); usar Room en su lugar
```

---

## Proceso recomendado

1. Identificar las preferencias a almacenar y comprobar que ninguna es sensible (si lo es, detener y resolver primero el almacenamiento seguro).
2. Definir las claves en `AppPreferenceKeys` con los builders tipados.
3. Crear `createDataStore(producePath)` en `commonMain` y su `producePath()` específico por plataforma.
4. Implementar el Repository (ej. `SettingsRepositoryImpl`) exponiendo Flows tipados con `.catch{}` y funciones `suspend fun set...()`.
5. Registrar el `DataStore<Preferences>` y el Repository en Koin.
6. Consumir el Repository desde el `UseCase`/ViewModel correspondiente, nunca el `DataStore` directamente desde Presentation.

---

## Checklist

- [ ] Solo se usa Preferences DataStore, nunca Proto DataStore, en código de `commonMain`.
- [ ] El `DataStore<Preferences>` se crea una sola vez y se registra como `single` en Koin.
- [ ] Las claves están centralizadas en un objeto tipado, no como `String` sueltos.
- [ ] Las lecturas tienen un valor por defecto explícito ante fallo (`.catch{}`).
- [ ] Ninguna preferencia sensible (token, contraseña, dato médico identificable) se guarda en DataStore.
- [ ] El Repository expone tipos de dominio, no `Preferences` crudo, fuera de Infrastructure.

---

## Definition of Done

- Las preferencias definidas se leen/escriben de forma idéntica en Android, iOS y Desktop.
- Ninguna excepción de DataStore llega sin manejar a Application/Presentation.
- No hay ningún dato sensible almacenado en DataStore Preferences.
- El Repository de configuración tiene su test correspondiente (ver `testing-kmp`), incluyendo el caso de valor por defecto.

---

## Riesgos

- Guardar tokens/credenciales en DataStore Preferences sin cifrar, exponiendo datos sensibles si el dispositivo se compromete.
- Claves repetidas como `String` sueltos, con riesgo de typos y colisiones de nombre.
- Propagar `IOException` de una lectura corrupta hasta la UI en lugar de usar un valor por defecto.
- Mezclar datos relacionales/voluminosos en DataStore en lugar de Room, degradando el rendimiento.
- `DataStore<Preferences>` instanciado más de una vez (no singleton), provocando inconsistencias entre instancias.

---

## Anti-patrones

- `dataStore.edit { it[stringPreferencesKey("auth_token")] = token }` — guardar un token de sesión en DataStore sin cifrar.
- `stringPreferencesKey("user_name")` repetido como literal en varios ficheros en lugar de centralizado en `AppPreferenceKeys`.
- `dataStore.data.collect { ... }` sin `.catch{}`, dejando que una `IOException` rompa el flujo observado por la UI.
- Usar Proto DataStore (`DataStoreFactory.create(serializer = ...)`) en `commonMain`, no soportado en KMP.
- Exponer `DataStore<Preferences>` directamente a un ViewModel en lugar de envolverlo en un Repository tipado.

---

## Comandos útiles

```toml
# libs.versions.toml
[versions]
datastore = "1.2.1"

[libraries]
androidx-datastore = { module = "androidx.datastore:datastore", version.ref = "datastore" }
androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
```

```kotlin
// shared/build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.androidx.datastore)
            implementation(libs.androidx.datastore.preferences)
        }
    }
}
```

---

## Salida esperada

```text
infrastructure/
  persistence/
    preferences/
      AppPreferenceKeys.kt
      DataStoreFactory.kt          → createDataStore(producePath) (commonMain)
      DataStoreFactory.android.kt  → producePath() (androidMain)
      DataStoreFactory.ios.kt      → producePath() (iosMain)
      DataStoreFactory.desktop.kt  → producePath() (desktopMain)
      SettingsRepositoryImpl.kt
domain/
  repositories/
    SettingsRepository.kt          → Port (interfaz)
```

---

## Ejemplos

### Correcto — claves centralizadas

```kotlin
// infrastructure/persistence/preferences/AppPreferenceKeys.kt
object AppPreferenceKeys {
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val LAST_USED_FILTER = stringPreferencesKey("last_used_filter")
}
```

### Correcto — factory común + path por plataforma

```kotlin
// infrastructure/persistence/preferences/DataStoreFactory.kt — commonMain
const val PREFERENCES_FILE_NAME = "app.preferences_pb"

fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })
```

```kotlin
// androidMain
fun producePath(context: Context): String =
    context.filesDir.resolve(PREFERENCES_FILE_NAME).absolutePath
```

```kotlin
// iosMain
fun producePath(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )
    return requireNotNull(documentDirectory?.path) + "/$PREFERENCES_FILE_NAME"
}
```

```kotlin
// desktopMain (jvmMain)
fun producePath(): String =
    File(System.getProperty("java.io.tmpdir"), PREFERENCES_FILE_NAME).absolutePath
```

### Correcto — Repository tipado (ver repository-pattern)

```kotlin
// domain/repositories/SettingsRepository.kt
interface SettingsRepository {
    val isOnboardingCompleted: Flow<Boolean>
    suspend fun setOnboardingCompleted(value: Boolean)
}

// infrastructure/persistence/preferences/SettingsRepositoryImpl.kt
class SettingsRepositoryImpl(private val dataStore: DataStore<Preferences>) : SettingsRepository {

    override val isOnboardingCompleted: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences -> preferences[AppPreferenceKeys.ONBOARDING_COMPLETED] ?: false }

    override suspend fun setOnboardingCompleted(value: Boolean) {
        dataStore.edit { preferences -> preferences[AppPreferenceKeys.ONBOARDING_COMPLETED] = value }
    }
}
```

### Incorrecto

```kotlin
// ❌ Token de sesión guardado en DataStore sin cifrar
dataStore.edit { it[stringPreferencesKey("auth_token")] = token }

// ❌ Clave repetida como literal en varios ficheros
preferences[stringPreferencesKey("user_name")] // en ScreenA.kt
preferences[stringPreferencesKey("user_name")] // en ScreenB.kt, con riesgo de typo

// ❌ Sin manejo de fallo de lectura
val flag: Flow<Boolean> = dataStore.data.map { it[AppPreferenceKeys.ONBOARDING_COMPLETED] ?: false }
// si data lanza IOException, rompe el flujo sin valor por defecto

// ❌ DataStore expuesto directamente al ViewModel
class SettingsViewModel(private val dataStore: DataStore<Preferences>) : ViewModel()
```

---

## Referencias

- clean-architecture-kmp
- repository-pattern
- result-pattern
- dependency-injection-koin
- Documentación oficial: https://developer.android.com/kotlin/multiplatform/datastore
