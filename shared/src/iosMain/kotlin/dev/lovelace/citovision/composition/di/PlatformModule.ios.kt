package dev.lovelace.citovision.composition.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.lovelace.citovision.application.ports.AnalysisImageStore
import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.GoogleSignInLauncher
import dev.lovelace.citovision.infrastructure.auth.StubAuthService
import dev.lovelace.citovision.infrastructure.auth.StubGoogleSignInLauncher
import dev.lovelace.citovision.infrastructure.image.OkioAnalysisImageStore
import dev.lovelace.citovision.infrastructure.image.analysisImagesPath
import dev.lovelace.citovision.infrastructure.persistence.database.AppDatabase
import dev.lovelace.citovision.infrastructure.persistence.database.appDatabaseBuilder
import dev.lovelace.citovision.infrastructure.persistence.database.createAppDatabase
import dev.lovelace.citovision.infrastructure.persistence.preferences.createDataStore
import dev.lovelace.citovision.infrastructure.persistence.preferences.dataStorePath
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

// Fase 1: iOS usa el stub (Firebase iOS requiere GoogleService-Info.plist, pendiente).
actual val platformModule: Module =
    module {
        singleOf(::StubAuthService) bind AuthService::class
        singleOf(::StubGoogleSignInLauncher) bind GoogleSignInLauncher::class
        single<HttpClientEngine> { Darwin.create() }
        single<DataStore<Preferences>> { createDataStore { dataStorePath() } }
        single<AppDatabase> { createAppDatabase(appDatabaseBuilder()) }
        single { get<AppDatabase>().analysisDao() }
        single { get<AppDatabase>().outboxDao() }
        single<AnalysisImageStore> { OkioAnalysisImageStore(baseDirectory = analysisImagesPath()) }
    }
