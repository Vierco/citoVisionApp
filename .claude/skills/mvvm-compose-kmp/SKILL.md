---
name: mvvm-compose-kmp
description: "Crear o revisar un ViewModel, su UiState/UiEvent, o conectar un Composable con la capa Application."
---

# Skill: mvvm-compose-kmp

## Objetivo

Definir cómo implementar el patrón MVVM dentro de la capa **Presentation** en proyectos Kotlin Multiplatform con Compose Multiplatform (Android, iOS, Desktop).

Este Skill define exclusivamente cómo se estructuran ViewModel, UiState, UiEvent y su comunicación con la capa Application. No define Clean Architecture en general, ni casos de uso, ni inyección de dependencias, ni navegación: esos temas pertenecen a otros Skills.

---

## Modelo conceptual

```text
Composable (Android / iOS / Desktop)
        │  observa
        ▼
  StateFlow<UiState>  ◄────────────────  ViewModel (androidx.lifecycle.ViewModel)
        │                                       │
        │  evento de usuario                    │ viewModelScope.launch
        ▼                                       ▼
 onClick() / onEvent(UiEvent) ───────────►   UseCase (Application)
```

Flujo unidireccional:

```text
Evento de usuario → ViewModel → UseCase (Application) → nuevo UiState → recomposición
```

El ViewModel vive en `commonMain` y es el único punto de entrada de la UI hacia la lógica de aplicación.

---

## Cuándo usarlo

- Crear una nueva pantalla o feature en `presentation`.
- Definir o revisar el ViewModel de una pantalla existente.
- Decidir cómo modelar `UiState` o `UiEvent`.
- Conectar un Composable con los `UseCase` de Application.
- Revisar si un ViewModel respeta MVVM y la separación de capas.

## Cuándo NO usarlo

- Para definir las capas generales de Clean Architecture → usar `clean-architecture-kmp`.
- Para implementar casos de uso o lógica de aplicación → corresponde a Application, no a este Skill.
- Para configurar Koin u otro framework de DI → usar `dependency-injection-koin`.
- Para definir navegación entre pantallas → usar `navigation-compose-kmp`.
- Para estrategia de testing del ViewModel → usar `testing-kmp`.
- Para definir Result/Either de los UseCases → usar `result-pattern`.

---

## Dependencias

```text
- clean-architecture-kmp
- result-pattern
- dependency-injection-koin
- navigation-compose-kmp
- testing-kmp
```

---

## Entradas necesarias

- Nombre de la pantalla/feature.
- `UseCase`(s) de Application que la pantalla necesita invocar.
- Datos que debe mostrar la pantalla (para diseñar el `UiState`).
- Acciones de usuario esperadas (para diseñar `UiEvent` o funciones públicas).
- Parámetros de entrada de la pantalla, si los recibe por navegación (ej. un id).

---

## Criterios arquitectónicos

- El ViewModel es el único propietario del estado de la UI; el Composable nunca mantiene estado de negocio propio.
- El flujo de datos es siempre unidireccional: Evento → ViewModel → UseCase → UiState.
- El `UiState` debe representar en todo momento un estado completo y consistente de la pantalla, nunca fragmentos sueltos.
- El Composable solo renderiza `UiState` y emite eventos; no decide, no transforma datos de negocio.
- El ViewModel no debe conocer detalles de plataforma (Android `Context`, UIKit, AWT/Desktop, etc.).
- El ViewModel reside en `commonMain` y debe compilar igual en Android, iOS y Desktop.

---

## Reglas

