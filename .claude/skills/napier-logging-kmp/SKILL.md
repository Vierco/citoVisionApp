---
name: napier-logging-kmp
description: "Inicializar el logging del proyecto, decidir el nivel de log adecuado para un mensaje, o loguear una excepción capturada en un UseCase/Repository/ViewModel."
---

# Skill: napier-logging-kmp

## Objetivo

Definir cómo configurar y usar **Napier** como logger en proyectos Kotlin Multiplatform (Android, iOS, Desktop), abstraído detrás de una interfaz `Logger` propia en `core/logging` — responsabilidad ya prevista explícitamente en `ARCHITECTURE.md` ("Logging abstraído" dentro de `core`).

Este Skill se basa en **Napier 2.7.x** (línea estable). Ninguna capa de Domain, Application, Infrastructure o Presentation debe importar `io.github.aakira.napier.Napier` directamente; todas dependen de la interfaz `Logger`, igual que nunca dependen de Ktor o Room directamente fuera de Infrastructure.

---

## Modelo conceptual

```text
core/logging
        │
        ├── Logger.kt          ← interfaz (Port transversal, no de negocio)
        ├── NapierLogger.kt     ← implementación concreta sobre Napier
        └── isDebugBuild.kt      ← expect val isDebugBuild: Boolean

composition/di
        │
        └── initLogger()        ← Napier.base(...) según isDebugBuild; se llama junto a initKoin()

androidMain  → actual val isDebugBuild: Boolean = BuildConfig.DEBUG
iosMain      → actual val isDebugBuild: Boolean = Platform.isDebugBinary
desktopMain  → actual val isDebugBuild: Boolean = true   // revisar cuando Desktop tenga build de release real

Domain / Application / Infrastructure / Presentation
        │
        └── reciben Logger por constructor (Koin), nunca Napier directamente
```

---

## Cuándo usarlo

- Inicializar el logging por primera vez en el proyecto.
- Decidir el nivel de log adecuado para un mensaje (`v`/`d`/`i`/`w`/`e`/`wtf`).
- Loguear una excepción capturada en un `UseCase`, `Repository` o ViewModel.
- Integrar el logging de Ktor con Napier (ver `ktor-client`).
- Revisar si un log expone datos sensibles.

## Cuándo NO usarlo

- Para decidir qué excepción se captura o cómo se mapea a `DomainError` → ver `result-pattern`; este Skill solo define cómo se registra el log, no el manejo del error en sí.
- Para crash reporting u observabilidad remota completa (Crashlytics, Sentry...) → puede integrarse como un `Antilog`/`Logger` personalizado, pero ese alcance no está cubierto aquí salvo que se solicite explícitamente.
- Para decidir qué dato es sensible y dónde se guarda → ver `datastore-multiplatform`; este Skill aplica el mismo criterio a los logs, no lo redefine.

---

## Dependencias

```text
- clean-architecture-kmp
- dependency-injection-koin
- ktor-client
- result-pattern
```

---

## Entradas necesarias

- Si el proyecto necesita un `Antilog`/`Logger` de producción distinto del de debug (ej. enviar errores a un backend de observabilidad) o simplemente silenciar logs verbosos en producción.
- Qué datos se consideran sensibles en el proyecto (tokens, PII, datos médicos — ver `datastore-multiplatform`), para no loguearlos nunca, ni siquiera en debug.

---

## Criterios arquitectónicos

- El logging vive abstraído en `core/logging` mediante una interfaz `Logger`; Napier es un detalle de implementación, no una dependencia que el resto de capas conozcan.
- Napier se inicializa una única vez por proceso (`Napier.base(...)`), junto al arranque de Koin, nunca de forma repetida ni dentro de una clase que solo necesita loguear.
- El nivel de log determina su intención: `v`/`d` para depuración de desarrollo, `i` para eventos relevantes de negocio, `w` para situaciones recuperables anómalas, `e` para errores que afectan al usuario, `wtf` reservado para invariantes rotas que nunca deberían ocurrir.
- El comportamiento de Napier en debug y en release no es el mismo: en debug se vuelca a consola (`DebugAntilog`); en release no debe asumirse que el log es invisible para terceros, por lo que nunca debe contener datos sensibles.

---

## Reglas

