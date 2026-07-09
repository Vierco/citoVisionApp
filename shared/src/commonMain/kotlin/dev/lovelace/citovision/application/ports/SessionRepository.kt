package dev.lovelace.citovision.application.ports

import kotlinx.coroutines.flow.Flow

/**
 * Persistencia de la sesión **local de invitado** (SPEC-0001 RF-8, RNF-8, RN-4).
 *
 * Solo se guarda un flag no sensible; nunca tokens ni credenciales (DataStore no está cifrado).
 * La sesión de cuenta (email/Google) la persiste el propio SDK de Firebase, no este puerto.
 * El acceso a DataStore vive únicamente en Infrastructure; UI y ViewModel usan este contrato.
 */
interface SessionRepository {
    fun isGuestSession(): Flow<Boolean>

    suspend fun setGuestSession(active: Boolean)
}
