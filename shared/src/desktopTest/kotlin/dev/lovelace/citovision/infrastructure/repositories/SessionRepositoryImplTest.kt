package dev.lovelace.citovision.infrastructure.repositories

import dev.lovelace.citovision.infrastructure.persistence.preferences.createDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test de integración de [SessionRepositoryImpl] sobre un DataStore real con fichero temporal
 * (regla testing-kmp: Infrastructure se testea con el store real, no con mocks). Cada test usa un
 * fichero propio para garantizar aislamiento (DataStore exige una única instancia activa por ruta).
 */
class SessionRepositoryImplTest {
    private lateinit var tempFile: File
    private lateinit var repository: SessionRepositoryImpl

    @BeforeTest
    fun setUp() {
        tempFile = File.createTempFile("citovision_session_test", ".preferences_pb").apply { delete() }
        repository = SessionRepositoryImpl(createDataStore { tempFile.absolutePath })
    }

    @AfterTest
    fun tearDown() {
        tempFile.delete()
    }

    @Test
    fun `given nothing stored when reading then returns false by default`() =
        runTest {
            // When / Then
            assertFalse(repository.isGuestSession().first())
        }

    @Test
    fun `given the guest flag set to true when reading then returns true`() =
        runTest {
            // Given
            repository.setGuestSession(true)

            // When / Then
            assertTrue(repository.isGuestSession().first())
        }

    @Test
    fun `given the guest flag cleared when reading then returns false`() =
        runTest {
            // Given
            repository.setGuestSession(true)
            repository.setGuestSession(false)

            // When / Then
            assertFalse(repository.isGuestSession().first())
        }
}
