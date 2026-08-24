---
name: build-config-kmp
description: "Definir la URL base de la API por entorno, gestionar claves de API u otros secretos de compilación, o definir feature flags fijados en tiempo de compilación."
---

# Skill: build-config-kmp

## Objetivo

Definir cómo gestionar la configuración de build por entorno (dev/staging/prod) en un proyecto Kotlin Multiplatform — URLs base, claves de API, flags fijados en tiempo de compilación — sin duplicar valores por plataforma y sin comprometer secretos en el repositorio.

Este Skill se basa en el plugin **`com.github.gmazzo.buildconfig`** (línea estable `6.x`), que genera un `BuildConfig` multiplataforma mediante `expect`/`actual` para los valores que difieren por plataforma. Su soporte para Kotlin Multiplatform es señalado por el propio autor como experimental a fecha de este Skill; ver "Riesgos".

---

## Modelo conceptual

```text
shared/build.gradle.kts
        │
        ├── appEnv = property "appEnv" (dev | staging | prod), por defecto "dev"
        ├── buildConfig {
        │       buildConfigField("ENVIRONMENT", appEnv)
        │       buildConfigField("BASE_URL", <según entorno>)
        │       buildConfigField("API_KEY", System.getenv("...") ?: error(...))
        │   }
        │
        ▼
BuildConfig (generado, expect/actual si hace falta)
        │
        ▼
core/config/AppConfig.kt   ← envuelve el BuildConfig generado
        │
        ▼
ktor-client / napier-logging-kmp / resto de la app
   (consumen AppConfig, nunca el BuildConfig generado directamente)
```

Los secretos nunca viven en el script de Gradle ni en el repositorio: se leen de variables de entorno o de un fichero local no versionado.

---

## Cuándo usarlo

- Definir la URL base de la API por entorno (consumida por `ktor-client`).
- Gestionar claves de API u otros secretos de compilación.
- Definir feature flags fijados en tiempo de compilación (distintos de los flags de usuario en runtime, ver `datastore-multiplatform`).
- Decidir qué valores cambian por entorno y cuáles son iguales en todos.

## Cuándo NO usarlo

- Para preferencias de usuario en runtime → usar `datastore-multiplatform`.
- Para secretos obtenidos en runtime (ej. el token de sesión tras login) → eso no es configuración de build; pertenece a almacenamiento seguro, no cubierto todavía por ningún Skill.
- Para decidir el motor HTTP/engine por plataforma → usar `ktor-client`; este Skill solo provee el valor de `BASE_URL`, no decide el cliente.
- Para la señal `isDebugBuild` en sí misma → ya está definida en `napier-logging-kmp`; este Skill la reutiliza, no la redefine.

---

## Dependencias

```text
- clean-architecture-kmp
- gradle-conventions-kmp
- ktor-client
- napier-logging-kmp
```

---

## Entradas necesarias

- Lista de valores que cambian por entorno (URL base, claves públicas, flags) y cuáles son secretos.
- Entornos soportados (dev/staging/prod) y cómo se selecciona uno al construir (`-PappEnv=...`).
- De dónde provienen los secretos: variable de entorno de CI, `local.properties`/`.env` no versionado.
- Si Android ya tiene `productFlavors` definidos o si el entorno se gestiona solo por propiedad de Gradle.

---

## Criterios arquitectónicos

- La configuración de build vive generada por Gradle (`BuildConfig`), nunca hardcodeada como literal disperso en el código de features.
- El acceso a estos valores desde el código de la app se hace a través de un objeto propio `AppConfig` en `core/config` que envuelve el `BuildConfig` generado — mismo criterio que `Logger` envuelve a Napier en `napier-logging-kmp` — para no acoplar Domain/Application al nombre de clase generado por un plugin de terceros.
- Los secretos nunca se commitean al repositorio; se inyectan desde variables de entorno o un fichero local no versionado.
- Cada entorno determina un conjunto coherente y completo de valores; nunca se mezclan valores de distintos entornos en el mismo build.

