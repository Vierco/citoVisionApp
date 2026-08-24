---
name: navigation-compose-kmp
description: "Definir cómo navegar entre pantallas de una nueva feature, elegir entre Navigation 3 y Navigation 2 Compose Multiplatform, o implementar el paso de parámetros entre pantallas."
---

# Skill: navigation-compose-kmp

## Objetivo

Definir cómo implementar la navegación entre pantallas en `presentation/navigation` en un proyecto Kotlin Multiplatform (Android, iOS, Desktop).

Este Skill documenta **dos enfoques** disponibles a la fecha de su redacción:

- **Opción A — Navigation 3** (`androidx.navigation3`): estable en Android (línea `1.1.x`); su soporte multiplatform (UI, adaptive, scoping de ViewModel) sigue en **alpha** para iOS/Desktop a fecha de este Skill.
- **Opción B — Navigation 2 Compose Multiplatform** (`org.jetbrains.androidx.navigation:navigation-compose`): estable en Android, iOS y Desktop.

Este Skill no decide por sí solo cuál usar: exige comprobar el estado de madurez actual antes de elegir (ver "Decisiones automáticas").

---

## Modelo conceptual

```text
presentation/navigation
        │
        ├── routes/        ← @Serializable data class/object (comunes a ambos enfoques)
        │
        ├── ¿Navigation 3 multiplatform ya estable en los targets del proyecto?
        │        │
        │   Sí ──┴── No
        │   │         │
        │   ▼         ▼
        │ Opción A      Opción B
        │ NavBackStack   NavController
        │ NavDisplay     NavHost
        │ entryProvider  composable<T>()
        │
        └── El ViewModel emite un NavigationEvent
                  │
                  ▼
        El contenedor de navegación (NavDisplay/NavHost) lo colecta
        y lo traduce en backStack.add()/navController.navigate()
```

El ViewModel nunca conoce `NavController`/`NavBackStack`; solo emite la intención de navegar.

---

## Cuándo usarlo

- Definir cómo navegar entre pantallas de una nueva feature.
- Decidir entre Navigation 3 y Navigation 2 Compose Multiplatform para el proyecto.
- Implementar el paso de parámetros entre pantallas de forma type-safe.
- Decidir cómo el ViewModel dispara una navegación sin acoplarse al sistema de navegación.
- Revisar el estado de madurez de las librerías de navegación antes de actualizar dependencias.

## Cuándo NO usarlo

- Para diseñar el ViewModel/`UiState`/`UiEvent` en sí → usar `mvvm-compose-kmp`; este Skill solo define cómo se traduce un evento de navegación en una acción real.
- Para inyectar dependencias en pantallas o ViewModels → usar `dependency-injection-koin`.
- Para diseñar las capas en general → usar `clean-architecture-kmp`.
- Para deep links: no se activan por iniciativa propia de este Skill; se tratan solo si el desarrollador los solicita expresamente.

---

## Dependencias

```text
- clean-architecture-kmp
- mvvm-compose-kmp
- dependency-injection-koin
```

---

## Entradas necesarias

- Estado de madurez actual de Navigation 3 multiplatform (UI, adaptive, `lifecycle-viewmodel-navigation3`) comprobado en la documentación oficial en el momento de implementar — no debe asumirse el estado descrito en este Skill como vigente indefinidamente.
- Lista de pantallas/destinos de la feature y los parámetros que necesita cada uno.
- Si la navegación necesita layouts adaptativos (list-detail en pantallas grandes/Desktop).
- Si el proyecto necesita ViewModels con scope ligado al ciclo de vida de cada entrada del back stack.

---

## Criterios arquitectónicos

- Los destinos/rutas viven en `presentation/navigation/routes` como clases `@Serializable`; Domain/Application nunca conocen rutas ni pantallas.
- El ViewModel nunca mantiene una referencia directa a `NavController`/`NavBackStack`; toda navegación se dispara emitiendo un evento de navegación, igual que ya define `mvvm-compose-kmp` para las acciones de UI.
- El back stack pertenece a la capa de navegación (el contenedor `NavHost`/`NavDisplay`), no al ViewModel de cada pantalla individual.
- Antes de fijar el enfoque para una plataforma del proyecto, debe comprobarse el estado de madurez actual en la documentación oficial.

---

## Reglas

Comunes a ambos enfoques:

- Las rutas deben definirse como `@Serializable` `data class`/`data object` en `presentation/navigation/routes`, nunca como `String` sueltos.
- El ViewModel debe exponer la intención de navegar mediante un canal de eventos (`Channel<NavigationEvent>`/`SharedFlow<NavigationEvent>`), nunca recibiendo `NavController`/`NavBackStack` como dependencia inyectada.
- El contenedor de navegación (`NavHost`/`NavDisplay`) debe ser el único punto que traduce un `NavigationEvent` en una llamada real de navegación.
- Los ViewModels de pantalla deben seguir obteniéndose vía Koin (`koinViewModel<T>()`, ver `mvvm-compose-kmp`/`dependency-injection-koin`), independientemente del enfoque de navegación elegido.
- No deben mezclarse ambos enfoques (Navigation 3 y Navigation 2) dentro del mismo grafo de navegación de la app.

