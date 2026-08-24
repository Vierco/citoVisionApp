---
name: result-pattern
description: "Definir la firma de un método que puede fallar (Repository, UseCase, DataSource), tipar un error de Domain/Application, o decidir si algo debe lanzar excepción o devolver Result."
---

# Skill: result-pattern

## Objetivo

Definir cómo modelar el éxito y el error de las operaciones en Domain, Application e Infrastructure mediante un tipo `Result<T, E>` propio, evitando las excepciones como mecanismo de control de flujo de negocio, y cómo tipar el error en cada capa (`DomainError`, `AppError`).

Este Skill define únicamente el patrón Result/Either y la jerarquía de errores. No define el `UiState`/`UiEvent` del ViewModel (eso es `mvvm-compose-kmp`), ni cómo se inyectan Repository/UseCase (eso es `dependency-injection-koin`).

---

## Modelo conceptual

```text
            Domain
  interface UserRepository {            ← Port
      suspend fun getUser(id): Result<User, DomainError>
  }
                ▲
                │ implementa
       Infrastructure
  class UserRepositoryImpl : UserRepository {
      // try/catch interno
      // mapea excepciones técnicas → DomainError
  }

            Application
  class GetUserUseCase(repo: UserRepository) {
      suspend operator fun invoke(id): Result<User, AppError>
      // AppError puede envolver un DomainError recibido de un Port
  }

            Presentation
  viewModelScope.launch {
      useCase(id).fold(
          onSuccess = { user -> _uiState.update { ... } },
          onFailure = { error -> _uiState.update { it.copy(errorMessage = error.toUiMessage()) } }
      )
  }
  // La UI nunca recibe DomainError/AppError sin mapear
```

La dependencia de los tipos de error sigue la misma dirección que las capas: `AppError` puede conocer `DomainError`, pero `DomainError` nunca conoce `AppError`.

---

## Cuándo usarlo

- Diseñar la firma de un método de `Repository`, `UseCase` o `DataSource`.
- Decidir cómo propagar y tipar un error entre capas.
- Revisar si una función debería lanzar una excepción o devolver `Result`.
- Mapear errores técnicos de Infrastructure (red, BD, serialización) a errores de Domain/Application.
- Consumir el resultado de un `UseCase` desde un ViewModel.

## Cuándo NO usarlo

- Para definir las capas en general → usar `clean-architecture-kmp`.
- Para decidir cómo se inyecta un `Repository`/`UseCase` → usar `dependency-injection-koin`.
- Para diseñar la forma del `UiState`/`UiEvent` del ViewModel → usar `mvvm-compose-kmp`; este Skill solo define cómo el ViewModel **consume** el `Result`, no la forma del estado.
- Para validación de campos de formulario en tiempo real sin paso por un `UseCase`: es responsabilidad de Presentation, no de este Skill.

---

## Dependencias

```text
- clean-architecture-kmp
- mvvm-compose-kmp
- dependency-injection-koin
- testing-kmp
```

---

## Entradas necesarias

- Catálogo de errores de negocio esperables por feature (ej. `UserNotFound`, `InvalidCredentials`).
- Catálogo de errores técnicos esperables en Infrastructure (red, BD, parsing) que deban traducirse.
- Si la capa Application necesita errores propios además de los de Domain (validación de parámetros del `UseCase`, fallos de orquestación entre varios Ports).

---

## Criterios arquitectónicos

- Un único tipo genérico `Result<T, E>` vive en `core/result` y es compartido por todas las capas; no se reimplementa por capa ni por feature.
- El tipo de error (`E`) sí cambia por capa: `Domain` define `DomainError`, `Application` define `AppError`, cada uno en su carpeta `errors/` correspondiente.
- `AppError` puede envolver un `DomainError` (composición hacia el exterior), pero `Domain` nunca conoce `AppError`: la dependencia de errores apunta hacia el centro, igual que el resto de Clean Architecture.
- Infrastructure nunca expone excepciones nativas (`IOException`, `SQLException`, `SerializationException`) fuera de sus límites; siempre las traduce al `DomainError` esperado por la interfaz que implementa.
- Presentation nunca consume `DomainError`/`AppError` directamente en la UI; siempre los traduce a un mensaje o a un estado tipado mediante un mapper.

---

## Reglas

