package uk.nhs.nhsx.covid19.android.app.util.crashreporting

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test
import java.security.SecureRandom

class RemoteServiceExceptionReportRateLimiterTest {

    private val secureRandom = mockk<SecureRandom>()

    val testSubject = RemoteServiceExceptionReportRateLimiter(secureRandom)

    @Test
    fun `allow remote service exception report when random value is below limit`() {
        every { secureRandom.nextInt(100) } returns 4

        val result = testSubject.isAllowed()

        Assert.assertTrue(result)
    }

    @Test
    fun `disallow remote service exception report when random value equals limit`() {
        every { secureRandom.nextInt(100) } returns 5

        val result = testSubject.isAllowed()

        Assert.assertFalse(result)
    }

    @Test
    fun `disallow remote service exception report when random value is above limit`() {
        every { secureRandom.nextInt(100) } returns 6

        val result = testSubject.isAllowed()

        Assert.assertFalse(result)
    }
}
