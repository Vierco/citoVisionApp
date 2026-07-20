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

// JDK que usa `jpackage` al empaquetar (createDistributable, packageDmg...). La JBR que trae Android Studio
// es un runtime recortado y no incluye `jpackage`, así que si el build corre con ella el empaquetado falla
// con «Failed to check JDK distribution: 'jpackage' is missing». Se toma de la variable de entorno
// DESKTOP_JAVA_HOME para no clavar una ruta local en el repo; si no está, se usa el JDK del build y quien
// empaquete verá ese error con la instrucción de qué exportar. No afecta a la compilación, solo al paquete.
val desktopPackagingJavaHome = providers.environmentVariable("DESKTOP_JAVA_HOME")

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
    // Mismo `jvmTarget` que el target desktop de `shared`: sin fijarlo, el bytecode hereda la versión del JDK
    // que ejecute el build y el runtime empaquetado puede no poder cargarlo (ver comentario en shared).
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

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

        desktopPackagingJavaHome.orNull?.let { javaHome = it }

        // Nombre que enseña el Dock de macOS al pasar el puntero por el icono. Sin esto sale «java»: no lo
        // cubren ni `apple.awt.application.name` (Main.kt), que solo afecta a la barra de menús, ni
        // `macOS.dockName`, que el plugin únicamente traduce a `-Xdock:name` al empaquetar con jpackage.
        // Para `./gradlew run` hay que pasarlo aquí: la task `run` hace `setJvmArgs(defaultJvmArgs + estos)`,
        // reemplazando la lista, así que configurarla desde fuera no sirve — el plugin lo sobrescribe.
        // Solo en macOS: en Windows y Linux `-Xdock:name` no es una opción reconocida y la JVM abortaría.
        if (System.getProperty("os.name").startsWith("Mac")) {
            jvmArgs += "-Xdock:name=citoVision"
        }

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
            )
            packageName = "citoVision"
            packageVersion = "1.0.0"

            // `jpackage` construye con jlink un runtime recortado, y ahí no entra `jdk.unsupported`, que es
            // donde vive `sun.misc.Unsafe`. El protobuf de DataStore lo usa al serializar preferencias, así
            // que sin este módulo la app empaquetada arranca pero revienta con NoClassDefFoundError en
            // cuanto escribe sesión: el modo invitado se queda colgado y cerrar sesión no navega a login.
            // No se nota con `run` porque ahí el JDK está completo.
            modules("jdk.unsupported")

            // El .icns se genera desde `icons/citovision-1024.png` (ver icons/README.md). Alimenta el icono
            // del .app / .dmg y, además, el plugin lo pasa como `-Xdock:icon` al ejecutar con `run`.
            //
            // `dockName` es el nombre que enseña el Dock de macOS al pasar el puntero por el icono. Sin él
            // sale «java»: no lo cubre `apple.awt.application.name` (Main.kt), que solo afecta a la barra de
            // menús, ni `packageName`, que solo nombra el paquete. El plugin lo traduce a `-Xdock:name` y lo
            // aplica tanto a `run` como a la app empaquetada; ponerlo a mano sobre la task `run` no funciona
            // porque esa task no es un `JavaExec` y además se registra después de evaluar este script.
            macOS {
                iconFile.set(project.file("icons/citovision.icns"))
                dockName = "citoVision"
            }

            // Cada sistema exige su propio formato y ninguno acepta el de los otros: .icns en macOS, .ico en
            // Windows y PNG en Linux. Los tres se generan desde `icons/citovision-1024.png`; el .ico con
            // `tools/make_windows_icon.py` (ver icons/README.md).
            windows {
                iconFile.set(project.file("icons/citovision.ico"))
            }

            linux {
                iconFile.set(project.file("icons/citovision.png"))
            }
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