Específicas por enfoque:

- Si se elige **Navigation 3** para iOS/Desktop, debe verificarse antes que `navigation3-ui`, `adaptive-navigation3` y `lifecycle-viewmodel-navigation3` (multiplatform, grupo `org.jetbrains.androidx.*`) ya estén en una versión estable; mientras sigan en alpha, el riesgo asumido debe documentarse explícitamente (ej. en un ADR).
- Si se elige **Navigation 2 Compose Multiplatform**, debe usarse el artefacto `org.jetbrains.androidx.navigation:navigation-compose`, nunca la versión Android-only (`androidx.navigation:navigation-compose`), en código de `commonMain`.

---

## Decisiones automáticas

```text
Si el target es solo Android (sin iOS/Desktop en el alcance inmediato)
    → Navigation 3 es una opción segura ya hoy (estable en Android desde la línea 1.1.x)

Si el target incluye iOS o Desktop con Compose Multiplatform
    → comprobar en la documentación oficial si navigation3-ui / adaptive-navigation3 /
      lifecycle-viewmodel-navigation3 (multiplatform) ya son estables
        → Si son estables → Navigation 3
        → Si siguen en alpha → Navigation 2 Compose Multiplatform (navigation-compose),
          estable en las 3 plataformas

Si no hay una decisión previa documentada para el proyecto y surge la duda
    → no asumir ninguno de los dos enfoques; preguntar explícitamente antes de generar
      código de navegación
```

---

## Proceso recomendado

1. Comprobar el estado de madurez actual de Navigation 3 multiplatform en la documentación oficial.
2. Elegir el enfoque (Navigation 3 o Navigation 2 Compose Multiplatform) según el resultado del paso 1.
3. Definir las rutas como clases `@Serializable` en `presentation/navigation/routes`.
4. Implementar el contenedor de navegación (`NavDisplay`+`NavBackStack` o `NavHost`+`NavController`) en el punto más alto posible del árbol de Composables.
5. Definir el canal de `NavigationEvent` en cada ViewModel que necesite disparar una navegación.
6. Conectar el contenedor de navegación para colectar esos eventos y traducirlos en la acción de navegación real.
7. Registrar los ViewModels vía Koin (`koinViewModel<T>()`), nunca manualmente.
8. Tratar los deep links como una ampliación explícita solicitada por el desarrollador, no como parte automática de este proceso.

---

## Checklist

- [ ] Se comprobó el estado de madurez de Navigation 3 multiplatform antes de elegir enfoque.
- [ ] Las rutas son clases `@Serializable`, no `String` sueltos.
- [ ] El ViewModel no mantiene referencia directa a `NavController`/`NavBackStack`.
- [ ] La navegación real se resuelve en el nivel de `NavHost`/`NavDisplay`, a partir de un `NavigationEvent`.
- [ ] Los ViewModels se obtienen vía Koin.
- [ ] No se mezclan Navigation 3 y Navigation 2 en el mismo grafo.
- [ ] Si se usa Navigation 3 en iOS/Desktop con piezas en alpha, el riesgo está documentado explícitamente.

---

## Definition of Done

- La navegación funciona de forma equivalente en Android, iOS y Desktop con el enfoque elegido.
- Ningún ViewModel depende de tipos de navegación; toda navegación pasa por un `NavigationEvent`.
- Las rutas son type-safe (`@Serializable`), sin parámetros sueltos por `String`.
- La elección de enfoque y su justificación quedan documentadas (ADR o comentario en `ARCHITECTURE.md`) cuando afecten a todo el proyecto.

---

## Riesgos

- Adoptar piezas de Navigation 3 en alpha para iOS/Desktop en un proyecto de producción, expuesto a cambios de API no estables.
- ViewModel acoplado a `NavController`/`NavBackStack`, dificultando testearlo de forma aislada (ver `testing-kmp`).
- Rutas representadas como `String` sueltos, perdiendo type-safety y facilitando errores en tiempo de ejecución.
- Mezclar Navigation 3 y Navigation 2 en el mismo proyecto, duplicando conceptos de back stack.
- ViewModels sin scope correcto al back stack, sobreviviendo más de lo esperado o perdiéndose al navegar.

---

## Anti-patrones

- `class HomeViewModel(private val navController: NavController)` — el ViewModel no debe conocer el sistema de navegación.
- Rutas definidas como `"detail/{id}"` (`String`) en lugar de una `data class DetailRoute(val id: String)` `@Serializable`.
- Navegar directamente desde un Composable de pantalla cuando la navegación depende de lógica de negocio que debería resolver el ViewModel primero.
- Usar `androidx.navigation:navigation-compose` (Android-only) en lugar de `org.jetbrains.androidx.navigation:navigation-compose` en `commonMain`.
- Adoptar Navigation 3 en iOS/Desktop sin documentar que las piezas multiplatform usadas están en alpha.

---

## Comandos útiles

