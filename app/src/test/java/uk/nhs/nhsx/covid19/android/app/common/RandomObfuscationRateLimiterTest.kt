package uk.nhs.nhsx.covid19.android.app.common

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import java.security.SecureRandom
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RandomObfuscationRateLimiterTest {
    private val mockedSecureRandom = mockk<SecureRandom>()
    private val testSubject = RandomObfuscationRateLimiter(mockedSecureRandom)

    @Test
    fun `always calls the secure random with value of 10 and returns true with 0`() {
        every { mockedSecureRandom.nextInt(10) } returns 0

        val result = testSubject.allow

        assertTrue(result)
        verify { mockedSecureRandom.nextInt(10) }
    }

    @Test
    fun `always calls the secure random with value of 10 and returns false with non 0`() {
        (1..9).forEach { value ->
            every { mockedSecureRandom.nextInt(10) } returns value

            val result = testSubject.allow

            assertFalse(result)
            verify { mockedSecureRandom.nextInt(10) }
        }
    }
}
