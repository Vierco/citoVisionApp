package dev.lovelace.citovision.composition.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.lovelace.citovision.application.ports.AuthService
import dev.lovelace.citovision.application.ports.GoogleSignInLauncher
import dev.lovelace.citovision.infrastructure.auth.AndroidGoogleSignInLauncher
import dev.lovelace.citovision.infrastructure.auth.FirebaseAuthService
import dev.lovelace.citovision.infrastructure.auth.GOOGLE_WEB_CLIENT_ID_PROPERTY
import dev.lovelace.citovision.infrastructure.persistence.preferences.PREFERENCES_FILE_NAME
import dev.lovelace.citovision.infrastructure.persistence.preferences.createDataStore
import dev.lovelace.citovision.infrastructure.platform.ActivityProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformModule: Module =
    module {
        singleOf(::FirebaseAuthService) bind AuthService::class
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
    }
