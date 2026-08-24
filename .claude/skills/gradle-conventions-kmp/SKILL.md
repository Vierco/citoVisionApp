---
name: gradle-conventions-kmp
description: "Configurar o auditar la estructura de módulos del proyecto, añadir una dependencia/plugin nuevo al catálogo, o decidir si introducir convention plugins (build-logic)."
---

# Skill: gradle-conventions-kmp

## Objetivo

Definir las convenciones de Gradle para el proyecto Kotlin Multiplatform: organización de módulos (entry points de plataforma vs módulo de librería compartida), Version Catalog (`libs.versions.toml`) como única fuente de versiones, dónde declarar plugins de compilador (KSP, serialization, Room, Mokkery...), y cuándo introducir convention plugins (`build-logic`) para evitar duplicar configuración.

Este Skill no depende de qué librerías concretas use el proyecto; aplica independientemente de si se usa Koin, Room, Ktor, etc. — cada una de ellas ya define su propio bloque `[versions]`/`[libraries]`/`[plugins]` en sus Skills correspondientes; este Skill define cómo se integran todas esas entradas en un único catálogo coherente.

---

## Modelo conceptual

```text
MyApp/
├── androidApp/        ← módulo de aplicación Android (entry point)
│                          AGP 9.0: obligatorio separado del módulo KMP
├── iosApp/              ← proyecto Xcode (entry point iOS)
├── desktopApp/            ← módulo de aplicación Desktop (entry point: fun main())
│                              [recomendado, no obligatorio por tooling, sí por consistencia]
├── shared/                  ← módulo KMP de librería: SIN entry points,
│                                solo lógica y/o UI compartida
├── gradle/
│   └── libs.versions.toml       ← única fuente de versiones de todo el proyecto
├── settings.gradle.kts
└── build.gradle.kts
```

`shared` nunca empaqueta ni se ejecuta por sí mismo; cada plataforma con una app instalable tiene su propio módulo de entry point que lo consume como dependencia.

---

## Cuándo usarlo

- Configurar la estructura de módulos de un proyecto KMP nuevo, o actualizar uno existente a AGP 9.0.
- Añadir una nueva dependencia o plugin y decidir si va en el catálogo y cómo se nombra.
- Decidir si introducir convention plugins (`build-logic`) cuando empieza a haber duplicación entre módulos.
- Auditar que ningún entry point de plataforma viva dentro del módulo `shared`.

## Cuándo NO usarlo

- Para decidir cuándo partir `shared` en varios módulos por feature → eso correspondería a un Skill de modularización por feature, no a este.
- Para gestionar variables de entorno/API keys por flavor (dev/staging/prod) → eso correspondería a un Skill de configuración de build/entornos, no a este; este Skill solo define la organización general de Gradle.
- Para las dependencias concretas de una librería (Koin, Room, Ktor, DataStore...) → cada una ya tiene su propio Skill con su propio bloque `[versions]`/`[libraries]`/`[plugins]`; este Skill define cómo conviven todas esas entradas en un único catálogo.

---

## Dependencias

```text
- clean-architecture-kmp
```

---

## Entradas necesarias

- Targets de la app (Android, iOS, Desktop, y si aplica, Web).
- Si el proyecto comparte UI (Compose Multiplatform) en todas las plataformas o usa UI nativa en alguna (ej. SwiftUI en iOS) — afecta a si hace falta separar `sharedLogic` de `sharedUI`.
- Versión de Android Gradle Plugin del proyecto (para saber si ya aplica la separación obligatoria de Android).
- Si Desktop se va a distribuir como aplicación instalable real o solo como target de pruebas/compartición de lógica.

---

## Criterios arquitectónicos

