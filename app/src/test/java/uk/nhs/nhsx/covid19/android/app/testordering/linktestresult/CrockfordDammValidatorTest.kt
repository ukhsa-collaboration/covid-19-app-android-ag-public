package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrockfordDammValidatorTest {

    val testSubject = CrockfordDammValidator()

    @Test
    fun `validate empty string`() = runBlocking {
        assertFalse { testSubject.validate("") }
    }

    @Test
    fun `validates correct codes`() = runBlocking {
        assertTrue { testSubject.validate("f3dzcfdt") }
        assertTrue { testSubject.validate("8vb7xehg") }
    }

    @Test
    fun `does not validate incorrect codes`() = runBlocking {
        assertFalse { testSubject.validate("f3dzcfdx") }
        assertFalse { testSubject.validate("8vb7xehb") }
    }

    @Test
    fun `does NOT replace illegal characters`() = runBlocking {
        assertFalse { testSubject.validate("1zlkdmdp") } // l not 1
        assertFalse { testSubject.validate("1z1keowk") } // o not 0
        assertFalse { testSubject.validate("1z1k-eowk") } // random -
        assertFalse { testSubject.validate("iz1keowk") } // i not 1
    }

    @Test
    fun `check regex condition`() {
        // mathematically valid codes that contain ilou
        assertFalse { testSubject.validate("bziclhdr") }
        assertFalse { testSubject.validate("bzicoqw7") }
        assertFalse { testSubject.validate("bzicuz6k") }
    }

    @Test
    fun `does not validate caps`() = runBlocking {
        assertFalse { testSubject.validate("f3dZcfdt") }
        assertFalse { testSubject.validate("8vb7xeHg") }
    }
}
