package dev.lovelace.citovision.composition.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import dev.lovelace.citovision.application.ports.AnalysisImageStore
import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.GoogleSignInLauncher
import dev.lovelace.citovision.application.ports.ImageDecoder
import dev.lovelace.citovision.application.ports.OnnxRunner
import dev.lovelace.citovision.application.ports.UrlOpener
import dev.lovelace.citovision.infrastructure.auth.AndroidGoogleSignInLauncher
import dev.lovelace.citovision.infrastructure.auth.FirebaseAuthService
import dev.lovelace.citovision.infrastructure.auth.GOOGLE_WEB_CLIENT_ID_PROPERTY
import dev.lovelace.citovision.infrastructure.image.ANALYSIS_IMAGES_DIRECTORY
import dev.lovelace.citovision.infrastructure.image.OkioAnalysisImageStore
import dev.lovelace.citovision.infrastructure.inference.ImageDecoderImpl
import dev.lovelace.citovision.infrastructure.inference.OnnxRunnerImpl
import dev.lovelace.citovision.infrastructure.inference.loadCellDetectorModel
import dev.lovelace.citovision.infrastructure.persistence.database.AppDatabase
import dev.lovelace.citovision.infrastructure.persistence.database.DATABASE_NAME
import dev.lovelace.citovision.infrastructure.persistence.database.createAppDatabase
import dev.lovelace.citovision.infrastructure.persistence.preferences.PREFERENCES_FILE_NAME
import dev.lovelace.citovision.infrastructure.persistence.preferences.createDataStore
import dev.lovelace.citovision.infrastructure.platform.ActivityProvider
import dev.lovelace.citovision.infrastructure.platform.AndroidUrlOpener
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformModule: Module =
    module {
        singleOf(::FirebaseAuthService) bind AuthService::class
        single<HttpClientEngine> { OkHttp.create() }
        single { ActivityProvider() }
        single<GoogleSignInLauncher> {
            AndroidGoogleSignInLauncher(
                activityProvider = get(),
                webClientId = getProperty(GOOGLE_WEB_CLIENT_ID_PROPERTY),
            )
        }
        single<DataStore<Preferences>> {
            createDataStore {
                androidContext().filesDir.resolve(PREFERENCES_FILE_NAME).absolutePath
            }
        }
        single<AppDatabase> {
            createAppDatabase(
                Room.databaseBuilder<AppDatabase>(
                    context = androidContext(),
                    name = androidContext().getDatabasePath(DATABASE_NAME).absolutePath,
                ),
            )
        }
        single { get<AppDatabase>().analysisDao() }
        single { get<AppDatabase>().outboxDao() }
        single<AnalysisImageStore> {
            OkioAnalysisImageStore(
                baseDirectory = androidContext().filesDir.resolve(ANALYSIS_IMAGES_DIRECTORY).absolutePath,
            )
        }
        single<ImageDecoder> { ImageDecoderImpl() }
        single<OnnxRunner> { OnnxRunnerImpl(::loadCellDetectorModel) }
        single<UrlOpener> { AndroidUrlOpener(androidContext()) }
    }
