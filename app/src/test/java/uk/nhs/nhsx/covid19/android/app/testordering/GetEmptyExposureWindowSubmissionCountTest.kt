package uk.nhs.nhsx.covid19.android.app.testordering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import java.security.SecureRandom
import kotlin.test.assertEquals

class GetEmptyExposureWindowSubmissionCountTest {

    private val secureRandom = mockk<SecureRandom>(relaxed = true)

    private val testSubject = GetEmptyExposureWindowSubmissionCount(secureRandom)

    @Test
    fun `returns at most 15 with zero actual exposure windows sent and random number generator returning the maximum value`() {
        every { secureRandom.nextInt(any()) } returns 13

        val result = testSubject.invoke(numberOfExposureWindowsSent = 0)

        assertEquals(expected = 15, actual = result)

        verify(exactly = 1) { secureRandom.nextInt(UPPER_BOUND) }
    }

    @Test
    fun `returns a minimum of two with zero actual exposure windows sent and random number generator returning zero`() {
        every { secureRandom.nextInt(any()) } returns 0

        val result = testSubject.invoke(numberOfExposureWindowsSent = 0)

        assertEquals(expected = 2, actual = result)

        verify(exactly = 1) { secureRandom.nextInt(UPPER_BOUND) }
    }

    @Test
    fun `returns zero if number of actual exposure windows sent is 15`() {
        every { secureRandom.nextInt(any()) } returns 13

        val result = testSubject.invoke(numberOfExposureWindowsSent = 15)

        assertEquals(expected = 0, actual = result)

        verify(exactly = 1) { secureRandom.nextInt(UPPER_BOUND) }
    }

    @Test
    fun `returns zero if number of actual exposure windows sent is above 15`() {
        every { secureRandom.nextInt(any()) } returns 13

        val result = testSubject.invoke(numberOfExposureWindowsSent = 16)

        assertEquals(expected = 0, actual = result)

        verify(exactly = 1) { secureRandom.nextInt(UPPER_BOUND) }
    }

    @Test
    fun `returns two if number of total number of exposure windows is four and number of actual exposure windows sent is two`() {
        every { secureRandom.nextInt(any()) } returns 2

        val result = testSubject.invoke(numberOfExposureWindowsSent = 2)

        assertEquals(expected = 2, actual = result)

        verify(exactly = 1) { secureRandom.nextInt(UPPER_BOUND) }
    }

    companion object {
        private const val UPPER_BOUND = 14
    }
}
