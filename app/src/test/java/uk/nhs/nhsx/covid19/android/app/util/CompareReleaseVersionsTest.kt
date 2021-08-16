package uk.nhs.nhsx.covid19.android.app.util

import org.junit.Test
import kotlin.test.assertEquals

class CompareReleaseVersionsTest {

    private val testSubject = CompareReleaseVersions()

    @Test
    fun `compare versions`() {
        // Equal versions
        assertEquals(0, testSubject("1", "1"))
        assertEquals(0, testSubject("2.2", "2.2"))
        assertEquals(0, testSubject("3.3.3", "3.3.3"))

        // Newer version2
        assertEquals(-1, testSubject("1", "2"))
        assertEquals(-1, testSubject("1.1", "2"))
        assertEquals(-1, testSubject("1.1", "2.2"))
        assertEquals(-1, testSubject("1.1", "2.2.2"))
        assertEquals(-1, testSubject("1.2.3", "3.2.1"))

        // Older version2
        assertEquals(1, testSubject("2", "1"))
        assertEquals(1, testSubject("2", "1.1"))
        assertEquals(1, testSubject("2.2", "1.1"))
        assertEquals(1, testSubject("2.2.2", "1.1"))
        assertEquals(1, testSubject("3.2.1", "1.2.3"))
    }
}
