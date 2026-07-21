package dev.lovelace.citovision.composition.di

import dev.lovelace.citovision.application.usecases.AnalyzeSampleUseCase
import dev.lovelace.citovision.application.usecases.DeleteAllAnalysesUseCase
import dev.lovelace.citovision.application.usecases.DeleteAnalysisUseCase
import dev.lovelace.citovision.application.usecases.DeleteRemoteAnalysisUseCase
import dev.lovelace.citovision.application.usecases.HasActiveSessionUseCase
import dev.lovelace.citovision.application.usecases.ObserveAnalysesUseCase
import dev.lovelace.citovision.application.usecases.ObserveCurrentUserUseCase
import dev.lovelace.citovision.application.usecases.ObserveLastPatientCodeUseCase
import dev.lovelace.citovision.application.usecases.ObserveSessionStatusUseCase
import dev.lovelace.citovision.application.usecases.ObserveThemePreferenceUseCase
import dev.lovelace.citovision.application.usecases.PickImageUseCase
import dev.lovelace.citovision.application.usecases.ProcessPendingSyncUseCase
import dev.lovelace.citovision.application.usecases.SaveLastPatientCodeUseCase
import dev.lovelace.citovision.application.usecases.SearchPatientAnalysesUseCase
import dev.lovelace.citovision.application.usecases.SendPasswordResetUseCase
import dev.lovelace.citovision.application.usecases.SetThemePreferenceUseCase
import dev.lovelace.citovision.application.usecases.SignInAsGuestUseCase
import dev.lovelace.citovision.application.usecases.SignInWithEmailUseCase
import dev.lovelace.citovision.application.usecases.SignInWithGoogleUseCase
import dev.lovelace.citovision.application.usecases.SignOutUseCase
import dev.lovelace.citovision.application.usecases.SubmitFeedbackUseCase
import dev.lovelace.citovision.application.usecases.SyncAnalysisUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/** Casos de uso de la capa Application (ver Skill dependency-injection-koin). */
val applicationModule =
    module {
        factoryOf(::SignInWithEmailUseCase)
        factoryOf(::SignInWithGoogleUseCase)
        factoryOf(::SignInAsGuestUseCase)
        factoryOf(::SignOutUseCase)
        factoryOf(::SendPasswordResetUseCase)
        factoryOf(::HasActiveSessionUseCase)
        factoryOf(::ObserveSessionStatusUseCase)
        factoryOf(::ObserveCurrentUserUseCase)
        factoryOf(::PickImageUseCase)
        factoryOf(::ObserveAnalysesUseCase)
        factoryOf(::DeleteAnalysisUseCase)
        factoryOf(::DeleteAllAnalysesUseCase)
        // Análisis real con el modelo on-device (SPEC-0006), sustituye al andamiaje mock.
        factoryOf(::AnalyzeSampleUseCase)
        factoryOf(::SyncAnalysisUseCase)
        factoryOf(::ProcessPendingSyncUseCase)
        factoryOf(::SearchPatientAnalysesUseCase)
        factoryOf(::DeleteRemoteAnalysisUseCase)
        factoryOf(::SubmitFeedbackUseCase)
        factoryOf(::ObserveThemePreferenceUseCase)
        factoryOf(::SetThemePreferenceUseCase)
        factoryOf(::ObserveLastPatientCodeUseCase)
        factoryOf(::SaveLastPatientCodeUseCase)
    }
