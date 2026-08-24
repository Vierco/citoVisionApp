---
name: dependency-injection-koin
description: "Configurar Koin, registrar un nuevo Repository/UseCase/ViewModel en un módulo, o resolver una dependencia específica de plataforma (Context, Keychain, drivers)."
---

# Skill: dependency-injection-koin

## Objetivo

Definir cómo configurar e inyectar dependencias con **Koin** en proyectos Kotlin Multiplatform (Android, iOS, Desktop), ubicando módulos, scopes y ViewModels dentro de la capa **Composition** definida en `clean-architecture-kmp` y `ARCHITECTURE.md`.

Este Skill se basa en **Koin 4.2** (línea estable, gestionada vía `koin-bom`), usando la **DSL clásica con referencias de constructor** (`singleOf`, `factoryOf`, `viewModelOf`). No cubre el nuevo Koin Compiler Plugin (sustituto nativo de Koin Annotations/KSP), que a la fecha de este Skill sigue en Release Candidate y no se considera apto para producción; ver nota en "Decisiones automáticas".

---

## Modelo conceptual

```text
composition/di
        │
        ├── domainModule          (normalmente vacío; solo Domain Services con estado)
        ├── applicationModule     (UseCases → factoryOf)
        ├── infrastructureModule  (Repository Impl, DataSources, cliente Ktor → singleOf + bind)
        ├── presentationModule    (ViewModels → viewModelOf)
        │
        └── platformModule (expect/actual)
                 │
        ┌────────┼─────────┐
        ▼        ▼         ▼
   androidMain  iosMain  desktopMain
   (Context,    (Keychain, (JVM FS,
   SQLDelight   Native     SQLDelight
   Android,     Driver,    JVM Driver,
   OkHttp)      Darwin)    CIO/OkHttp)
```

`initKoin()` vive en `commonMain` y ensambla todos los módulos. Cada plataforma solo aporta su `platformModule` y, si aplica, su propio Context (Android).

---

## Cuándo usarlo

- Configurar Koin desde cero en un proyecto KMP nuevo.
- Registrar un nuevo `Repository`, `UseCase`, `DataSource` o `ViewModel`.
- Resolver una dependencia específica de plataforma (Context Android, Keychain iOS, sistema de ficheros Desktop).
- Diagnosticar errores de resolución de dependencias o ciclos.
- Revisar si un módulo de Koin respeta los límites de capa.

## Cuándo NO usarlo

- Para definir las capas y sus responsabilidades en general → usar `clean-architecture-kmp`.
- Para diseñar la estructura interna del ViewModel (`UiState`, `UiEvent`) → usar `mvvm-compose-kmp`; este Skill solo cubre cómo se **inyecta**, no cómo se **construye internamente**.
- Para definir `Result`/`Either` de los `UseCase` → usar `result-pattern`.
- Para elegir o configurar la librería concreta de red o persistencia (Ktor, SQLDelight, Room) → cada una tiene su propio Skill; aquí solo se decide **dónde y cómo se inyecta**.
- Para estrategia de tests con dobles de Koin → usar `testing-kmp` (este Skill solo cubre la verificación básica del grafo).

---

## Dependencias

```text
- clean-architecture-kmp
- mvvm-compose-kmp
- repository-pattern
- testing-kmp
```

---

## Entradas necesarias

- Lista de clases a registrar: `Repository` (interfaz + implementación), `UseCase`, `DataSource`, `ViewModel`.
- Si alguna dependencia necesita parámetros dinámicos en el momento de la inyección (ej. un id de pantalla) vía `parametersOf`.
- Si existe algún binding específico de plataforma necesario (driver de SQLDelight, engine de Ktor, Keychain, DataStore).
- Versión de Koin fijada en el proyecto (`koin-bom`), para mantener coherencia entre artefactos.

---

## Criterios arquitectónicos

