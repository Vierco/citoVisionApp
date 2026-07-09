package dev.lovelace.citovision.composition.di

import dev.lovelace.citovision.application.ports.ImagePicker
import dev.lovelace.citovision.application.ports.SessionRepository
import dev.lovelace.citovision.infrastructure.image.FileKitImagePicker
import dev.lovelace.citovision.infrastructure.repositories.SessionRepositoryImpl
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Implementaciones de puertos comunes a todas las plataformas. El `DataStore<Preferences>` que
 * consume [SessionRepositoryImpl] lo aporta cada [platformModule] (la ruta es específica de plataforma).
 * [FileKitImagePicker] es común porque FileKit expone API multiplataforma (SPEC-0003).
 */
val infrastructureModule =
    module {
        singleOf(::SessionRepositoryImpl) bind SessionRepository::class
        singleOf(::FileKitImagePicker) bind ImagePicker::class
    }
