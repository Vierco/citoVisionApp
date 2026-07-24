import io.gitlab.arturbosch.detekt.Detekt

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.mokkery)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kover)
}

// Esquema versionado de Room, para poder escribir migraciones (SPEC-0004 RNF-9).
room {
    schemaDirectory("$projectDir/schemas")
}

// Cobertura de tests (TESTING.md §2). Kover solo instrumenta los targets JVM: mide `commonTest` y los tests
// de Android/Desktop, NO los de iOS. Se excluye únicamente código generado, que falsearía el porcentaje sin
// aportar información; la UI Compose SÍ cuenta, así que el número refleja también la deuda de tests de UI.
kover {
    reports {
        filters {
            excludes {
                // Recursos y singletons que genera Compose.
                packages("citovision.shared.generated.resources")
                classes("*ComposableSingletons*")
                // Implementaciones de DAO y base de datos generadas por Room.
                classes("*_Impl")
                // Serializadores generados por kotlinx.serialization.
                classes("*\$\$serializer")
                // Para medir solo la lógica, dejando fuera toda la UI, descomentar:
                // annotatedBy("androidx.compose.runtime.Composable")
            }
        }
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    // Sin `jvmTarget` explícito, el bytecode saldría con la versión del JDK que ejecute el build (la JBR de
    // Android Studio es 21), y entonces el runtime que empaqueta `jpackage` puede no poder cargarlo:
    // «UnsupportedClassVersionError: class file version 65.0». Fijarlo hace la salida reproducible.
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        // `kotlin.time.Instant`/`Clock` siguen marcados como experimentales en Kotlin 2.2.x.
        all {
            languageSettings.optIn("kotlin.time.ExperimentalTime")
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.preview)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.navigation.compose)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.napier)
            // Persistencia local no sensible (flag de sesión de invitado) — DataStore multiplataforma.
            implementation(libs.androidx.datastore)
            implementation(libs.androidx.datastore.preferences)
            // Selección de imagen (SPEC-0003): FileKit (selector nativo KMP) + Coil 3 (previsualización).
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)
            implementation(libs.coil.compose)
            // Fetcher de red para Coil 3 (imágenes remotas de Storage, SPEC-0005): usa el engine Ktor.
            implementation(libs.coil.network.ktor3)
            // Persistencia de análisis (SPEC-0004): Room Multiplatform + driver SQLite empaquetado.
            implementation(libs.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.kotlinx.datetime)
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ktor.client.mock)
            }
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.koin.android)
            implementation(libs.ktor.client.okhttp)
            // Firebase (GitLive) solo tiene variante Android/iOS/JS, no Desktop-JVM.
            // El BOM fija las versiones de los artefactos com.google.firebase:* que
            // GitLive declara sin versión (p.ej. firebase-common-ktx).
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.kmp.app)
            implementation(libs.firebase.kmp.auth)
            // Google Sign-In nativo: Credential Manager (GoogleSignInClient está deprecada).
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.google.identity.googleid)
            // Inferencia on-device (SPEC-0006 / ADR-0003): ONNX Runtime, variante Android (AAR).
            implementation(libs.onnxruntime.android)
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                // Engine de Ktor para Desktop (auth Firebase REST, Fase 2 / ADR-0002).
                implementation(libs.ktor.client.okhttp)
                // Inferencia on-device (SPEC-0006 / ADR-0003): ONNX Runtime, variante JVM (nativos empaquetados).
                implementation(libs.onnxruntime.jvm)
            }
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "dev.lovelace.citovision.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 27
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Room genera código por target, así que el procesador KSP debe declararse en cada configuración.
dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspDesktop", libs.room.compiler)
    add("kspIosX64", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

ktlint {
    android.set(true)
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
