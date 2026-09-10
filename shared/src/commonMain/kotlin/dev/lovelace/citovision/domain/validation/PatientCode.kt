package dev.lovelace.citovision.domain.validation

/**
 * Validación del código de paciente (SPEC-0005 RN-1): solo dígitos y guion medio, con estructura mínima
 * `XX-YY` (al menos dos dígitos, guion, al menos dos dígitos), admitiendo más dígitos y segmentos.
 * Centralizado para que la pantalla de escaneo y la de búsqueda de Pacientes usen la misma regla.
 */
private val PATIENT_CODE_REGEX = Regex("^[0-9]{2,}(-[0-9]{2,})+$")

fun isValidPatientCode(code: String): Boolean = PATIENT_CODE_REGEX.matches(code.trim())

/**
 * Variantes de guion que un teclado puede insertar en lugar del guion medio ASCII: el de iOS y la
 * puntuación inteligente son los sospechosos habituales, pero también aparecen al pegar un código
 * copiado de un documento. Se aceptan y se normalizan en vez de descartarlas, porque para quien escribe
 * son el mismo carácter y verlas desaparecer resulta incomprensible.
 */
private val DASH_VARIANTS =
    setOf(
        '‐', // hyphen
        '‑', // non-breaking hyphen
        '‒', // figure dash
        '–', // en dash
        '—', // em dash
        '―', // horizontal bar
        '−', // minus sign
        '﹣', // small hyphen-minus
        '－', // fullwidth hyphen-minus
    )

/**
 * Deja solo dígitos y guiones, para el filtrado en vivo del campo de texto.
 *
 * Los dígitos se limitan a `0`-`9` a propósito, para no admitir dígitos de otros sistemas de escritura
 * que [PATIENT_CODE_REGEX] rechazaría después: el campo aceptaría el carácter y la validación lo daría
 * por inválido sin explicar por qué.
 */
fun sanitizePatientCode(input: String): String =
    buildString {
        input.forEach { char ->
            when {
                char in '0'..'9' -> append(char)
                char == '-' || char in DASH_VARIANTS -> append('-')
            }
        }
    }
