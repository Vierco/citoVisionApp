package dev.lovelace.citovision.composition.di

import dev.lovelace.citovision.application.ports.AnalysisRepository
import dev.lovelace.citovision.application.ports.CellDetector
import dev.lovelace.citovision.application.ports.ImagePicker
import dev.lovelace.citovision.application.ports.PatientCodeRepository
import dev.lovelace.citovision.application.ports.RemoteAnalysisSync
import dev.lovelace.citovision.application.ports.RemoteFeedback
import dev.lovelace.citovision.application.ports.RemotePatientAnalyses
import dev.lovelace.citovision.application.ports.SessionRepository
import dev.lovelace.citovision.application.ports.ThemeRepository
import dev.lovelace.citovision.core.coroutines.defaultDispatcher
import dev.lovelace.citovision.infrastructure.image.FileKitImagePicker
import dev.lovelace.citovision.infrastructure.inference.CellDetectorImpl
import dev.lovelace.citovision.infrastructure.network.createHttpClient
import dev.lovelace.citovision.infrastructure.remote.FIREBASE_PROJECT_ID
import dev.lovelace.citovision.infrastructure.remote.FIREBASE_STORAGE_BUCKET
import dev.lovelace.citovision.infrastructure.remote.FIREBASE_WEB_API_KEY_PROPERTY
import dev.lovelace.citovision.infrastructure.remote.RemoteAnalysisSyncImpl
import dev.lovelace.citovision.infrastructure.remote.firestore.FirestoreAnalysisDataSource
import dev.lovelace.citovision.infrastructure.remote.firestore.FirestoreFeedbackDataSource
import dev.lovelace.citovision.infrastructure.remote.storage.FirebaseStorageDataSource
import dev.lovelace.citovision.infrastructure.repositories.AnalysisRepositoryImpl
import dev.lovelace.citovision.infrastructure.repositories.PatientCodeRepositoryImpl
import dev.lovelace.citovision.infrastructure.repositories.SessionRepositoryImpl
import dev.lovelace.citovision.infrastructure.repositories.ThemeRepositoryImpl
import io.ktor.client.HttpClient
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Implementaciones de puertos comunes a todas las plataformas. El `DataStore<Preferences>`, el
 * `AnalysisDao`, el `AnalysisImageStore` y el `HttpClientEngine` los aporta cada [platformModule]
 * (rutas / engine son de plataforma). [FileKitImagePicker] es común porque FileKit expone API
 * multiplataforma (SPEC-0003). El `HttpClient` es único (RULES.md §Networking) y toma el engine inyectado.
 */
val infrastructureModule =
    module {
        singleOf(::SessionRepositoryImpl) bind SessionRepository::class
        singleOf(::ThemeRepositoryImpl) bind ThemeRepository::class
        singleOf(::PatientCodeRepositoryImpl) bind PatientCodeRepository::class
        singleOf(::FileKitImagePicker) bind ImagePicker::class
        singleOf(::AnalysisRepositoryImpl) bind AnalysisRepository::class
        // Orquestador de inferencia común; el ImageDecoder y el OnnxRunner los aporta cada platformModule.
        single<CellDetector> {
            CellDetectorImpl(imageDecoder = get(), onnxRunner = get(), dispatcher = defaultDispatcher)
        }
        single<HttpClient> { createHttpClient(get()) }
        single {
            FirestoreAnalysisDataSource(
                client = get(),
                projectId = FIREBASE_PROJECT_ID,
                apiKey = getProperty(FIREBASE_WEB_API_KEY_PROPERTY, ""),
            )
        }
        single<RemotePatientAnalyses> { get<FirestoreAnalysisDataSource>() }
        single<RemoteFeedback> {
            FirestoreFeedbackDataSource(
                client = get(),
                projectId = FIREBASE_PROJECT_ID,
                apiKey = getProperty(FIREBASE_WEB_API_KEY_PROPERTY, ""),
            )
        }
        single { FirebaseStorageDataSource(client = get(), bucket = FIREBASE_STORAGE_BUCKET) }
        single<RemoteAnalysisSync> {
            RemoteAnalysisSyncImpl(
                outboxDao = get(),
                analysisDao = get(),
                imageStore = get(),
                firestore = get(),
                storage = get(),
            )
        }
    }
