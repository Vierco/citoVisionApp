package dev.lovelace.citovision.composition.di

import dev.lovelace.citovision.presentation.viewmodels.AnalysisViewModel
import dev.lovelace.citovision.presentation.viewmodels.HistoryViewModel
import dev.lovelace.citovision.presentation.viewmodels.LoginViewModel
import dev.lovelace.citovision.presentation.viewmodels.SettingsViewModel
import dev.lovelace.citovision.presentation.viewmodels.SplashViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** ViewModels de la capa Presentation (ver Skill dependency-injection-koin). */
val presentationModule =
    module {
        viewModelOf(::SplashViewModel)
        viewModelOf(::LoginViewModel)
        viewModelOf(::SettingsViewModel)
        viewModelOf(::AnalysisViewModel)
        viewModelOf(::HistoryViewModel)
    }