- El módulo `shared` (o módulos de librería KMP) nunca contiene un entry point de aplicación; solo lógica y/o UI compartida.
- Cada plataforma con una app instalable tiene su propio módulo de aplicación (`androidApp`, `desktopApp`) o proyecto nativo (`iosApp`), que consume `shared` como dependencia.
- Existe un único `libs.versions.toml` en la raíz del proyecto que centraliza todas las versiones, librerías y plugins usados por cualquier módulo.
- Los convention plugins (`build-logic`) solo se introducen cuando hay duplicación real de configuración entre más de un módulo, no de forma preventiva en un proyecto de un solo módulo `shared`.

---

## Reglas

- Debe existir un único fichero `gradle/libs.versions.toml`; ningún módulo debe declarar una versión de dependencia fuera del catálogo ni hardcodeada en su `build.gradle.kts`.
- Las entradas del catálogo deben nombrarse en `kebab-case` dentro del TOML (ej. `koin-core`, `ktor-client-okhttp`), que Gradle expone como acceso anidado en camelCase (`libs.koin.core`, `libs.ktor.client.okhttp`).
- Cuando una librería publique un BOM (ej. `koin-bom`), debe usarse el BOM para fijar versiones del conjunto de artefactos relacionados, en lugar de fijar cada versión por separado.
- El módulo `shared` (KMP) nunca debe aplicar un plugin de aplicación de plataforma (`com.android.application`); solo `com.android.library` (o el plugin KMP equivalente) y `kotlin("multiplatform")`.
- El entry point de Android (`MainActivity`, `Application`) debe vivir en un módulo `androidApp` separado de `shared`, aplicando `com.android.application`; esto es **obligatorio desde AGP 9.0**, que ya no permite aplicar el plugin de aplicación Android dentro de un módulo multiplatform.
- Si Desktop se distribuye como aplicación instalable (no solo como target para compartir lógica), su entry point (`fun main()`) debe vivir en un módulo `desktopApp` separado de `shared`, siguiendo el mismo criterio que Android.
- Los plugins de compilador (KSP, `kotlin.plugin.serialization`, Room Gradle plugin, Mokkery, etc.) deben declararse en `[plugins]` del catálogo y aplicarse solo en los módulos que los necesiten.
- Antes de introducir un convention plugin (`build-logic`), debe haber al menos dos módulos con configuración de Gradle duplicada; no se crea `build-logic` de forma anticipada en un proyecto de un solo módulo `shared`.

---

## Decisiones automáticas

```text
Si el proyecto tiene una sola app por plataforma y un único módulo shared
    → no se necesita build-logic todavía; libs.versions.toml + build.gradle.kts por módulo es suficiente

Si aparece configuración de Gradle duplicada en 2+ módulos (ej. configuración de Compose, de targets KMP)
    → extraer esa configuración a un convention plugin en build-logic

Si el proyecto usa o actualiza a AGP 9.0
    → el entry point de Android debe vivir en androidApp, nunca en shared (cambio obligatorio, no opcional)

Si Desktop se distribuye como aplicación real (no solo target de lógica compartida)
    → su entry point va en un módulo desktopApp separado, igual que Android (recomendado por consistencia)

Si el proyecto decide usar UI nativa en alguna plataforma (ej. SwiftUI en iOS) en lugar de Compose Multiplatform en todas
    → separar shared en sharedLogic (sin Compose) y sharedUI (con Compose, solo para las plataformas que la usan)
```

---

## Proceso recomendado

1. Verificar que el entry point de cada plataforma con app instalable vive en su propio módulo (`androidApp`, `desktopApp`) y no en `shared`.
2. Centralizar todas las versiones/librerías/plugins en `gradle/libs.versions.toml`.
3. Usar BOMs donde existan (ej. Koin) en lugar de fijar cada artefacto por separado.
4. Aplicar los plugins de compilador (KSP, serialization, Room, Mokkery) únicamente en los módulos que los necesiten.
5. Revisar periódicamente si hay duplicación de configuración entre módulos; si aparece, extraerla a un convention plugin en `build-logic`.
6. Documentar en `ARCHITECTURE.md` cualquier cambio de estructura de módulos (ej. al introducir `desktopApp` o `build-logic`).

---

## Checklist

