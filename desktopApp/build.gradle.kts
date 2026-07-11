import io.gitlab.arturbosch.detekt.Detekt

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

// --- Build config: Web API key de Firebase para la auth Desktop (Fase 2, ADR-0002) ---
// Se lee de local.properties (no versionado) o de la variable de entorno FIREBASE_WEB_API_KEY y se
// hornea en un objeto generado. No es un secreto de servidor: identifica el proyecto Firebase; la
// autorización real vive en las reglas de seguridad. Se usan providers (texto crudo + env) y el
// parseo se hace en la ejecución de la task, no en configuración, para ser compatible con el
// configuration cache (sin referencias a objetos del script en los providers serializados).
val localPropertiesText =
    providers
        .fileContents(rootProject.layout.projectDirectory.file("local.properties"))
        .asText
        .orElse("")

val firebaseWebApiKeyFromEnv = providers.environmentVariable("FIREBASE_WEB_API_KEY").orElse("")

val generateDesktopBuildConfig by tasks.registering {
    val propertiesText = localPropertiesText
    val envApiKey = firebaseWebApiKeyFromEnv
    val outputDir = layout.buildDirectory.dir("generated/buildConfig")
    inputs.property("localProperties", propertiesText)
    inputs.property("envApiKey", envApiKey)
    outputs.dir(outputDir)
    doLast {
        val fromProperties =
            propertiesText
                .get()
                .lineSequence()
                .map { it.trim() }
                .firstOrNull { it.startsWith("firebaseWebApiKey=") }
                ?.substringAfter('=')
                ?.trim()
                .orEmpty()
        val apiKey = fromProperties.ifEmpty { envApiKey.get() }
        val file = outputDir.get().asFile.resolve("dev/lovelace/citovision/config/DesktopBuildConfig.kt")
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package dev.lovelace.citovision.config
            |
            |internal object DesktopBuildConfig {
            |    const val FIREBASE_WEB_API_KEY: String = "$apiKey"
            |}
            |
            """.trimMargin(),
        )
    }
}

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting {
            kotlin.srcDir(generateDesktopBuildConfig)
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(libs.kotlinx.coroutines.swing)
                // initKoin { properties(...) } usa el receptor KoinApplication → koin-core en el classpath.
                implementation(libs.koin.core)
                // Napier en el classpath del entry point para inicializar el logging (Napier.base).
                implementation(libs.napier)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "dev.lovelace.citovision.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
            )
            packageName = "citoVIsion"
            packageVersion = "1.0.0"
        }
    }
}

ktlint {
    outputToConsole.set(true)
    ignoreFailures.set(false)
    filter {
        exclude { it.file.path.contains("build/") }
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$rootDir/config/detekt/detekt.yml")
}

tasks.withType<Detekt>().configureEach {
    reports {
        xml.required.set(true)
        html.required.set(true)
        txt.required.set(false)
    }
}
