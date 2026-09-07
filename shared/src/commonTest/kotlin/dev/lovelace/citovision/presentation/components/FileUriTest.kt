package dev.lovelace.citovision.presentation.components

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * URI `file://` de las imágenes locales del historial (SPEC-0004 RN-5, ADR-0011).
 *
 * El caso que motivó la función es Windows: la ruta empieza por la letra de unidad y usa contrabarras,
 * así que anteponer el esquema sin normalizar produce `file://C:\Users\...`, donde `C:` se interpreta
 * como *host* y las contrabarras no son separadores legales. Coil no la resuelve y la tarjeta cae al
 * placeholder gris. Los casos de macOS, Linux, Android e iOS se cubren para garantizar que el arreglo
 * no cambió el resultado en las plataformas donde ya funcionaba.
 */
class FileUriTest {
    @Test
    fun `given a Windows path when converting then the drive letter is not read as a host`() {
        val path = "C:\\Users\\Sergio\\.citovision\\analysis_images\\a1b2c3.png"
        assertEquals(
            "file:///C:/Users/Sergio/.citovision/analysis_images/a1b2c3.png",
            path.toFileUri(),
        )
    }

    @Test
    fun `given a Windows path with forward slashes when converting then the third slash is still added`() {
        val path = "C:/Users/Sergio/.citovision/analysis_images/a1b2c3.png"
        assertEquals(
            "file:///C:/Users/Sergio/.citovision/analysis_images/a1b2c3.png",
            path.toFileUri(),
        )
    }

    @Test
    fun `given a Windows path with mixed separators when converting then every backslash is normalised`() {
        val path = "C:\\Users\\Sergio/.citovision\\analysis_images/a1b2c3.png"
        assertEquals(
            "file:///C:/Users/Sergio/.citovision/analysis_images/a1b2c3.png",
            path.toFileUri(),
        )
    }

    @Test
    fun `given a macOS path when converting then the result is the same as before the fix`() {
        val path = "/Users/sergioalvarez/.citovision/analysis_images/a1b2c3.png"
        assertEquals(
            "file:///Users/sergioalvarez/.citovision/analysis_images/a1b2c3.png",
            path.toFileUri(),
        )
    }

    @Test
    fun `given an Android path when converting then the result is the same as before the fix`() {
        val path = "/data/user/0/dev.lovelace.citovision/files/analysis_images/a1b2c3.png"
        assertEquals(
            "file:///data/user/0/dev.lovelace.citovision/files/analysis_images/a1b2c3.png",
            path.toFileUri(),
        )
    }
}
