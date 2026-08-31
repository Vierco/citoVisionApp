package dev.lovelace.citovision.infrastructure.auth

/**
 * [TokenStore] que no persiste nada: la sesión vive mientras viva el proceso. Es el comportamiento de
 * Desktop desde ADR-0002 (al reiniciar se vuelve a pedir login), y el que usan los tests.
 *
 * El acceso queda serializado por el mutex de [RestFirebaseAuthService], único consumidor.
 */
class InMemoryTokenStore : TokenStore {
    private var session: StoredSession? = null

    override suspend fun load(): StoredSession? = session

    override suspend fun save(session: StoredSession) {
        this.session = session
    }

    override suspend fun clear() {
        session = null
    }
}