- Debe definirse un único `Result<out T, out E>` en `core/result`, usado en toda firma que pueda fallar de forma esperada (`Repository`, `UseCase`, `DataSource`).
- No debe usarse `kotlin.Result` de la stdlib (ni `runCatching` como sustituto) en firmas públicas de Domain/Application/Infrastructure; solo el `Result` propio.
- Toda función que pueda fallar de forma esperada debe devolver `Result<T, E>`; nunca debe lanzar una excepción para un error de negocio.
- Las excepciones (`Throwable`) deben reservarse para errores de programación no esperados (bugs), nunca para errores de negocio o de validación.
- `Domain` debe definir y devolver siempre `DomainError` (o subtipos) en sus Ports (interfaces de Repository/Servicio) y en sus Domain Services.
- `Application` debe definir `AppError` para sus `UseCase`; `AppError` puede envolver un `DomainError` recibido de un Port, pero nunca al revés.
- `Infrastructure` debe capturar toda excepción técnica dentro de su propia implementación y traducirla al `DomainError` esperado por el Port que implementa; nunca debe dejar escapar la excepción original.
- Al capturar excepciones dentro de una corrutina, debe relanzarse siempre `CancellationException`; nunca debe absorberse ni convertirse en un `Failure`.
- `Presentation` nunca debe mostrar el `.toString()` de un `DomainError`/`AppError` directamente en la UI; debe mapearlo a un mensaje localizado o a un estado tipado del `UiState`.
- Cada `DomainError`/`AppError` debe ser una `sealed class`/`sealed interface` cerrada, nunca una `Exception` abierta ni un `String` libre.
- No debe mezclarse en una misma función el patrón `Result` con `throw` para casos de error esperados; se elige un único mecanismo y se mantiene.

---

## Decisiones automáticas

```text
Si el error es previsible y forma parte del contrato de negocio (no encontrado, credenciales inválidas, validación)
    → Result.Failure con DomainError/AppError

Si el error es un fallo de programación (null inesperado, índice fuera de rango, contrato roto)
    → excepción; no se envuelve en Result

Si la función vive en una interfaz definida en Domain (Port)
    → su Result usa DomainError

Si la función vive en un UseCase de Application
    → su Result usa AppError (puede envolver un DomainError recibido de un Port)

Si Infrastructure captura una excepción técnica (IOException, SQLException, SerializationException)
    → debe mapearla al DomainError correspondiente antes de retornar el Result; nunca debe propagar el tipo de excepción original

Si se captura una excepción dentro de una corrutina
    → si es CancellationException, debe relanzarse siempre, nunca convertirse en Failure
```

---

## Proceso recomendado

1. Identificar los errores de negocio esperables de la feature y definirlos como subtipos de `DomainError` en `domain/errors`.
2. Definir la firma del Port en Domain devolviendo `Result<T, DomainError>`.
3. Implementar el Port en Infrastructure, capturando las excepciones técnicas y mapeándolas a `DomainError`.
4. Definir, si aplica, `AppError` en `application/errors` para errores propios del `UseCase` (validación de parámetros, orquestación entre varios Ports).
5. Implementar el `UseCase` devolviendo `Result<T, AppError>`, envolviendo el `DomainError` recibido cuando corresponda.
6. Consumir el `Result` en el ViewModel con `fold`/`onSuccess`/`onFailure`, mapeando el error a un mensaje o estado de `UiState` (ver `mvvm-compose-kmp`).
7. Cubrir con tests el caso `Success` y cada variante de `Failure` (ver `testing-kmp`).

---

## Checklist

- [ ] Existe un único `Result<T, E>` en `core/result`, usado por todas las capas.
- [ ] No se usa `kotlin.Result` en ninguna firma de Domain/Application/Infrastructure.
- [ ] `DomainError` está definido en `domain/errors` y `AppError` en `application/errors`.
- [ ] Infrastructure no deja escapar ninguna excepción técnica sin mapear.
- [ ] `CancellationException` se relanza siempre dentro de corrutinas.
- [ ] El ViewModel no expone `DomainError`/`AppError` directamente a la UI.
- [ ] Ninguna función de negocio usa `throw` para un error esperado del dominio.

---

## Definition of Done

- Todas las funciones de Domain, Application e Infrastructure que pueden fallar de forma esperada devuelven `Result<T, E>`.
- Cada error de negocio está representado como un caso cerrado de `DomainError` o `AppError`, no como texto libre ni excepción abierta.
- La UI nunca recibe un tipo de error de Domain/Application sin mapear a mensaje o estado.
- Los tests cubren `Success` y cada `Failure` relevante por feature.

---

## Riesgos

- Excepciones técnicas filtrándose desde Infrastructure hasta Presentation sin mapear.
- `CancellationException` absorbida por error, rompiendo la cancelación cooperativa de corrutinas.
- Proliferación de un `AppError`/`DomainError` "cajón de sastre" (`Unknown(message: String)`) que oculta el caso real.
- Mezclar `try/catch` y `Result` en el mismo flujo, dificultando saber dónde se gestiona cada error.
- UI mostrando mensajes técnicos (stack traces, nombres de excepción) en lugar de mensajes de negocio.

---

## Anti-patrones

