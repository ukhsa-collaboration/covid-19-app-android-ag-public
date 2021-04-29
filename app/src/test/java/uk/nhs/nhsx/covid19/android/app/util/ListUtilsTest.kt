package uk.nhs.nhsx.covid19.android.app.util

import org.junit.Test
import kotlin.test.assertEquals

class ListUtilsTest {

    @Test
    fun `shallowCopy creates a copy of the initial list`() {
        val initialList = mutableListOf(1, 2, 3, 4, 5)
        val shallowCopy = initialList.shallowCopy()

        assertEquals(listOf(1, 2, 3, 4, 5), shallowCopy)
    }

    @Test
    fun `modifications to the initial list do not change a copy created by shallowCopy`() {
        val initialList = mutableListOf(1, 2, 3, 4, 5)
        val shallowCopy = initialList.shallowCopy()

        initialList.add(6)
        initialList.removeAt(0)

        assertEquals(listOf(1, 2, 3, 4, 5), shallowCopy)
    }
}
