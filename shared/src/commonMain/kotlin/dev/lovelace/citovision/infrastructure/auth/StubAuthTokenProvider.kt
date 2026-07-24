package dev.lovelace.citovision.infrastructure.auth

import dev.lovelace.citovision.application.ports.AuthTokenProvider

/**
 * Proveedor de token para plataformas sin Firebase (iOS en fase 1, igual que [StubAuthService]): no hay
 * sesión de cuenta, luego no hay ID token que enviar y las llamadas remotas quedarán sin autorizar.
 */
class StubAuthTokenProvider : AuthTokenProvider {
    override suspend fun currentIdToken(): String? = null
}