---

## Reglas

- Debe declararse el plugin `com.github.gmazzo.buildconfig` (línea estable `6.x`) en el módulo `shared`.
- Debe envolverse el `BuildConfig` generado en `AppConfig` (`core/config`); el resto del código (UseCases, Repositories, ViewModels) debe depender de `AppConfig`, nunca del `BuildConfig` generado directamente.
- Los valores que cambian por entorno deben leerse de una propiedad de Gradle (`-PappEnv=staging`) o variable de entorno, nunca como literal fijo en el script de build.
- Los secretos (API keys privadas, tokens de build) deben leerse desde variables de entorno o un fichero `local.properties`/`.env` excluido de git (`.gitignore`); nunca deben aparecer como literal en `build.gradle.kts` ni en el repositorio.
- Cada entorno soportado debe tener sus valores agrupados de forma consistente (ej. un `Map`/`when` único en el script de Gradle), nunca condicionales sueltos repetidos por cada variable.
- Un campo sin proveedor explícito (`expect<String>()` sin default) debe forzar que cada plataforma lo provea; no debe quedar un campo sin valor en ninguna plataforma soportada.
- Debe documentarse en el repositorio (ej. `local.properties.example`) qué variables o propiedades espera el build, sin incluir los valores reales.

---

## Decisiones automáticas

```text
Si el valor cambia por entorno (dev/staging/prod) pero es público (URL base, nombre del entorno)
    → buildConfigField leído de una propiedad de Gradle por entorno

Si el valor es un secreto (API key privada, token de build)
    → leer desde variable de entorno o local.properties/.env no versionado, nunca hardcodear

Si el valor es el mismo en todas las plataformas
    → buildConfigField común, sin necesidad de expect/actual por plataforma

Si el valor necesita ser distinto por plataforma además de por entorno
    → declarar el campo como expect<T>() sin default y proveer el actual en cada sourceSet de plataforma

Si no hay variable/propiedad provista para un entorno requerido
    → fallar el build explícitamente (error()), nunca asumir un valor por defecto silencioso para producción
```

---

## Proceso recomendado

1. Listar los valores que cambian por entorno y los que son secretos.
2. Definir los entornos soportados y cómo se selecciona uno al construir.
3. Aplicar el plugin `com.github.gmazzo.buildconfig` en `shared` y declarar los `buildConfigField` correspondientes.
4. Para los secretos, leerlos desde variable de entorno o `local.properties` no versionado.
5. Envolver el `BuildConfig` generado en `AppConfig` (`core/config`).
6. Inyectar/consumir `AppConfig` donde se necesite (ej. `BASE_URL` en `ktor-client`).
7. Documentar las variables esperadas sin exponer sus valores reales.

---

## Checklist

- [ ] El plugin de BuildConfig está aplicado solo en los módulos que lo necesitan.
- [ ] Los valores por entorno se seleccionan vía propiedad de Gradle, no hardcodeados.
- [ ] Ningún secreto aparece como literal en `build.gradle.kts` ni en el repositorio.
- [ ] Existe `AppConfig` en `core/config` envolviendo el `BuildConfig` generado.
- [ ] El resto del código depende de `AppConfig`, no del `BuildConfig` generado directamente.
- [ ] Hay documentación (sin valores reales) de qué variables/propiedades espera el build.
- [ ] El build falla explícitamente si falta una variable requerida.

---

## Definition of Done

- Cada entorno produce un build con un conjunto coherente y completo de valores de configuración.
- Ningún secreto está versionado en el repositorio.
- Cambiar de entorno no requiere tocar código de features, solo la selección de entorno en el build.

---

## Riesgos

- Secretos commiteados accidentalmente en `build.gradle.kts` o en un fichero de propiedades versionado.
- Build de producción usando por error valores de un entorno distinto (ej. `BASE_URL` de staging en un build de release).
- Acoplar Domain/Application al nombre de clase `BuildConfig` generado por el plugin, dificultando cambiar de herramienta en el futuro.
- Campos `expect` sin `actual` en alguna plataforma, rompiendo la compilación solo en ese target.
- El soporte multiplatform del plugin es experimental: combinaciones concretas de `useKotlinOutput()` con `com.android.library` han presentado bugs conocidos en según qué versión; fijar la versión del plugin y revisar su changelog antes de actualizar.

