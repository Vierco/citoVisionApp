package dev.lovelace.citovision.composition.di

import org.koin.core.module.Module

/**
 * Módulo de dependencias específicas de plataforma (ver ARCHITECTURE.md, Skill dependency-injection-koin).
 * Aporta el binding de [dev.lovelace.citovision.application.ports.AuthService]:
 * Firebase en androidMain; stub en desktopMain/iosMain.
 */
expect val platformModule: Module
