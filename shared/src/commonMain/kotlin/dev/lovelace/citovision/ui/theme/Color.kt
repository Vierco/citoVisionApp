package dev.lovelace.citovision.ui.theme

import androidx.compose.ui.graphics.Color

// Primary
val primary = Color(0xFF4E54EA)
val primaryPressed = Color(0xFF292FD5)
val onPrimary = Color(0xFFFFFFFF)

// Secondary
val secondary = Color(0xFF252466)
val secondaryPressed = Color(0xFF171643)

// Ojo con el nombre: `secondaryDark` NO deriva de `secondary`. Es el verde de la prioridad BAJA, un color
// semántico del semáforo de revisión, y por eso sigue siendo verde aunque la marca ya no lo sea.
val secondaryDark = Color(0xFF177552)
val onSecondary = Color(0xFFFFFFFF)

// Tertiary
val tertiary = Color(0xFF252466)
val onTertiary = Color(0xFFFFFFFF)

// Background & Surface
val background = Color(0xFFFFFFFF)
val surface = Color(0xFFFFFFFF)
val onBackground = Color(0xFF282828)
val onSurface = Color(0xFF6F6F6F)
val hint = Color(0xFF9E9E9E)

// Semantic Colors
val success = Color(0xFF2FD38A)
val warning = Color(0xFFF59E3A)
val error = Color(0xFFF53A63)
val errorPressed = Color(0xFFC71C43)
val info = Color(0xFF2FA7F0)

// --- Tema oscuro (ver DESIGN.md "Paleta oscura"): misma identidad, marca aclarada a un tono intermedio,
// fondo azulado y secondaryDark invertido a verde claro (texto sobre fondo oscuro).
//
// El tono intermedio no es un capricho estético. Estos colores se usan a la vez como relleno con texto
// blanco encima (botones, avatar) y como texto sobre el fondo (nombre de la app, confirmaciones), y sobre
// #111318 ningún color cumple AA en ambos papeles: como texto exige luminancia >= 0,204 y para que el
// blanco encima cumpla hace falta <= 0,183. Los rangos no se solapan, así que se busca el compromiso:
// AA como relleno y AA de texto grande como texto, que es el criterio que aplica (88sp y 28sp). ---
val darkPrimary = Color(0xFF7378EE)
val darkPrimaryPressed = Color(0xFF5F63CF)
val darkOnPrimary = Color(0xFFFFFFFF)

val darkSecondary = Color(0xFF6362D0)
val darkSecondaryPressed = Color(0xFF4B4AA6)
val darkSecondaryDark = Color(0xFF64D3A1)
val darkOnSecondary = Color(0xFFFFFFFF)

val darkTertiary = Color(0xFF6362D0)
val darkOnTertiary = Color(0xFFFFFFFF)

val darkBackground = Color(0xFF111318)
val darkSurface = Color(0xFF262A33)
val darkOnBackground = Color(0xFFECECEC)
val darkOnSurface = Color(0xFFA8A8A8)
val darkHint = Color(0xFF707070)

val darkSuccess = Color(0xFF3EC589)
val darkWarning = Color(0xFFE59D4B)
val darkError = Color(0xFFE14869)
val darkErrorPressed = Color(0xFFB52949)
val darkInfo = Color(0xFF42A4E0)