- `composition` es la única capa autorizada a conocer simultáneamente Domain, Application, Infrastructure y Presentation; un módulo de Koin nunca debe vivir fuera de `composition/di`.
- Un módulo de Koin no debe contener lógica de negocio ni decisiones condicionales de dominio; solo cablea instancias.
- Toda implementación de Infrastructure debe bindearse explícitamente a la interfaz (Port) definida en Domain o Application; nunca se inyecta el tipo concreto fuera de Infrastructure/Composition.
- Preferir inyección por constructor sobre `KoinComponent` + `by inject()` disperso por el código (Service Locator implícito).
- `single` para todo lo que deba vivir durante toda la app (Repositorios, cliente HTTP, base de datos). `factory` para instancias de vida corta o sin estado compartido. `viewModelOf`/`viewModel<T>()` exclusivamente para ViewModels.

---

## Reglas

- Debe fijar la versión de Koin con `koin-bom` (`io.insert-koin:koin-bom:4.2.0` o superior dentro de la línea 4.2.x), nunca versionando cada artefacto por separado.
- Debe declarar los módulos en `composition/di`, separados al menos por capa (`applicationModule`, `infrastructureModule`, `presentationModule`); `domainModule` solo si existen Domain Services con estado a inyectar.
- Debe usar las funciones de referencia de constructor (`singleOf(::Clase)`, `factoryOf(::Clase)`, `viewModelOf(::Clase)`) en lugar de lambdas manuales (`single { Clase(get(), get()) }`), salvo que la construcción requiera lógica adicional (mapeo, configuración de cliente, etc.).
- Debe bindear cada implementación de Infrastructure a su interfaz con `bind`: `singleOf(::FooRepositoryImpl) bind FooRepository::class`.
- Debe declarar los ViewModels únicamente con `viewModelOf`, nunca con `single` o `factory`.
- Debe existir una única función `initKoin()` en `commonMain`, invocada desde cada entry point de plataforma (`Application.onCreate()` en Android, wrapper Swift en iOS, `fun main()` en Desktop).
- No debe llamarse `startKoin()` más de una vez por proceso.
- Debe declarar `expect val platformModule: Module` en `commonMain` y su `actual` en `androidMain`/`iosMain`/`desktopMain` para todo binding que dependa de una API de plataforma (driver SQLDelight, engine Ktor, Keychain, Context).
- El Context de Android se registra exclusivamente mediante `androidContext(this)` dentro del bloque de `startKoin`, nunca pasado a mano como parámetro de un módulo común.
- Ninguna clase de Domain o Application debe heredar de `KoinComponent` ni importar nada de `org.koin.*`.
- Debe usar `parametersOf`/`@InjectedParam` únicamente para datos que cambian en cada creación (ej. un id de pantalla); nunca para dependencias estables que deberían resolverse por el grafo.
- Si dos implementaciones compiten por la misma interfaz, debe diferenciarse con `named()`/`qualifier`, nunca con condicionales dentro del módulo.

---

## Decisiones automáticas

```text
Si la clase no tiene estado relevante o debe ser única durante toda la app (Repository, HttpClient, base de datos)
    → singleOf

Si la clase debe crearse de nuevo en cada resolución (caso de uso simple, mapper con dependencias variables)
    → factoryOf

Si la clase extiende ViewModel
    → viewModelOf

Si la implementación requiere una API de plataforma (Context, UIApplication, java.io.File, SQLDelight Driver concreto)
    → declarar el binding en platformModule (expect/actual), nunca en un módulo de commonMain

Si dos implementaciones compiten por la misma interfaz
    → diferenciarlas con named()/qualifier

Si el proyecto aún no usa KSP ni el Koin Compiler Plugin
    → usar la DSL clásica (singleOf/factoryOf/viewModelOf) — base de este Skill

Si el proyecto decide adoptar Koin Annotations o el Compiler Plugin nativo
    → evaluarlo como evolución aparte; verificar que la versión usada esté en estado estable (GA) antes de llevarlo a producción
```

---

## Proceso recomendado

