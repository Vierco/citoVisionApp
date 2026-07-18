package dev.lovelace.citovision.domain.entities

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * El catálogo [CellClass] es la fuente única del mapeo del modelo (SPEC-0006). Se blinda su integridad:
 * 14 clases, índices 0..13 contiguos, pesos de RN-8 y qué clases no son células (RN-6).
 */
class CellClassTest {
    @Test
    fun `given the catalog then it has the 14 model classes with unique contiguous indices`() {
        assertEquals(14, CellClass.entries.size)
        assertEquals((0..13).toList(), CellClass.entries.map { it.index }.sorted())
        assertEquals(
            14,
            CellClass.entries
                .map { it.index }
                .toSet()
                .size,
        )
    }

    @Test
    fun `given an index when resolving then it maps to the expected class or null`() {
        assertEquals(CellClass.BLASTO, CellClass.fromIndex(3))
        assertEquals(CellClass.RESTOS_CELULARES, CellClass.fromIndex(13))
        assertNull(CellClass.fromIndex(14))
        assertNull(CellClass.fromIndex(-1))
    }

    @Test
    fun `given the morphological weights then they match SPEC-0006 RN-8`() {
        assertEquals(5, CellClass.BLASTO.priorityWeight)
        assertEquals(4, CellClass.PROMIELOCITO.priorityWeight)
        assertEquals(3, CellClass.MIELOCITO.priorityWeight)
        assertEquals(3, CellClass.METAMIELOCITO.priorityWeight)
        assertEquals(2, CellClass.LINFOCITO_ATIPICO.priorityWeight)
        assertEquals(2, CellClass.BASOFILO.priorityWeight)
        assertEquals(2, CellClass.ERITROBLASTO.priorityWeight)
        assertEquals(1, CellClass.BASTONETE.priorityWeight)
        assertEquals(0, CellClass.LINFOCITO.priorityWeight)
        assertEquals(0, CellClass.NEUTROFILO_SEGMENTADO.priorityWeight)
        assertEquals(0, CellClass.MONOCITO.priorityWeight)
        assertEquals(0, CellClass.EOSINOFILO.priorityWeight)
    }

    @Test
    fun `given the non-cell classes then only artefact and debris are not cells`() {
        assertFalse(CellClass.ARTEFACTO.isCell)
        assertFalse(CellClass.RESTOS_CELULARES.isCell)
        assertTrue(CellClass.BLASTO.isCell)
        assertEquals(12, CellClass.entries.count { it.isCell })
    }
}
