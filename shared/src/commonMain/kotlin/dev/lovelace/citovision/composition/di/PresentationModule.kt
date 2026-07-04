package dev.lovelace.citovision.composition.di

import dev.lovelace.citovision.presentation.viewmodels.LoginViewModel
import dev.lovelace.citovision.presentation.viewmodels.SplashViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel { SplashViewModel() }
    viewModel { LoginViewModel() }
}
