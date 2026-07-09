package dev.lovelace.citovision.presentation.format

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private val DATE_TIME_FORMAT =
    LocalDateTime.Format {
        dayOfMonth()
        char('/')
        monthNumber()
        char('/')
        year()
        char(' ')
        hour()
        char(':')
        minute()
    }

/** Formatea el instante del análisis en la zona horaria del dispositivo: `dd/MM/yyyy HH:mm` (SPEC-0004 RN-2). */
fun Instant.formatAnalysisDateTime(): String = toLocalDateTime(TimeZone.currentSystemDefault()).format(DATE_TIME_FORMAT)