1. Definir la interfaz (Port) en Domain o Application.
2. Implementarla en Infrastructure.
3. Declarar el binding en el módulo correspondiente dentro de `composition/di` (`infrastructureModule`, `applicationModule`, etc.).
4. Declarar el ViewModel correspondiente en `presentationModule`.
5. Añadir el módulo a la lista cargada por `initKoin()`.
6. Si la dependencia requiere una API de plataforma, declarar `expect val platformModule: Module` en `commonMain` y su `actual` en cada `*Main` de plataforma.
7. Invocar `initKoin()` desde el entry point de cada plataforma.
8. Verificar el grafo con `koin-test` (`checkModules`/`verify`) antes de mergear.

---

## Checklist

- [ ] El proyecto fija la versión de Koin con `koin-bom`.
- [ ] Todos los módulos de Koin están en `composition/di`.
- [ ] Cada implementación de Infrastructure está bindeada a su interfaz de Domain/Application.
- [ ] Los ViewModels se declaran con `viewModelOf`, no con `single`/`factory`.
- [ ] Existe una única función `initKoin()` compartida en `commonMain`.
- [ ] `startKoin()` se invoca una sola vez por proceso.
- [ ] El `platformModule` usa `expect/actual` y no contiene lógica de negocio.
- [ ] Ninguna clase de Domain o Application hereda de `KoinComponent`.
- [ ] Las dependencias con datos dinámicos usan `parametersOf`/`@InjectedParam`.

---

## Definition of Done

- El grafo de dependencias arranca sin errores en Android, iOS y Desktop desde el mismo `initKoin()`.
- Cada implementación de Infrastructure está bindeada a su interfaz y no se referencia su tipo concreto fuera de Infrastructure/Composition.
- Los módulos compilan en `commonMain`, salvo el `platformModule` (`expect/actual`).
- `checkModules()`/`verify()` (si `testing-kmp` ya está aplicado) pasa sin errores.

---

## Riesgos

- Ciclos de dependencias entre módulos.
- Inyección directa de `Context`/`UIApplication`/APIs de plataforma en capas internas (Domain, Application).
- Mezcla de Service Locator (`by inject()` disperso) con inyección por constructor, generando inconsistencia.
- Módulos gigantes con todas las dependencias del proyecto en un único fichero.
- Olvidar el `bind` de la interfaz, provocando errores en tiempo de ejecución en lugar de en compilación.
- Adoptar el Koin Compiler Plugin en producción mientras esté en Release Candidate, exponiéndose a cambios de API no estables.

---

## Anti-patrones

- ViewModel declarado con `single()` o `factory()` en lugar de `viewModelOf()`.
- `UseCase` o ViewModel recibiendo el tipo concreto `FooRepositoryImpl` en vez de la interfaz `FooRepository`.
- Lógica de negocio dentro de un módulo de Koin (`single { if (...) FooA() else FooB() }` con reglas de dominio).
- `KoinComponent` + `by inject()` dentro de Domain o Application.
- `startKoin()` invocado por separado en cada plataforma con módulos distintos, en lugar de una función `initKoin()` común.
- Context de Android inyectado directamente en un `UseCase`.
- Binding de un driver de plataforma (SQLDelight, Ktor engine) declarado en `commonMain` en lugar de en el `platformModule` `actual`.

---

## Comandos útiles

```kotlin
// shared/build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(platform("io.insert-koin:koin-bom:4.2.0"))
            implementation("io.insert-koin:koin-core")
            implementation("io.insert-koin:koin-compose")
            implementation("io.insert-koin:koin-compose-viewmodel")
        }
        commonTest.dependencies {
            implementation("io.insert-koin:koin-test")
        }
        androidMain.dependencies {
            implementation("io.insert-koin:koin-android")
        }
    }
}
```

```kotlin
// commonTest — verificación básica del grafo
class KoinGraphTest : KoinTest {

    @Test
    fun `el grafo de Koin se resuelve sin errores`() {
        koinApplication {
            modules(domainModule, applicationModule, infrastructureModule, presentationModule)
        }.koin.checkModules()
    }
}
```

