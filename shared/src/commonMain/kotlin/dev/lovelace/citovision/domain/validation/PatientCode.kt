package dev.lovelace.citovision.domain.validation

/**
 * Validación del código de paciente (SPEC-0005 RN-1): solo dígitos y guion medio, con estructura mínima
 * `XX-YY` (al menos dos dígitos, guion, al menos dos dígitos), admitiendo más dígitos y segmentos.
 * Centralizado para que la pantalla de escaneo y la de búsqueda de Pacientes usen la misma regla.
 */
private val PATIENT_CODE_REGEX = Regex("^[0-9]{2,}(-[0-9]{2,})+$")

fun isValidPatientCode(code: String): Boolean = PATIENT_CODE_REGEX.matches(code.trim())

/** Quita cualquier carácter que no sea dígito o guion, para el filtrado en vivo del campo de texto. */
fun sanitizePatientCode(input: String): String = input.filter { it.isDigit() || it == '-' }
