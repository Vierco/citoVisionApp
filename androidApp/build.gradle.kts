import io.gitlab.arturbosch.detekt.Detekt

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.google.services)
}

// Web API key de Firebase (SIN restricción) para las llamadas REST a Firestore/Storage (SPEC-0005). Se
// lee de local.properties (no versionado) o de la variable de entorno FIREBASE_WEB_API_KEY; es la MISMA
// que usa Desktop. No es un secreto de servidor (identifica el proyecto; la autorización real está en las
// reglas de Firebase). Providers para compatibilidad con el configuration cache.
val firebaseWebApiKey: String =
    providers
        .fileContents(rootProject.layout.projectDirectory.file("local.properties"))
        .asText
        .map { text ->
            text
                .lineSequence()
                .map { it.trim() }
                .firstOrNull { it.startsWith("firebaseWebApiKey=") }
                ?.substringAfter("=")
                ?.trim()
                .orEmpty()
        }.orElse(providers.environmentVariable("FIREBASE_WEB_API_KEY"))
        .getOrElse("")

android {
    namespace = "dev.lovelace.citovision"
    compileSdk {
        version =
            release(36) {
                minorApiLevel = 1
            }
    }

    defaultConfig {
        applicationId = "dev.lovelace.citovision"
        minSdk = 27
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "FIREBASE_WEB_API_KEY", "\"$firebaseWebApiKey\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
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

dependencies {
    implementation(project(":shared"))
    implementation(libs.koin.android)
    // FileKit.init(activity) para el selector de imagen en Android (SPEC-0003).
    implementation(libs.filekit.dialogs)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