- Debe definirse `interface Logger` en `core/logging` con, al menos, `v`, `d`, `i`, `w`, `e` (con `tag` y `throwable` opcionales); ninguna otra capa debe importar `Napier` directamente.
- `NapierLogger` (implementación de `Logger` sobre Napier) debe ser la única clase del proyecto que llama a la API estática de `Napier`.
- Debe inicializarse Napier una sola vez, con `Napier.base(DebugAntilog())` en builds de debug; en release no debe usarse `DebugAntilog` sin más — debe decidirse explícitamente un `Antilog` que no exponga datos sensibles (silencioso, o conectado a un backend de observabilidad si el proyecto lo tiene).
- Debe usarse `isDebugBuild` (`expect/actual` por plataforma) para decidir el `Antilog`, nunca una constante hardcodeada en `commonMain`.
- Las clases de negocio deben recibir `Logger` por constructor (vía Koin), igual que cualquier otra dependencia; no debe usarse un singleton estático accedido directamente desde Domain/Application.
- Cada log debe incluir un `tag` identificable (ej. el nombre simple de la clase emisora), para poder rastrear el origen sin depender de convenciones de plataforma.
- **Nunca debe loguearse un token de sesión, contraseña, clave de API o dato sensible** (mismo criterio que `datastore-multiplatform`); esto aplica también a logs de nivel `v`/`d` en builds de debug, no solo a release.
- Al loguear un error ya mapeado a `DomainError`/`AppError` (ver `result-pattern`), debe loguearse antes de envolverlo en `Result.Failure`, incluyendo la excepción original como `throwable` si la hay.
- La integración de Napier con el logging de Ktor (`ktor-client`) debe mantener la regla ya establecida de sanitizar cabeceras sensibles (`Authorization`); Napier no debe recibir el log crudo del cliente HTTP sin pasar por esa sanitización.

---

## Decisiones automáticas

```text
Si el mensaje es de depuración interna sin relevancia de negocio
    → Logger.v / Logger.d

Si el mensaje describe un evento de negocio relevante (login exitoso, sincronización completada)
    → Logger.i

Si ocurre una situación anómala pero recuperable (reintento de red, caché vacía usada como fallback)
    → Logger.w

Si ocurre un error que afecta al usuario o se traduce en un DomainError/AppError
    → Logger.e con la excepción original como throwable

Si el dato a loguear es sensible (token, contraseña, dato médico/personal identificable)
    → no se loguea, ni en debug ni en release; se omite o se sustituye por un valor enmascarado

Si el build es de release y no hay backend de observabilidad definido
    → usar un Antilog silencioso o mínimo, nunca DebugAntilog sin modificar
```

---

## Proceso recomendado

1. Definir `interface Logger` en `core/logging` con los niveles necesarios.
2. Implementar `NapierLogger` sobre la API de Napier.
3. Definir `expect val isDebugBuild: Boolean` y sus `actual` por plataforma.
4. Llamar a `Napier.base(...)` una sola vez, junto a `initKoin()`, eligiendo el `Antilog` según `isDebugBuild`.
5. Registrar `Logger` como `single` en Koin (`single<Logger> { NapierLogger() }`).
6. Inyectar `Logger` en las clases que lo necesiten (UseCases, Repositories, ViewModels) por constructor.
7. Revisar antes de cada log si el contenido podría ser sensible; si lo es, omitirlo o enmascararlo.

---

## Checklist

- [ ] Existe `interface Logger` en `core/logging`; ninguna otra clase importa `Napier` directamente.
- [ ] Napier se inicializa una sola vez, junto a `initKoin()`.
- [ ] El `Antilog` usado depende de `isDebugBuild`, no de una constante hardcodeada.
- [ ] `Logger` se inyecta por constructor vía Koin, no se accede como singleton estático desde Domain/Application.
- [ ] Cada log incluye un `tag` identificable.
- [ ] Ningún log (en ningún nivel, en ningún build) contiene tokens, contraseñas o datos sensibles.
- [ ] Los errores mapeados a `DomainError`/`AppError` se loguean con su excepción original antes de envolverse en `Result.Failure`.

---

## Definition of Done

- El logging funciona de forma idéntica en Android, iOS y Desktop a través de la misma interfaz `Logger`.
- Ningún log de producción expone datos sensibles.
- Los errores relevantes (`Logger.e`) incluyen suficiente contexto (`tag`, mensaje, excepción) para diagnosticar el problema sin necesidad de reproducirlo.
- Sustituir Napier por otra librería de logging no requeriría tocar Domain, Application ni Presentation, solo `NapierLogger` en `core/logging`.

---

## Riesgos

- Loguear tokens, contraseñas o datos médicos/personales, exponiendo información sensible en logs de dispositivo o de producción.
- Usar `DebugAntilog` en builds de release, dejando trazas detalladas accesibles si el binario se inspecciona.
- Acoplar Domain/Application a la API estática de `Napier`, dificultando testear esas clases o sustituir la librería de logging en el futuro.
- Logs sin `tag` ni contexto suficiente, dificultando el diagnóstico de errores reportados por usuarios.
- Inicializar Napier más de una vez o desde varios puntos del código, con configuraciones inconsistentes.

