---
name: ktor-client
description: "Configurar el cliente HTTP, implementar un RemoteDataSource, decidir plugins (logging, timeout, auth) o mapear una excepción de red a un error de dominio."
---

# Skill: ktor-client

## Objetivo

Definir cómo configurar y usar **Ktor Client** (multiplatform) para consumir APIs REST desde Infrastructure en proyectos Kotlin Multiplatform (Android, iOS, Desktop): engine por plataforma, serialización, plugins comunes (logging, timeout, reintentos, autenticación) y manejo de errores de red.

Este Skill se basa en **Ktor 3.5.x** (línea estable) con **kotlinx.serialization** para JSON. Cubre exclusivamente el **cliente** HTTP (consumir APIs ya existentes); no cubre Ktor Server (crear APIs), que sería un Skill independiente.

---

## Modelo conceptual

```text
infrastructure/network
        │
        ├── commonMain
        │     HttpClient(engine) {                  ← un único bloque de configuración
        │         install(Logging) { ... }            ← SIEMPRE antes de ContentNegotiation
        │         install(ContentNegotiation) { json(...) }
        │         install(HttpTimeout) { ... }
        │         install(HttpRequestRetry) { ... }
        │         install(Auth) { bearer { ... } }     ← solo si la API requiere autenticación
        │     }
        │     ItemRemoteDataSource(client: HttpClient) ← usa el client inyectado, captura excepciones
        │
        ├── androidMain  → HttpClientEngine = OkHttp.create()
        ├── iosMain      → HttpClientEngine = Darwin.create()
        └── desktopMain  → HttpClientEngine = OkHttp.create() (o CIO.create())
```

El `HttpClient` se construye una sola vez en `commonMain`; el engine es lo único específico de plataforma y se inyecta vía el `platformModule` de Koin (ver `dependency-injection-koin`).

---

## Cuándo usarlo

- Configurar el cliente HTTP por primera vez en el módulo `shared`.
- Implementar un `RemoteDataSource` (ver `repository-pattern`).
- Decidir qué plugins instalar (ContentNegotiation, Logging, Timeout, Retry, Auth).
- Mapear una excepción de red a un `DomainError`.
- Revisar si una llamada HTTP respeta los límites de capa.

## Cuándo NO usarlo

- Para crear o exponer una API (backend) → sería un Skill de Ktor Server, no este.
- Para decidir cómo se combina la red con la caché local → usar `repository-pattern`.
- Para tipar el error de la operación → usar `result-pattern`; este Skill captura excepciones de Ktor, pero el tipo `DomainError` final se define allí.
- Para inyectar el `HttpClient`/engine con Koin → usar `dependency-injection-koin`; este Skill define el contenido de cada módulo, no cómo se registra.

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

- Base URL de la API y entornos (dev/staging/prod), si aplica.
- Forma de los DTO a serializar/deserializar.
- Si la API requiere autenticación (token, API key) y cómo se obtiene/renueva.
- Política de timeouts y reintentos esperada por el proyecto.

---

## Criterios arquitectónicos

- El `HttpClient` se configura una única vez en `commonMain`; los plugins (ContentNegotiation, Logging, Timeout, Retry, Auth) se instalan ahí, no en cada `RemoteDataSource`.
- El motor (`HttpClientEngine`) es lo único específico de plataforma; se inyecta desde el `platformModule` de Koin, nunca se hardcodea dentro del `HttpClient` común.
- El `RemoteDataSource` es el único lugar de Infrastructure que conoce al `HttpClient`; Application/Presentation nunca lo importan.
- Las excepciones de Ktor (`ClientRequestException`, `ServerResponseException`, `HttpRequestTimeoutException`, `IOException`) se capturan dentro del `RemoteDataSource`/Repository y se traducen a `DomainError` (ver `result-pattern`); nunca se propagan tal cual.
- Los DTO de red nunca cruzan el límite de Infrastructure; se mapean siempre a la Entity de Domain.

---

## Reglas

