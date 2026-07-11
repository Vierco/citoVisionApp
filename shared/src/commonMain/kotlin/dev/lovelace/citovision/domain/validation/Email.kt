package dev.lovelace.citovision.domain.validation

/** Validación básica de formato de correo electrónico, compartida por las pantallas que lo requieran. */
val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]{2,}$")

fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email)
