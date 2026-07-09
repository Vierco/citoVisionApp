package dev.lovelace.citovision.composition.di

import dev.lovelace.citovision.application.usecases.HasActiveSessionUseCase
import dev.lovelace.citovision.application.usecases.ObserveSessionStatusUseCase
import dev.lovelace.citovision.application.usecases.PickImageUseCase
import dev.lovelace.citovision.application.usecases.SendPasswordResetUseCase
import dev.lovelace.citovision.application.usecases.SignInAsGuestUseCase
import dev.lovelace.citovision.application.usecases.SignInWithEmailUseCase
import dev.lovelace.citovision.application.usecases.SignInWithGoogleUseCase
import dev.lovelace.citovision.application.usecases.SignOutUseCase
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
        factoryOf(::PickImageUseCase)
    }
