package dev.lovelace.citovision.domain.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Saneado y validación del código de paciente (SPEC-0005 RN-1). */
class PatientCodeTest {
    @Test
    fun `given digits and an ascii hyphen when sanitizing then nothing changes`() {
        assertEquals("12-34", sanitizePatientCode("12-34"))
    }

    /**
     * El caso que reportó un tester en iOS: el teclado no siempre inserta el guion medio ASCII. Si esas
     * variantes se descartaran, el carácter tecleado desaparecería del campo sin explicación.
     */
    @Test
    fun `given a unicode dash when sanitizing then it becomes an ascii hyphen`() {
        assertEquals("12-34", sanitizePatientCode("12–34")) // en dash
        assertEquals("12-34", sanitizePatientCode("12‐34")) // hyphen
        assertEquals("12-34", sanitizePatientCode("12−34")) // minus sign
        assertEquals("12-34", sanitizePatientCode("12—34")) // em dash
    }

    @Test
    fun `given a normalised unicode dash when validating then the code is accepted`() {
        assertTrue(isValidPatientCode(sanitizePatientCode("12–34")))
    }

    @Test
    fun `given letters and symbols when sanitizing then they are stripped`() {
        assertEquals("12-34", sanitizePatientCode("12-ab34"))
        assertEquals("1234", sanitizePatientCode("12 34"))
        assertEquals("12-34", sanitizePatientCode("(12)-34"))
    }

    /**
     * Los dígitos de otros sistemas de escritura se descartan aunque `Char.isDigit()` los acepte: la
     * validación usa `[0-9]`, así que colarlos daría un campo que admite el carácter y luego lo declara
     * inválido sin decir por qué.
     */
    @Test
    fun `given non-ascii digits when sanitizing then they are stripped`() {
        assertEquals("12-34", sanitizePatientCode("12-34٠١"))
    }

    @Test
    fun `given a code without two segments when validating then it is rejected`() {
        assertFalse(isValidPatientCode("1234"))
        assertFalse(isValidPatientCode("1-34"))
        assertFalse(isValidPatientCode("12-"))
    }

    @Test
    fun `given surrounding whitespace when validating then it is ignored`() {
        assertTrue(isValidPatientCode(" 12-34 "))
    }
}