- [ ] Existe un único `libs.versions.toml` con todas las versiones del proyecto.
- [ ] Ningún módulo declara una versión hardcodeada fuera del catálogo.
- [ ] `shared` no aplica ningún plugin de aplicación de plataforma.
- [ ] El entry point de Android vive en `androidApp`, separado de `shared`.
- [ ] Si Desktop es una app instalable, su entry point vive en `desktopApp`.
- [ ] Los BOMs disponibles (ej. Koin) se usan en lugar de fijar versiones sueltas.
- [ ] No existe `build-logic` salvo que haya duplicación real de configuración entre módulos.

---

## Definition of Done

- El proyecto compila y empaqueta cada aplicación (Android, iOS, Desktop) de forma independiente desde su propio módulo de entry point.
- Todas las dependencias y plugins usados en cualquier módulo están declarados en `libs.versions.toml`.
- La estructura de módulos es compatible con AGP 9.0 sin cambios adicionales.

---

## Riesgos

- Entry point de Android dentro de `shared`, rompiendo la compilación al actualizar a AGP 9.0.
- Versiones de una misma librería declaradas distintas en distintos módulos por no pasar por el catálogo.
- `build-logic` introducido prematuramente, añadiendo complejidad antes de que haga falta.
- Plugins de compilador aplicados en módulos que no los necesitan, aumentando tiempos de build sin motivo.

---

## Anti-patrones

- `implementation("io.insert-koin:koin-core:4.2.1")` con versión hardcodeada en un `build.gradle.kts`, en lugar de `libs.koin.core`.
- Módulo `shared` con `plugins { id("com.android.application") }`.
- Dos módulos con bloques de configuración de Compose Multiplatform copiados y pegados en lugar de un convention plugin.
- Crear `build-logic` en un proyecto de un único módulo `shared` "por si acaso".

---

## Comandos útiles

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}

include(":androidApp", ":desktopApp", ":shared")
```

```bash
# Compilar y empaquetar cada app de forma independiente
./gradlew :androidApp:assembleDebug
./gradlew :desktopApp:run

# Ejecutar los tests del módulo compartido en todos los targets
./gradlew :shared:allTests
```

---

## Salida esperada

```text
MyApp/
├── androidApp/
├── iosApp/
├── desktopApp/
├── shared/
├── gradle/
│   └── libs.versions.toml
├── settings.gradle.kts
└── build.gradle.kts
```

---

## Ejemplos

### Correcto — versión vía catálogo, no hardcodeada

```toml
# gradle/libs.versions.toml
[versions]
koin-bom = "4.2.1"

[libraries]
koin-bom = { module = "io.insert-koin:koin-bom", version.ref = "koin-bom" }
koin-core = { module = "io.insert-koin:koin-core" }
```

```kotlin
// shared/build.gradle.kts
commonMain.dependencies {
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
}
```

### Correcto — entry point de Android separado de shared

```kotlin
// androidApp/build.gradle.kts
plugins {
    id("com.android.application")
    kotlin("android")
}

dependencies {
    implementation(project(":shared"))
}
```

```kotlin
// shared/build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("com.android.library") // nunca com.android.application
}
```

### Incorrecto

```kotlin
// ❌ Versión hardcodeada fuera del catálogo
dependencies {
    implementation("io.insert-koin:koin-core:4.2.1")
}

// ❌ Módulo shared aplicando el plugin de aplicación Android
// shared/build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("com.android.application") // rompe con AGP 9.0; debería ser com.android.library
}

// ❌ MainActivity dentro de shared en lugar de androidApp
// shared/src/androidMain/kotlin/.../MainActivity.kt
class MainActivity : ComponentActivity() { /* ... */ }
```

---

## Referencias

- clean-architecture-kmp
- Documentación oficial: https://blog.jetbrains.com/kotlin/2026/05/new-kmp-default-structure/
- Guía de migración: https://kotlinlang.org/docs/multiplatform/multiplatform-project-recommended-structure.html
