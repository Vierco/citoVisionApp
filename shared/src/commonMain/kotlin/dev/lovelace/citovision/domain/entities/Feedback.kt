package dev.lovelace.citovision.domain.entities

import kotlin.time.Instant

/**
 * Feedback enviado por el usuario desde Ajustes. Se persiste en la base de datos remota para revisión
 * manual (MVP: sin envío de correo). `ownerUid` es nulo cuando lo envía un invitado.
 */
data class Feedback(
    val email: String,
    val message: String,
    val createdAt: Instant,
    val ownerUid: String?,
)
