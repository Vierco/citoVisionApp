package dev.lovelace.citovision.composition.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.lovelace.citovision.application.ports.AnalysisImageStore
import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.GoogleSignInLauncher
import dev.lovelace.citovision.application.ports.ImageDecoder
import dev.lovelace.citovision.application.ports.OnnxRunner
import dev.lovelace.citovision.infrastructure.auth.DesktopFirebaseAuthService
import dev.lovelace.citovision.infrastructure.auth.StubGoogleSignInLauncher
import dev.lovelace.citovision.infrastructure.auth.remote.IdentityToolkitAuthDataSource
import dev.lovelace.citovision.infrastructure.image.OkioAnalysisImageStore
import dev.lovelace.citovision.infrastructure.image.analysisImagesPath
import dev.lovelace.citovision.infrastructure.inference.ImageDecoderImpl
import dev.lovelace.citovision.infrastructure.inference.OnnxRunnerImpl
import dev.lovelace.citovision.infrastructure.inference.loadCellDetectorModel
import dev.lovelace.citovision.infrastructure.persistence.database.AppDatabase
import dev.lovelace.citovision.infrastructure.persistence.database.appDatabaseBuilder
import dev.lovelace.citovision.infrastructure.persistence.database.createAppDatabase
import dev.lovelace.citovision.infrastructure.persistence.preferences.createDataStore
import dev.lovelace.citovision.infrastructure.persistence.preferences.dataStorePath
import dev.lovelace.citovision.infrastructure.remote.FIREBASE_WEB_API_KEY_PROPERTY
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformModule: Module =
    module {
        single {
            IdentityToolkitAuthDataSource(client = get(), apiKey = getProperty(FIREBASE_WEB_API_KEY_PROPERTY, ""))
        }
        single<AuthService> { DesktopFirebaseAuthService(remote = get()) }
        singleOf(::StubGoogleSignInLauncher) bind GoogleSignInLauncher::class
        single<HttpClientEngine> { OkHttp.create() }
        single<DataStore<Preferences>> { createDataStore { dataStorePath() } }
        single<AppDatabase> { createAppDatabase(appDatabaseBuilder()) }
        single { get<AppDatabase>().analysisDao() }
        single { get<AppDatabase>().outboxDao() }
        single<AnalysisImageStore> { OkioAnalysisImageStore(baseDirectory = analysisImagesPath()) }
        single<ImageDecoder> { ImageDecoderImpl() }
        single<OnnxRunner> { OnnxRunnerImpl(::loadCellDetectorModel) }
    }
