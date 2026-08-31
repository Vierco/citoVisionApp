package dev.lovelace.citovision.composition.di

// `setValue:forHTTPHeaderField:` vive en la categoría NSMutableHTTPURLRequest, y Kotlin/Native expone los
// métodos de categoría como extensiones que hay que importar explícitamente.
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.lovelace.citovision.application.ports.AnalysisImageStore
import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.AuthTokenProvider
import dev.lovelace.citovision.application.ports.GoogleSignInLauncher
import dev.lovelace.citovision.application.ports.ImageDecoder
import dev.lovelace.citovision.application.ports.OnnxRunner
import dev.lovelace.citovision.application.ports.UrlOpener
import dev.lovelace.citovision.infrastructure.auth.IosGoogleSignInLauncher
import dev.lovelace.citovision.infrastructure.auth.KeychainTokenStore
import dev.lovelace.citovision.infrastructure.auth.RestFirebaseAuthService
import dev.lovelace.citovision.infrastructure.auth.TokenStore
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
import dev.lovelace.citovision.infrastructure.platform.IosUrlOpener
import dev.lovelace.citovision.infrastructure.remote.FIREBASE_WEB_API_KEY_PROPERTY
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module
import platform.Foundation.setValue

// Auth por Identity Toolkit REST, compartida con Desktop (ADR-0006): iOS no enlaza el SDK nativo de
// Firebase. Google Sign-In sigue en stub hasta que exista el puente Swift con el cliente OAuth de iOS.
actual val platformModule: Module =
    module {
        single {
            IdentityToolkitAuthDataSource(client = get(), apiKey = getProperty(FIREBASE_WEB_API_KEY_PROPERTY, ""))
        }
        // A diferencia de Desktop, la sesión sobrevive al reinicio: los tokens van al Keychain (RF-8).
        single<TokenStore> { KeychainTokenStore() }
        // Misma instancia como puerto de sesión y como proveedor del ID token para Firestore/Storage.
        single {
            RestFirebaseAuthService(remote = get(), tokenStore = get())
        }.binds(arrayOf(AuthService::class, AuthTokenProvider::class))
        singleOf(::IosGoogleSignInLauncher) bind GoogleSignInLauncher::class
        // `Accept-Encoding: identity` para pedir la respuesta sin comprimir. NSURLSession añade por su
        // cuenta `gzip` y descomprime de forma transparente, pero deja la cabecera `Content-Length` del
        // contenido comprimido; Ktor la compara con los bytes ya descomprimidos y aborta con
        // "Content-Length mismatch" (KTOR-7943). Afectaba a toda llamada REST en iOS (auth, Firestore y
        // Storage), y al caer en el catch genérico se anunciaba como falta de conexión.
        single<HttpClientEngine> {
            Darwin.create {
                configureRequest {
                    setValue("identity", forHTTPHeaderField = "Accept-Encoding")
                }
            }
        }
        single<DataStore<Preferences>> { createDataStore { dataStorePath() } }
        single<AppDatabase> { createAppDatabase(appDatabaseBuilder()) }
        single { get<AppDatabase>().analysisDao() }
        single { get<AppDatabase>().outboxDao() }
        single<AnalysisImageStore> { OkioAnalysisImageStore(baseDirectory = analysisImagesPath()) }
        single<ImageDecoder> { ImageDecoderImpl() }
        single<OnnxRunner> { OnnxRunnerImpl(::loadCellDetectorModel) }
        single<UrlOpener> { IosUrlOpener() }
    }