- Debe fijar la versión de Ktor en `libs.versions.toml` (a fecha de este Skill: línea estable `3.5.x`).
- Debe usar `kotlinx.serialization` como librería de serialización, instalando `ContentNegotiation` con `json(...)`.
- Debe instalar el plugin `Logging` **antes** que `ContentNegotiation` en el bloque de configuración del `HttpClient` (orden requerido por Ktor; instalarlo después puede perder el log del cuerpo de la petición/respuesta).
- Debe configurar `HttpTimeout` explícitamente (`requestTimeoutMillis`, `connectTimeoutMillis`, `socketTimeoutMillis`); no debe depender de los valores por defecto del engine.
- Debe sanitizar las cabeceras sensibles en el logger (ej. `Authorization`) para no loguear tokens en claro.
- El engine debe declararse en el `platformModule` (`OkHttp` en Android, `Darwin` en iOS, `OkHttp`/`CIO` en Desktop) e inyectarse en el `HttpClient` común; el `HttpClient` de `commonMain` nunca debe importar un engine concreto.
- Si la API requiere autenticación, debe usarse el plugin `Auth` con `bearer { }`, gestionando `loadTokens`/`refreshTokens` ahí; nunca debe añadirse el header `Authorization` manualmente en cada llamada.
- Si la API no requiere autenticación, no debe instalarse el plugin `Auth` "por si en el futuro hace falta".
- Cada `RemoteDataSource` debe capturar las excepciones de Ktor y técnicas (`ClientRequestException`, `ServerResponseException`, `IOException`) y traducirlas según el contrato que espera el Repository (ver `result-pattern`); debe relanzar siempre `CancellationException`.
- El `RemoteDataSource` nunca debe exponer `HttpResponse` ni tipos de Ktor fuera de Infrastructure; siempre debe devolver el DTO ya deserializado.
- No debe crearse más de un `HttpClient` por aplicación salvo necesidad explícita (ej. un servicio de terceros con configuración incompatible).

---

## Decisiones automáticas

```text
El proyecto usa engines nativos por plataforma: OkHttp (Android), Darwin (iOS), OkHttp (Desktop)
    → decisión fijada para este proyecto; no usar CIO salvo que se decida lo contrario explícitamente

Si la API requiere autenticación
    → instalar el plugin Auth con bearer { loadTokens {} ; refreshTokens {} }

Si la API no requiere autenticación
    → omitir el plugin Auth por completo

Si una llamada falla por red (sin conexión, timeout)
    → el RemoteDataSource la traduce a DomainError.NetworkUnavailable (ver result-pattern)

Si una llamada falla por un error de servidor (5xx) o de cliente (4xx)
    → el RemoteDataSource distingue el código de estado para mapear a un DomainError más específico cuando aporte valor (ej. 401 → InvalidCredentials, 404 → UserNotFound)
```

---

## Proceso recomendado

1. Definir la base URL y los DTO de la API a consumir.
2. Configurar el `HttpClient` común en `infrastructure/network` (`commonMain`): `Logging`, `ContentNegotiation`, `HttpTimeout`, `HttpRequestRetry` y, si aplica, `Auth`.
3. Declarar el `HttpClientEngine` en el `platformModule` de cada plataforma (ver `dependency-injection-koin`).
4. Implementar el `RemoteDataSource` correspondiente (ver `repository-pattern`), inyectando el `HttpClient`.
5. Capturar las excepciones de Ktor dentro del `RemoteDataSource`/Repository y traducirlas a `DomainError`.
6. Registrar el `HttpClient` y el engine en Koin.
7. Cubrir con tests usando `MockEngine` (ver `testing-kmp`) los casos de éxito, error 4xx/5xx y timeout.

---

## Checklist

- [ ] El `HttpClient` se configura una sola vez en `commonMain`.
- [ ] `Logging` está instalado antes que `ContentNegotiation`.
- [ ] `HttpTimeout` está configurado explícitamente.
- [ ] El engine es lo único específico de plataforma, inyectado vía `platformModule`.
- [ ] Las cabeceras sensibles están sanitizadas en el logger.
- [ ] El `RemoteDataSource` traduce las excepciones de Ktor a `DomainError`, nunca las propaga tal cual.
- [ ] Ningún `HttpResponse`/tipo de Ktor sale de Infrastructure.
- [ ] El plugin `Auth` solo está instalado si la API realmente lo requiere.

---

## Definition of Done

- El cliente HTTP funciona de forma idéntica en Android, iOS y Desktop, con la misma configuración de `commonMain`.
- Toda llamada de red que puede fallar termina traducida a un `DomainError` tipado antes de llegar a Application.
- No hay tokens ni datos sensibles en los logs.
- Los tests cubren éxito, error de cliente/servidor y timeout con `MockEngine`.

---

## Riesgos

- Loguear el header `Authorization` u otros datos sensibles en texto plano.
- Instalar `Logging` después de `ContentNegotiation`, perdiendo el log del cuerpo de la petición/respuesta.
- Dejar timeouts en los valores por defecto del engine, dando lugar a comportamientos distintos entre Android/iOS/Desktop.
- DTO/`HttpResponse` filtrándose a Application/Presentation.
- Instanciar varios `HttpClient` sin necesidad real, desperdiciando conexiones y duplicando configuración.

---

## Anti-patrones