- Lanzar una excepción de negocio (`throw UserNotFoundException()`) en lugar de devolver `Result.Failure(DomainError.UserNotFound)`.
- `Repository` devolviendo `Result<T, Throwable>` en lugar de un `DomainError` tipado.
- `catch (e: Exception) { /* ignorado */ }` sin relanzar `CancellationException`.
- ViewModel haciendo `errorMessage = error.toString()` con el error de dominio crudo.
- Un único `AppError.Unknown(val message: String)` usado para todos los casos de fallo, sin granularidad.
- Usar `kotlin.Result`/`runCatching` como sustituto del `Result` propio en las firmas públicas de Domain/Application.

---

## Comandos útiles

N/A — Skill sin dependencias externas ni comandos CLI; `Result<T, E>` es Kotlin puro y no requiere librerías adicionales.

---

## Salida esperada

```text
core/
  result/
    Result.kt        → sealed interface Result<out T, out E> + fold/map/mapError
domain/
  errors/
    DomainError.kt    → sealed class DomainError
application/
  errors/
    AppError.kt        → sealed class AppError
```

---

## Ejemplos

### Correcto — Result genérico en core

```kotlin
// core/result/Result.kt
sealed interface Result<out T, out E> {
    data class Success<out T>(val value: T) : Result<T, Nothing>
    data class Failure<out E>(val error: E) : Result<Nothing, E>
}

inline fun <T, E, R> Result<T, E>.fold(
    onSuccess: (T) -> R,
    onFailure: (E) -> R
): R = when (this) {
    is Result.Success -> onSuccess(value)
    is Result.Failure -> onFailure(error)
}

inline fun <T, E, R> Result<T, E>.map(transform: (T) -> R): Result<R, E> =
    when (this) {
        is Result.Success -> Result.Success(transform(value))
        is Result.Failure -> this
    }

inline fun <T, E, F> Result<T, E>.mapError(transform: (E) -> F): Result<T, F> =
    when (this) {
        is Result.Success -> this
        is Result.Failure -> Result.Failure(transform(error))
    }
```

### Correcto — DomainError + Port + Infrastructure

```kotlin
// domain/errors/DomainError.kt
sealed class DomainError {
    data object UserNotFound : DomainError()
    data object InvalidCredentials : DomainError()
    data class Unexpected(val cause: String) : DomainError()
}

// domain/repositories/UserRepository.kt — Port
interface UserRepository {
    suspend fun getUser(id: String): Result<User, DomainError>
}

// infrastructure/repositories/UserRepositoryImpl.kt
class UserRepositoryImpl(private val api: UserApi) : UserRepository {
    override suspend fun getUser(id: String): Result<User, DomainError> {
        return try {
            Result.Success(api.fetchUser(id).toEntity())
        } catch (e: CancellationException) {
            throw e
        } catch (e: ClientRequestException) {
            Result.Failure(DomainError.UserNotFound)
        } catch (e: Exception) {
            Result.Failure(DomainError.Unexpected(e.message ?: "unknown"))
        }
    }
}
```

### Correcto — AppError + UseCase + ViewModel

```kotlin
// application/errors/AppError.kt
sealed class AppError {
    data class Domain(val error: DomainError) : AppError()
    data object InvalidInput : AppError()
}

// application/usecases/GetUserUseCase.kt
class GetUserUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(id: String): Result<User, AppError> {
        if (id.isBlank()) return Result.Failure(AppError.InvalidInput)
        return repository.getUser(id).mapError { AppError.Domain(it) }
    }
}

// presentation/viewmodels/UserViewModel.kt
private fun loadUser(id: String) {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        getUserUseCase(id).fold(
            onSuccess = { user -> _uiState.update { it.copy(isLoading = false, user = user) } },
            onFailure = { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.toUiMessage()) } }
        )
    }
}

private fun AppError.toUiMessage(): String = when (this) {
    is AppError.InvalidInput -> "El identificador no es válido."
    is AppError.Domain -> when (error) {
        DomainError.UserNotFound -> "No hemos encontrado ese usuario."
        DomainError.InvalidCredentials -> "Credenciales incorrectas."
        is DomainError.Unexpected -> "Ha ocurrido un error inesperado."
    }
}
```

### Incorrecto

```kotlin
// ❌ Repository lanzando excepción en lugar de devolver Result
class UserRepositoryImpl(private val api: UserApi) : UserRepository {
    override suspend fun getUser(id: String): User {
        return api.fetchUser(id).toEntity() // si falla la red, lanza excepción sin mapear
    }
}

// ❌ UseCase reenvolviendo en una excepción genérica
class GetUserUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(id: String): User {
        return try {
            repository.getUser(id)
        } catch (e: Exception) {
            throw RuntimeException("Error al obtener usuario")
        }
    }
}

// ❌ ViewModel mostrando el error crudo
_uiState.update { it.copy(errorMessage = error.toString()) }
```

---

## Referencias

- clean-architecture-kmp
- mvvm-compose-kmp
- dependency-injection-koin
- testing-kmp
- ARCHITECTURE.md — secciones Domain/errors, Application/errors, Core/result
