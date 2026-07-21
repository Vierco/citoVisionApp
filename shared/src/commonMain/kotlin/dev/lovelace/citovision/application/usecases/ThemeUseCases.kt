package dev.lovelace.citovision.application.usecases

import dev.lovelace.citovision.application.ports.ThemeRepository
import dev.lovelace.citovision.domain.settings.ThemePreference
import kotlinx.coroutines.flow.Flow

/** Observa la preferencia de tema persistida (Claro/Oscuro/Seguir sistema). */
class ObserveThemePreferenceUseCase(
    private val themeRepository: ThemeRepository,
) {
    operator fun invoke(): Flow<ThemePreference> = themeRepository.themePreference()
}

/** Guarda la preferencia de tema elegida por el usuario. */
class SetThemePreferenceUseCase(
    private val themeRepository: ThemeRepository,
) {
    suspend operator fun invoke(preference: ThemePreference) = themeRepository.setThemePreference(preference)
}
