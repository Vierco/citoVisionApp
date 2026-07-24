package dev.lovelace.citovision.presentation.components

import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Corrige la alineación vertical de un icono situado junto a texto en un `Row` con
 * `Alignment.CenterVertically`.
 *
 * La fuente Dongle declara métricas verticales asimétricas, así que el centro óptico de los glifos no
 * coincide con el centro de la caja de línea del texto. `CenterVertically` alinea el icono a esa caja,
 * de modo que el icono queda ligeramente más bajo que el texto. Este modifier sube el icono ese
 * desajuste. Se expresa en `sp` para que escale con la densidad y con el tamaño de fuente del sistema
 * (el lambda de `offset` resuelve `sp` a px en contexto de densidad). Ajustar el valor aquí en un único
 * sitio afecta a todos los botones/filas que lo usan.
 */
fun Modifier.dongleIconAlign(): Modifier = offset { IntOffset(x = 0, y = -(3.sp).toPx().roundToInt()) }