- Añadir el header `Authorization` manualmente en cada llamada en lugar de usar el plugin `Auth`.
- Capturar `Exception` genérica en el `RemoteDataSource` y relanzarla sin mapear a `DomainError`.
- Crear el `HttpClient` con el engine hardcodeado (`HttpClient(OkHttp)`) directamente en `commonMain`.
- Exponer `HttpResponse` o `ClientRequestException` como tipo de retorno público de un método de Infrastructure.
- Instalar el plugin `Auth` con un `bearer {}` vacío en una API que no lo necesita "por si en el futuro hace falta".

---

## Comandos útiles

```toml
# libs.versions.toml
[versions]
ktor = "3.5.1"

[libraries]
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-logging = { module = "io.ktor:ktor-client-logging", version.ref = "ktor" }
ktor-client-auth = { module = "io.ktor:ktor-client-auth", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
```

```kotlin
// shared/build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.auth) // solo si la API requiere autenticación
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        jvmMain.dependencies { // Desktop
            implementation(libs.ktor.client.okhttp)
        }
    }
}
```

---

## Salida esperada

```text
infrastructure/
  network/
    HttpClientFactory.kt        → fun createHttpClient(engine: HttpClientEngine): HttpClient (commonMain)
    ItemRemoteDataSource.kt      → consume el HttpClient inyectado
```

---

## Ejemplos

### Correcto — HttpClient común en commonMain

```kotlin
// infrastructure/network/HttpClientFactory.kt — commonMain
fun createHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    install(Logging) {
        level = LogLevel.INFO
        sanitizeHeader { header -> header == HttpHeaders.Authorization }
    }
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 15_000
    }
    install(HttpRequestRetry) {
        retryOnServerErrors(maxRetries = 2)
        exponentialDelay()
    }
    // Solo si la API requiere autenticación:
    install(Auth) {
        bearer {
            loadTokens { tokenStorage.loadTokens() }
            refreshTokens { tokenStorage.refreshTokens(oldTokens) }
        }
    }
    defaultRequest {
        url(ApiConfig.BASE_URL)
    }
}
```

### Correcto — engine por plataforma (ver dependency-injection-koin)

```kotlin
// androidMain
actual val platformModule: Module = module {
    single<HttpClientEngine> { OkHttp.create() }
}

// iosMain
actual val platformModule: Module = module {
    single<HttpClientEngine> { Darwin.create() }
}

// desktopMain (jvmMain)
actual val platformModule: Module = module {
    single<HttpClientEngine> { OkHttp.create() }
}
```

### Correcto — RemoteDataSource (ver repository-pattern)

```kotlin
class KtorItemRemoteDataSource(private val client: HttpClient) : ItemRemoteDataSource {
    override suspend fun fetchItems(): List<ItemDto> = client.get("items").body()
    override suspend fun createItem(item: ItemDto): ItemDto = client.post("items") { setBody(item) }.body()
    override suspend fun deleteItem(id: String) { client.delete("items/$id") }
}
```

### Correcto — mapeo de errores en el Repository (ver result-pattern)

```kotlin
override suspend fun refresh(): Result<Unit, DomainError> = try {
    val dtos = remote.fetchItems()
    local.replaceAll(dtos.map { it.toEntity() })
    Result.Success(Unit)
} catch (e: CancellationException) {
    throw e
} catch (e: ClientRequestException) {
    if (e.response.status == HttpStatusCode.Unauthorized) {
        Result.Failure(DomainError.InvalidCredentials)
    } else {
        Result.Failure(DomainError.Unexpected(e.message ?: "client error"))
    }
} catch (e: ServerResponseException) {
    Result.Failure(DomainError.Unexpected("server error"))
} catch (e: HttpRequestTimeoutException) {
    Result.Failure(DomainError.NetworkUnavailable)
} catch (e: IOException) {
    Result.Failure(DomainError.NetworkUnavailable)
}
```

### Incorrecto

```kotlin
// ❌ Engine hardcodeado en commonMain — OkHttp no existe en iOS, rompe la compilación común
val client = HttpClient(OkHttp) { ... }

// ❌ Logging después de ContentNegotiation — pierde el log del cuerpo de la petición/respuesta
install(ContentNegotiation) { json() }
install(Logging) { ... }

// ❌ Token añadido a mano en cada llamada en lugar de usar el plugin Auth
client.get("items") {
    header("Authorization", "Bearer $token")
}

// ❌ Excepción de Ktor propagada sin mapear
override suspend fun refresh(): Result<Unit, DomainError> {
    val dtos = remote.fetchItems() // si lanza ClientRequestException, rompe sin Result.Failure
    ...
}
```

---

## Referencias

- clean-architecture-kmp
- repository-pattern
- result-pattern
- dependency-injection-koin
- Documentación oficial: https://ktor.io/docs/client-engines.html