---

## Anti-patrones

- `const val API_KEY = "sk_live_..."` literal en un fichero de Kotlin versionado.
- `val baseUrl = if (isDebugBuild) "https://dev..." else "https://api.citovision.com"` repetido en varios ficheros en lugar de un único `AppConfig.baseUrl`.
- Leer `BuildConfig.BASE_URL` directamente desde un `UseCase` de Application en lugar de a través de `AppConfig`.
- `local.properties`/`.env` commiteado al repositorio sin estar en `.gitignore`.

---

## Comandos útiles

```kotlin
// shared/build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("com.github.gmazzo.buildconfig") version "6.0.2"
}

val appEnv = (project.findProperty("appEnv") as String?) ?: "dev"

val environments = mapOf(
    "dev" to mapOf("baseUrl" to "https://dev.api.citovision.com"),
    "staging" to mapOf("baseUrl" to "https://staging.api.citovision.com"),
    "prod" to mapOf("baseUrl" to "https://api.citovision.com")
)

val currentEnv = environments[appEnv] ?: error("Entorno desconocido: $appEnv")

buildConfig {
    buildConfigField("ENVIRONMENT", appEnv)
    buildConfigField("BASE_URL", currentEnv["baseUrl"]!!)
    buildConfigField(
        "API_KEY",
        System.getenv("CITOVISION_API_KEY") ?: error("Falta la variable de entorno CITOVISION_API_KEY")
    )
}
```

```bash
# Seleccionar entorno al construir
./gradlew :androidApp:assembleRelease -PappEnv=prod
./gradlew :shared:allTests -PappEnv=dev
```

---

## Salida esperada

```text
core/
  config/
    AppConfig.kt              → envuelve el BuildConfig generado
shared/
  build.gradle.kts             → buildConfig { ... } por entorno/plataforma
local.properties.example         → plantilla sin valores reales (versionada)
local.properties                   → valores reales (NO versionado, en .gitignore)
```

---

## Ejemplos

### Correcto — AppConfig envolviendo el BuildConfig generado

```kotlin
// core/config/AppConfig.kt
object AppConfig {
    val environment: String = BuildConfig.ENVIRONMENT
    val baseUrl: String = BuildConfig.BASE_URL
    val apiKey: String = BuildConfig.API_KEY
}
```

### Correcto — uso en ktor-client (ver ktor-client)

```kotlin
// infrastructure/network/HttpClientFactory.kt
fun createHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    // ... Logging, ContentNegotiation, HttpTimeout, etc.
    defaultRequest {
        url(AppConfig.baseUrl)
    }
}
```

### Correcto — plantilla de variables locales (versionada, sin valores reales)

```properties
# local.properties.example — versionado en el repositorio
CITOVISION_API_KEY=
```

```properties
# local.properties — NO versionado (.gitignore)
CITOVISION_API_KEY=sk_live_real_value_aqui
```

### Incorrecto

```kotlin
// ❌ Secreto hardcodeado y versionado
const val API_KEY = "sk_live_51HhActualSecretKey..."

// ❌ Lógica de entorno repetida en cada feature en lugar de centralizada
val baseUrl = if (isDebugBuild) "https://dev.api.citovision.com" else "https://api.citovision.com"

// ❌ Acceso directo al BuildConfig generado desde Application
class GetUserUseCase(private val httpClient: HttpClient) {
    suspend fun call() = httpClient.get(BuildConfig.BASE_URL + "/user")
}
```

---

## Referencias

- clean-architecture-kmp
- gradle-conventions-kmp
- ktor-client
- napier-logging-kmp
- Documentación oficial: https://github.com/gmazzo/gradle-buildconfig-plugin
