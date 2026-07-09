package dev.lovelace.citovision.core.result

/**
 * Result genérico compartido por todas las capas (Domain, Application, Infrastructure).
 * Ver Skill result-pattern. No usar `kotlin.Result` en firmas públicas de esas capas.
 */
sealed interface Result<out T, out E> {
    data class Success<out T>(
        val value: T,
    ) : Result<T, Nothing>

    data class Failure<out E>(
        val error: E,
    ) : Result<Nothing, E>
}

inline fun <T, E, R> Result<T, E>.fold(
    onSuccess: (T) -> R,
    onFailure: (E) -> R,
): R =
    when (this) {
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