- Debe extender `androidx.lifecycle.ViewModel` (artefacto multiplatform `org.jetbrains.androidx.lifecycle`), nunca una clase propia con `CoroutineScope` manual.
- Debe exponer el estado únicamente como `StateFlow<UiState>`; el `MutableStateFlow` interno debe ser siempre `private`.
- Debe lanzar toda corrutina con `viewModelScope`; nunca debe crear scopes propios ni usar `GlobalScope`.
- El `UiState` debe ser una `data class` inmutable (`val`, sin colecciones mutables expuestas).
- Los estados de carga/éxito/error deben representarse dentro de un único `UiState`, no con variables sueltas adicionales.
- Para una acción simple y aislada (ej. "incrementar", "logout", "refrescar") puede usarse una función pública directa en el ViewModel (`onIncrement()`, `onLogout()`).
- Para pantallas con varias acciones relacionadas o formularios con múltiples campos, debe modelarse un `sealed interface UiEvent` resuelto en un único `onEvent(event: UiEvent)`.
- Dentro de una misma pantalla, una misma acción no debe gestionarse unas veces como función directa y otras como `UiEvent`: el estilo se elige por acción y se mantiene.
- El Composable debe observar el estado con `collectAsStateWithLifecycle()`, no con `collectAsState()`.
- El ViewModel solo puede invocar `UseCase` de Application; nunca un `Repository` ni un cliente Ktor/SQLDelight directamente.
- El ViewModel no contiene reglas de negocio, solo orquestación de UI y mapeo a `UiState`.
- El Composable no debe instanciar el ViewModel con su constructor; debe obtenerlo vía Koin (ver `dependency-injection-koin`).
- Las acciones que disparan navegación se comunican mediante un evento de navegación, nunca con el ViewModel sosteniendo un `NavController`.

---

## Decisiones automáticas

```text
Si la acción del usuario es única, aislada y sin variantes → función pública directa en el ViewModel.

Si la pantalla tiene varias acciones relacionadas o un formulario con múltiples campos → sealed interface UiEvent + onEvent(event).

Si el dato proviene de un Flow continuo (ej. observar una tabla) → combinarlo dentro de viewModelScope con stateIn().

Si el estado necesita representar carga/éxito/error → un único UiState con campos (isLoading, errorMessage, data), nunca banderas externas al UiState.

Si la pantalla recibe parámetros de navegación (ej. un id) → inyectarlos en el constructor del ViewModel vía Koin (parametersOf), nunca leerlos y mutarlos desde el Composable.
```

---

## Proceso recomendado

1. Identificar el/los `UseCase` de Application que la pantalla necesita.
2. Definir el `UiState` como `data class` inmutable en `presentation/state`.
3. Definir, si aplica, el `UiEvent` en `presentation/events`, o las funciones públicas necesarias.
4. Crear el ViewModel en `presentation/viewmodels` extendiendo `androidx.lifecycle.ViewModel`.
5. Exponer el estado: `MutableStateFlow` privado + `StateFlow` público vía `asStateFlow()`.
6. Orquestar la llamada a los `UseCase` dentro de `viewModelScope`.
7. Crear el Composable en `presentation/screens` observando el estado con `collectAsStateWithLifecycle()`.
8. Registrar el ViewModel en el módulo Koin correspondiente (`composition/di`).
9. Revisar que no existan imports de Infrastructure ni de SDK de plataforma en el ViewModel.

---

## Checklist

- [ ] El ViewModel extiende `androidx.lifecycle.ViewModel` (artefacto multiplatform).
- [ ] El `UiState` es inmutable y representa el estado completo de la pantalla.
- [ ] El estado se expone como `StateFlow`, nunca como `MutableStateFlow` público.
- [ ] Las corrutinas se lanzan con `viewModelScope`.
- [ ] El ViewModel solo invoca `UseCase` de Application.
- [ ] El Composable observa el estado con `collectAsStateWithLifecycle()`.
- [ ] El Composable no contiene lógica de negocio ni de orquestación.
- [ ] El ViewModel se obtiene vía Koin, nunca instanciado manualmente.
- [ ] Cada acción de usuario sigue un único estilo (función directa o `UiEvent`), sin mezclarlos para la misma acción.
- [ ] El ViewModel no conoce APIs específicas de Android, iOS o Desktop.

---

## Definition of Done

- La pantalla sigue el flujo unidireccional Evento → ViewModel → UseCase → UiState → Composable.
- El ViewModel reside en `commonMain` y compila sin código condicional en Android, iOS y Desktop.
- No existen violaciones de `clean-architecture-kmp` (Presentation no accede a Infrastructure).
- El `UiState` cubre todos los estados visibles de la pantalla (carga, error, vacío, éxito).
- Los tests del ViewModel definidos según `testing-kmp` pasan, si ya están aplicados.

---

## Riesgos

- ViewModels "God object" que acumulan lógica de varias pantallas o reglas de negocio.
- `UiState` mal modelado con banderas sueltas y combinaciones imposibles (`isLoading = true` y `errorMessage != null` a la vez).
- Fugas de memoria o corrutinas huérfanas por no usar `viewModelScope`.
- Inconsistencia al mezclar funciones directas y `UiEvent` para la misma acción.
- Recomposiciones incorrectas por leer estado fuera del `StateFlow` observado.

