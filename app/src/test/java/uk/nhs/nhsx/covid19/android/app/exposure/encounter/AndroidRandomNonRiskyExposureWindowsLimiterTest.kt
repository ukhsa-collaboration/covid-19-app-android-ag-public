package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class AndroidRandomNonRiskyExposureWindowsLimiterTest {

    private val secureRandom = mockk<SecureRandom>()

    val testSubject = AndroidRandomNonRiskyExposureWindowsLimiter(secureRandom)

    @Test
    fun `allow epidemiology events when random value is below limit`() {
        every { secureRandom.nextInt(1000) } returns 24

        val result = testSubject.isAllowed()

        assertTrue(result)
    }

    @Test
    fun `disallow epidemiology events when random value equals limit`() {
        every { secureRandom.nextInt(1000) } returns 25

        val result = testSubject.isAllowed()

        assertFalse(result)
    }

    @Test
    fun `disallow epidemiology events when random value is above limit`() {
        every { secureRandom.nextInt(1000) } returns 26

        val result = testSubject.isAllowed()

        assertFalse(result)
    }
}