---

## Salida esperada

```text
composition/
  di/
    AppModule.kt              → junta y expone todos los módulos comunes
    ApplicationModule.kt      → UseCases (factoryOf)
    InfrastructureModule.kt   → Repository Impl, DataSources, cliente Ktor (singleOf + bind)
    PresentationModule.kt     → ViewModels (viewModelOf)
    PlatformModule.kt         → expect val platformModule: Module
    InitKoin.kt               → fun initKoin(platformModule: Module, appDeclaration: KoinAppDeclaration = {})

androidMain/.../composition/di/
    PlatformModule.android.kt → actual val platformModule: Module
    MyApplication.kt           → llama a initKoin { androidContext(this) }

iosMain/.../composition/di/
    PlatformModule.ios.kt      → actual val platformModule: Module
    InitKoinIos.kt             → fun doInitKoin() = initKoin(platformModule)

desktopMain/.../composition/di/
    PlatformModule.desktop.kt  → actual val platformModule: Module
```

---

## Ejemplos

### Correcto — módulos por capa

```kotlin
// composition/di/ApplicationModule.kt
val applicationModule = module {
    factoryOf(::GetUserUseCase)
    factoryOf(::LoginUseCase)
}

// composition/di/InfrastructureModule.kt
val infrastructureModule = module {
    singleOf(::UserRepositoryImpl) bind UserRepository::class
    singleOf(::AuthRepositoryImpl) bind AuthRepository::class
    single { HttpClient(get<HttpClientEngine>()) { install(ContentNegotiation) { json() } } }
}

// composition/di/PresentationModule.kt
val presentationModule = module {
    viewModelOf(::LoginViewModel)
    viewModelOf(::ProfileViewModel)
}
```

### Correcto — platformModule con expect/actual

```kotlin
// commonMain — composition/di/PlatformModule.kt
expect val platformModule: Module

// commonMain — composition/di/InitKoin.kt
fun initKoin(platformModule: Module, appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(applicationModule, infrastructureModule, presentationModule, platformModule)
    }
}
```

```kotlin
// androidMain — composition/di/PlatformModule.android.kt
actual val platformModule: Module = module {
    single<SqlDriver> { AndroidSqliteDriver(AppDatabase.Schema, get(), "app.db") }
    single<HttpClientEngine> { OkHttp.create() }
}

// androidMain — MyApplication.kt
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(platformModule) {
            androidContext(this@MyApplication)
        }
    }
}
```

```kotlin
// iosMain — composition/di/PlatformModule.ios.kt
actual val platformModule: Module = module {
    single<SqlDriver> { NativeSqliteDriver(AppDatabase.Schema, "app.db") }
    single<HttpClientEngine> { Darwin.create() }
}

// iosMain — InitKoinIos.kt
fun doInitKoin() = initKoin(platformModule)
```

```swift
// iOSApp.swift
init() {
    HelperKt.doInitKoin()
}
```

### Incorrecto

```kotlin
// ❌ ViewModel registrado como single
val presentationModule = module {
    single { LoginViewModel(get()) }
}

// ❌ UseCase dependiendo del tipo concreto, no de la interfaz
class LoginUseCase(private val repo: UserRepositoryImpl) // debería ser UserRepository

// ❌ Context inyectado directamente en Application
class GetUserUseCase(private val context: Context) // Domain/Application nunca conocen Context

// ❌ startKoin() invocado más de una vez con módulos distintos por plataforma
// Android: startKoin { modules(androidModules) }
// iOS:     startKoin { modules(iosModules) }
```

---

## Referencias

- clean-architecture-kmp
- mvvm-compose-kmp
- repository-pattern
- testing-kmp
- ARCHITECTURE.md — sección Composition
- Documentación oficial: https://insert-koin.io/docs/reference/koin-core/kmp-setup/
