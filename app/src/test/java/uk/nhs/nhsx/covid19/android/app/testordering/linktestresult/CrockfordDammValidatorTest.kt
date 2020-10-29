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
}