---

## Anti-patrones

- ViewModel llamando directamente a un `Repository` o a un cliente Ktor/SQLDelight.
- Composable instanciando el ViewModel con su constructor (`LoginViewModel()` en lugar de `koinViewModel()`).
- `UiState` mutable (`var`, `MutableList`, `MutableStateFlow` expuesto públicamente).
- Lógica de negocio dentro del ViewModel en lugar de delegarla a un `UseCase`.
- Uso de `collectAsState()` en lugar de `collectAsStateWithLifecycle()`.
- ViewModel con referencia directa a `NavController`.
- Mismo evento de usuario gestionado unas veces como función directa y otras dentro de `onEvent()`.

---

## Comandos útiles

```kotlin
// shared/build.gradle.kts — commonMain
implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel:<version>")
implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:<version>")
```

> Nota: para Compose Multiplatform se usan los artefactos publicados bajo `org.jetbrains.androidx.lifecycle`, equivalentes en API a `androidx.lifecycle` pero compatibles con Android, iOS y Desktop. No usar el artefacto puro de Android (`androidx.lifecycle:lifecycle-viewmodel-compose`) en `commonMain`.

---

## Salida esperada

```text
presentation/
  viewmodels/   → LoginViewModel.kt
  state/        → LoginUiState.kt
  events/       → LoginUiEvent.kt (si aplica)
  screens/      → LoginScreen.kt
composition/
  di/           → registro del ViewModel en el módulo Koin
```

---

## Ejemplos

### Correcto — acción simple (función directa)

```kotlin
// presentation/state/CounterUiState.kt
data class CounterUiState(val count: Int = 0)

// presentation/viewmodels/CounterViewModel.kt
class CounterViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CounterUiState())
    val uiState: StateFlow<CounterUiState> = _uiState.asStateFlow()

    fun onIncrement() {
        _uiState.update { it.copy(count = it.count + 1) }
    }
}
```

### Correcto — formulario con varias acciones (UiEvent)

```kotlin
// presentation/state/LoginUiState.kt
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginSuccessful: Boolean = false
)

// presentation/events/LoginUiEvent.kt
sealed interface LoginUiEvent {
    data class EmailChanged(val value: String) : LoginUiEvent
    data class PasswordChanged(val value: String) : LoginUiEvent
    data object Submit : LoginUiEvent
}

// presentation/viewmodels/LoginViewModel.kt
class LoginViewModel(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEvent(event: LoginUiEvent) {
        when (event) {
            is LoginUiEvent.EmailChanged -> _uiState.update { it.copy(email = event.value) }
            is LoginUiEvent.PasswordChanged -> _uiState.update { it.copy(password = event.value) }
            is LoginUiEvent.Submit -> submit()
        }
    }

    private fun submit() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = loginUseCase(_uiState.value.email, _uiState.value.password)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, isLoginSuccessful = true) }
                is Result.Failure -> _uiState.update { it.copy(isLoading = false, errorMessage = result.error.message) }
            }
        }
    }
}

// presentation/screens/LoginScreen.kt
@Composable
fun LoginScreen(viewModel: LoginViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column {
        TextField(
            value = uiState.email,
            onValueChange = { viewModel.onEvent(LoginUiEvent.EmailChanged(it)) }
        )
        TextField(
            value = uiState.password,
            onValueChange = { viewModel.onEvent(LoginUiEvent.PasswordChanged(it)) }
        )
        Button(onClick = { viewModel.onEvent(LoginUiEvent.Submit) }) {
            Text("Login")
        }
        if (uiState.isLoading) CircularProgressIndicator()
        uiState.errorMessage?.let { Text(it) }
    }
}
```

### Incorrecto

```kotlin
class LoginViewModel : ViewModel() {
    val email = mutableStateOf("")              // ❌ estado mutable expuesto

    fun login() {
        viewModelScope.launch {
            val response = ktorClient.post("/login") { ... }   // ❌ acceso directo a Infrastructure
        }
    }
}

@Composable
fun LoginScreen() {
    val viewModel = LoginViewModel()             // ❌ instanciado manualmente, sin Koin
    ...
}
```

---

## Referencias

- clean-architecture-kmp
- result-pattern
- dependency-injection-koin
- navigation-compose-kmp
- testing-kmp
- ARCHITECTURE.md — sección Presentation
