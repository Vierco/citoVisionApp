package dev.lovelace.citovision.composition.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.GoogleSignInLauncher
import dev.lovelace.citovision.application.ports.SessionRepository
import dev.lovelace.citovision.application.usecases.HasActiveSessionUseCase
import dev.lovelace.citovision.application.usecases.ObserveSessionStatusUseCase
import dev.lovelace.citovision.application.usecases.PickImageUseCase
import dev.lovelace.citovision.application.usecases.SendPasswordResetUseCase
import dev.lovelace.citovision.application.usecases.SignInAsGuestUseCase
import dev.lovelace.citovision.application.usecases.SignInWithEmailUseCase
import dev.lovelace.citovision.application.usecases.SignInWithGoogleUseCase
import dev.lovelace.citovision.application.usecases.SignOutUseCase
import dev.mokkery.mock
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Smoke test del grafo de DI común (application + infrastructure). El `platformModule` real depende de
 * cada plataforma (Firebase/DataStore con rutas reales), así que aquí se sustituye por uno de test que
 * aporta los puertos mockeados; se verifica que todos los use cases se resuelven sin dependencias sueltas.
 */
class AppModulesGraphTest {
    private val testPlatformModule =
        module {
            single<AuthService> { mock() }
            single<GoogleSignInLauncher> { mock() }
            single<DataStore<Preferences>> { mock() }
        }

    @Test
    fun `given the common modules when resolving the use cases then all dependencies are satisfied`() {
        // Given
        val app =
            koinApplication {
                modules(applicationModule, infrastructureModule, testPlatformModule)
            }
        val koin = app.koin

        // When / Then: si falta algún binding, get() lanzaría y el test fallaría
        assertNotNull(koin.get<SignInWithEmailUseCase>())
        assertNotNull(koin.get<SignInWithGoogleUseCase>())
        assertNotNull(koin.get<SignInAsGuestUseCase>())
        assertNotNull(koin.get<SignOutUseCase>())
        assertNotNull(koin.get<SendPasswordResetUseCase>())
        assertNotNull(koin.get<HasActiveSessionUseCase>())
        assertNotNull(koin.get<ObserveSessionStatusUseCase>())
        assertNotNull(koin.get<PickImageUseCase>())
        assertNotNull(koin.get<SessionRepository>())

        app.close()
    }
}
