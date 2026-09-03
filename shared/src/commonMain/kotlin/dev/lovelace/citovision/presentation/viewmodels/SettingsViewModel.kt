package dev.lovelace.citovision.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lovelace.citovision.application.ports.UrlOpener
import dev.lovelace.citovision.application.usecases.DeleteAllAnalysesUseCase
import dev.lovelace.citovision.application.usecases.FeedbackResult
import dev.lovelace.citovision.application.usecases.ImageSourceOptionsAvailableUseCase
import dev.lovelace.citovision.application.usecases.ObserveCurrentUserUseCase
import dev.lovelace.citovision.application.usecases.ObserveImageSourceUseCase
import dev.lovelace.citovision.application.usecases.ObserveSessionStatusUseCase
import dev.lovelace.citovision.application.usecases.ObserveThemePreferenceUseCase
import dev.lovelace.citovision.application.usecases.SessionStatus
import dev.lovelace.citovision.application.usecases.SetImageSourceUseCase
import dev.lovelace.citovision.application.usecases.SetThemePreferenceUseCase
import dev.lovelace.citovision.application.usecases.SignOutUseCase
import dev.lovelace.citovision.application.usecases.SubmitFeedbackUseCase
import dev.lovelace.citovision.domain.settings.ImageSourcePreference
import dev.lovelace.citovision.domain.settings.ThemePreference
import dev.lovelace.citovision.domain.validation.isValidEmail
import dev.lovelace.citovision.presentation.events.SettingsUiEvent
import dev.lovelace.citovision.presentation.navigation.NavigationEvent
import dev.lovelace.citovision.presentation.state.SettingsUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Gestiona las acciones de sesión de la pantalla de Ajustes y expone el tipo de sesión activa para
 * mostrar la opción correcta (cuenta → cerrar sesión; invitado → iniciar sesión con cuenta).
 * Ambas acciones navegan a `LoginRoute`; el cierre de sesión limpia el flag de invitado y la cuenta
 * Firebase vía [SignOutUseCase]. También orquesta el borrado local y el envío de feedback remoto.
 */
class SettingsViewModel(
    private val signOut: SignOutUseCase,
    private val deleteAllAnalyses: DeleteAllAnalysesUseCase,
    private val submitFeedback: SubmitFeedbackUseCase,
    private val urlOpener: UrlOpener,
    private val setThemePreference: SetThemePreferenceUseCase,
    private val setImageSource: SetImageSourceUseCase,
    observeSessionStatus: ObserveSessionStatusUseCase,
    observeCurrentUser: ObserveCurrentUserUseCase,
    observeThemePreference: ObserveThemePreferenceUseCase,
    observeImageSource: ObserveImageSourceUseCase,
    imageSourceOptionsAvailable: ImageSourceOptionsAvailableUseCase,
) : ViewModel() {
    private val clearedConfirmation = MutableStateFlow(false)
    private val feedbackForm = MutableStateFlow(FeedbackForm())

    val uiState: StateFlow<SettingsUiState> =
        combine(
            observeSessionStatus(),
            observeCurrentUser(),
            clearedConfirmation,
            feedbackForm,
            // `combine` solo tipa hasta cinco flujos, así que las dos preferencias viajan juntas.
            combine(observeThemePreference(), observeImageSource(), ::UserPreferences),
        ) { status, user, cleared, feedback, preferences ->
            SettingsUiState(
                sessionStatus = status,
                email = user?.email,
                avatarUrl = user?.photoUrl,
                clearedConfirmationVisible = cleared,
                feedbackDialogVisible = feedback.visible,
                feedbackEmail = feedback.email,
                feedbackMessage = feedback.message,
                isFeedbackValid =
                    isValidEmail(feedback.email) &&
                        feedback.message.isNotBlank() &&
                        status == SessionStatus.ACCOUNT,
                feedbackSending = feedback.sending,
                feedbackSentVisible = feedback.sentVisible,
                feedbackErrorVisible = feedback.errorVisible,
                feedbackRequiresAccount = status != SessionStatus.ACCOUNT || feedback.requiresAccountVisible,
                themePreference = preferences.theme,
                imageSource = preferences.imageSource,
                imageSourceOptionsVisible = imageSourceOptionsAvailable(),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = SettingsUiState(),
        )

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            SettingsUiEvent.Login ->
                viewModelScope.launch {
                    _navigationEvents.send(NavigationEvent.ToLogin)
                }

            SettingsUiEvent.SignOut ->
                viewModelScope.launch {
                    signOut()
                    _navigationEvents.send(NavigationEvent.ToLogin)
                }

            SettingsUiEvent.ClearLocalAnalyses ->
                viewModelScope.launch {
                    deleteAllAnalyses()
                    clearedConfirmation.value = true
                }

            SettingsUiEvent.DismissClearedConfirmation ->
                clearedConfirmation.value = false

            SettingsUiEvent.OpenFeedback ->
                feedbackForm.value = FeedbackForm(visible = true, email = uiState.value.email.orEmpty())

            is SettingsUiEvent.FeedbackEmailChanged ->
                feedbackForm.update { it.copy(email = event.email) }

            is SettingsUiEvent.FeedbackMessageChanged ->
                feedbackForm.update { it.copy(message = event.message) }

            SettingsUiEvent.SubmitFeedback ->
                viewModelScope.launch {
                    feedbackForm.update {
                        it.copy(sending = true, errorVisible = false, requiresAccountVisible = false)
                    }
                    val form = feedbackForm.value
                    feedbackForm.value =
                        when (submitFeedback(form.email, form.message)) {
                            FeedbackResult.SENT -> FeedbackForm(sentVisible = true)
                            FeedbackResult.REQUIRES_ACCOUNT ->
                                form.copy(sending = false, requiresAccountVisible = true)
                            FeedbackResult.ERROR -> form.copy(sending = false, errorVisible = true)
                        }
                }

            SettingsUiEvent.CancelFeedback ->
                feedbackForm.value = FeedbackForm()

            SettingsUiEvent.DismissFeedbackSent ->
                feedbackForm.value = FeedbackForm()

            SettingsUiEvent.DismissFeedbackError ->
                feedbackForm.update { it.copy(errorVisible = false) }

            is SettingsUiEvent.OpenExternalUrl ->
                urlOpener.open(event.url)

            is SettingsUiEvent.SetTheme ->
                viewModelScope.launch { setThemePreference(event.preference) }

            is SettingsUiEvent.SetImageSource ->
                viewModelScope.launch { setImageSource(event.preference) }

            SettingsUiEvent.NavigateBack ->
                viewModelScope.launch {
                    _navigationEvents.send(NavigationEvent.Back)
                }
        }
    }

    /** Empaqueta las preferencias de Ajustes para no agotar los cinco flujos que `combine` tipa. */
    private data class UserPreferences(
        val theme: ThemePreference,
        val imageSource: ImageSourcePreference,
    )

    private data class FeedbackForm(
        val visible: Boolean = false,
        val email: String = "",
        val message: String = "",
        val sending: Boolean = false,
        val sentVisible: Boolean = false,
        val errorVisible: Boolean = false,
        val requiresAccountVisible: Boolean = false,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