```toml
# Opción A — Navigation 3 (estable en Android; multiplatform en alpha a fecha de este Skill)
# libs.versions.toml
[versions]
nav3 = "1.1.3" # estable en Android
nav3MultiplatformUi = "1.0.0-alpha05" # org.jetbrains.androidx.navigation3 — comprobar si ya hay estable

[libraries]
androidx-navigation3-runtime = { module = "androidx.navigation3:navigation3-runtime", version.ref = "nav3" }
jetbrains-navigation3-ui = { module = "org.jetbrains.androidx.navigation3:navigation3-ui", version.ref = "nav3MultiplatformUi" }
```

```kotlin
// shared/build.gradle.kts — commonMain (Opción A)
commonMain.dependencies {
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.jetbrains.navigation3.ui) // solo si ya es estable para tus targets
}
```

```toml
# Opción B — Navigation 2 Compose Multiplatform (estable en Android, iOS y Desktop)
# libs.versions.toml
[versions]
navigationCompose = "2.9.2"

[libraries]
jetbrains-navigation-compose = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "navigationCompose" }
```

```kotlin
// shared/build.gradle.kts — commonMain (Opción B)
commonMain.dependencies {
    implementation(libs.jetbrains.navigation.compose)
}
```

---

## Salida esperada

```text
presentation/
  navigation/
    routes/
      HomeRoute.kt          → @Serializable data object
      DetailRoute.kt          → @Serializable data class
    AppNavHost.kt             → contenedor de navegación (NavHost o NavDisplay), según enfoque elegido
```

---

## Ejemplos

### Correcto — rutas type-safe (comunes a ambos enfoques)

```kotlin
// presentation/navigation/routes/Routes.kt
@Serializable
data object HomeRoute

@Serializable
data class DetailRoute(val itemId: String)
```

### Correcto — NavigationEvent emitido desde el ViewModel (ver mvvm-compose-kmp)

```kotlin
sealed interface NavigationEvent {
    data class ToDetail(val itemId: String) : NavigationEvent
    data object Back : NavigationEvent
}

class HomeViewModel(private val getItemsUseCase: GetItemsUseCase) : ViewModel() {

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    fun onItemClick(itemId: String) {
        viewModelScope.launch {
            _navigationEvents.send(NavigationEvent.ToDetail(itemId))
        }
    }
}
```

### Correcto — Opción A: Navigation 3

```kotlin
@Composable
fun AppNavHost() {
    val backStack = rememberNavBackStack(HomeRoute)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<HomeRoute> {
                val viewModel = koinViewModel<HomeViewModel>()
                CollectNavigationEvents(viewModel.navigationEvents, backStack)
                HomeScreen(viewModel)
            }
            entry<DetailRoute> { route ->
                val viewModel = koinViewModel<DetailViewModel> { parametersOf(route.itemId) }
                DetailScreen(viewModel)
            }
        }
    )
}

@Composable
private fun CollectNavigationEvents(events: Flow<NavigationEvent>, backStack: NavBackStack) {
    LaunchedEffect(Unit) {
        events.collect { event ->
            when (event) {
                is NavigationEvent.ToDetail -> backStack.add(DetailRoute(event.itemId))
                is NavigationEvent.Back -> backStack.removeLastOrNull()
            }
        }
    }
}
```

### Correcto — Opción B: Navigation 2 Compose Multiplatform

```kotlin
@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            val viewModel = koinViewModel<HomeViewModel>()
            CollectNavigationEvents(viewModel.navigationEvents, navController)
            HomeScreen(viewModel)
        }
        composable<DetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<DetailRoute>()
            val viewModel = koinViewModel<DetailViewModel> { parametersOf(route.itemId) }
            DetailScreen(viewModel)
        }
    }
}

@Composable
private fun CollectNavigationEvents(events: Flow<NavigationEvent>, navController: NavController) {
    LaunchedEffect(Unit) {
        events.collect { event ->
            when (event) {
                is NavigationEvent.ToDetail -> navController.navigate(DetailRoute(event.itemId))
                is NavigationEvent.Back -> navController.popBackStack()
            }
        }
    }
}
```

### Incorrecto

```kotlin
// ❌ ViewModel acoplado al sistema de navegación
class HomeViewModel(private val navController: NavController) : ViewModel() {
    fun onItemClick(id: String) {
        navController.navigate(DetailRoute(id)) // el ViewModel no debería conocer NavController
    }
}

// ❌ Ruta como String suelto — pierde type-safety
navController.navigate("detail/$itemId")

// ❌ Mezclar Navigation 3 y Navigation 2 en el mismo grafo
val backStack = rememberNavBackStack(HomeRoute) // Navigation 3
val navController = rememberNavController()      // Navigation 2, en el mismo árbol
```

---

## Referencias

- clean-architecture-kmp
- mvvm-compose-kmp
- dependency-injection-koin
- Documentación oficial: https://developer.android.com/guide/navigation/navigation-3
- Documentación oficial (KMP): https://kotlinlang.org/docs/multiplatform/compose-navigation-3.html
- Documentación oficial (Navigation 2 KMP): https://kotlinlang.org/docs/multiplatform/compose-navigation.html
