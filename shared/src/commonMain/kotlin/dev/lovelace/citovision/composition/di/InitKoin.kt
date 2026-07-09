package dev.lovelace.citovision.composition.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * Punto único de arranque de Koin (RULES.md: "init una vez por plataforma").
 * Se invoca desde cada entry point: Application.onCreate (Android), fun main (Desktop),
 * wrapper Swift (iOS). No debe llamarse más de una vez por proceso.
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(
            applicationModule,
            infrastructureModule,
            presentationModule,
            platformModule,
        )
    }
}