---

## Anti-patrones

- `import io.github.aakira.napier.Napier` dentro de un `UseCase` o de una `Entity` de Domain.
- `Napier.d("token: $token")` — loguear un dato sensible, incluso en nivel debug.
- `Napier.base(DebugAntilog())` sin condicionarlo a `isDebugBuild`, dejándolo también activo en release.
- `class LoginViewModel { private val logger = NapierLogger() }` — instanciar el logger directamente en lugar de inyectarlo vía Koin.
- Logs sin tag (`Napier.e(e.message ?: "error")`) que no permiten saber qué componente generó el error.

---

## Comandos útiles

```toml
# libs.versions.toml
[versions]
napier = "2.7.1"

[libraries]
napier = { module = "io.github.aakira:napier", version.ref = "napier" }
```

```kotlin
// shared/build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.napier)
        }
    }
}
```

---

## Salida esperada

```text
core/
  logging/
    Logger.kt              → interfaz (commonMain)
    NapierLogger.kt          → implementación sobre Napier (commonMain)
    IsDebugBuild.kt            → expect val isDebugBuild: Boolean (commonMain)
    IsDebugBuild.android.kt     → actual (androidMain)
    IsDebugBuild.ios.kt           → actual (iosMain)
    IsDebugBuild.desktop.kt        → actual (desktopMain)
composition/
  di/
    InitLogger.kt                  → fun initLogger() { Napier.base(...) }
```

---

## Ejemplos

### Correcto — interfaz abstraída + implementación Napier

```kotlin
// core/logging/Logger.kt
interface Logger {
    fun v(tag: String, message: String)
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}

// core/logging/NapierLogger.kt
class NapierLogger : Logger {
    override fun v(tag: String, message: String) = Napier.v(tag = tag) { message }
    override fun d(tag: String, message: String) = Napier.d(tag = tag) { message }
    override fun i(tag: String, message: String) = Napier.i(tag = tag) { message }
    override fun w(tag: String, message: String, throwable: Throwable?) =
        Napier.w(tag = tag, throwable = throwable) { message }
    override fun e(tag: String, message: String, throwable: Throwable?) =
        Napier.e(tag = tag, throwable = throwable) { message }
}
```

### Correcto — isDebugBuild por plataforma

```kotlin
// core/logging/IsDebugBuild.kt — commonMain
expect val isDebugBuild: Boolean
```

```kotlin
// androidMain
actual val isDebugBuild: Boolean = BuildConfig.DEBUG
```

```kotlin
// iosMain
actual val isDebugBuild: Boolean = Platform.isDebugBinary
```

```kotlin
// desktopMain
actual val isDebugBuild: Boolean = true // revisar cuando Desktop tenga un build de release real
```

### Correcto — inicialización junto a Koin

```kotlin
// composition/di/InitLogger.kt — commonMain
fun initLogger() {
    Napier.base(if (isDebugBuild) DebugAntilog() else SilentAntilog())
}
```

```kotlin
// composition/di/LoggingModule.kt
val loggingModule = module {
    single<Logger> { NapierLogger() }
}
```

### Correcto — uso en un Repository (ver result-pattern)

```kotlin
class ItemRepositoryImpl(
    private val remote: ItemRemoteDataSource,
    private val local: ItemLocalDataSource,
    private val logger: Logger
) : ItemRepository {

    override suspend fun refresh(): Result<Unit, DomainError> = try {
        val dtos = remote.fetchItems()
        local.replaceAll(dtos.map { it.toEntity() })
        Result.Success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.e("ItemRepository", "refresh() failed", e)
        Result.Failure(DomainError.NetworkUnavailable)
    }
}
```

### Incorrecto

```kotlin
// ❌ Napier importado directamente en Domain
import io.github.aakira.napier.Napier

class CalculatePriceService {
    fun calculate(item: Item): Double {
        Napier.d("calculating price") // Domain no debe conocer Napier
        return item.basePrice * 1.21
    }
}

// ❌ Dato sensible logueado
Napier.d("user token: $authToken")

// ❌ DebugAntilog sin condicionar a isDebugBuild
fun initLogger() {
    Napier.base(DebugAntilog()) // también activo en release
}

// ❌ Logger instanciado a mano en lugar de inyectado
class HomeViewModel : ViewModel() {
    private val logger = NapierLogger()
}
```

---

## Referencias

- clean-architecture-kmp
- dependency-injection-koin
- ktor-client
- result-pattern
- ARCHITECTURE.md — sección Core ("Logging abstraído")
- Documentación oficial: https://github.com/AAkira/Napier
