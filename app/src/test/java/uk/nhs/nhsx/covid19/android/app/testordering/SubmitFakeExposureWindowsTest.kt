package uk.nhs.nhsx.covid19.android.app.testordering

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.EmptyApi
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource.EXPOSURE_WINDOW
import java.security.SecureRandom

class SubmitFakeExposureWindowsTest {

    private val emptyApi = mockk<EmptyApi>(relaxed = true)
    private val testScope = TestCoroutineScope()
    private val testDispatcher = TestCoroutineDispatcher()

    private val randomNumberGenerator = mockk<SecureRandom>(relaxed = true)
    private val testSubject = SubmitFakeExposureWindows(emptyApi, randomNumberGenerator, testScope, testDispatcher)

    @Test
    fun `empty api is called random times at least twice`() = testScope.runBlockingTest {
        every { randomNumberGenerator.nextInt(any()) } returns 0

        testSubject.invoke(EXPOSURE_WINDOW, 0)

        coVerify(exactly = 1) { randomNumberGenerator.nextInt(UPPER_BOUND) }
        coVerify(exactly = 2) { emptyApi.submit(EmptySubmissionRequest(EXPOSURE_WINDOW)) }
    }

    @Test
    fun `empty api is called random times at most 15`() = testScope.runBlockingTest {
        every { randomNumberGenerator.nextInt(any()) } returns 13

        testSubject.invoke(EXPOSURE_WINDOW, 0)

        coVerify(exactly = 1) { randomNumberGenerator.nextInt(UPPER_BOUND) }
        coVerify(exactly = 15) { emptyApi.submit(EmptySubmissionRequest(EXPOSURE_WINDOW)) }
    }

    @Test
    fun `empty api is called random times at most 15 minus the number of stored epidemiology events`() =
        testScope.runBlockingTest {
            every { randomNumberGenerator.nextInt(any()) } returns 13

            testSubject.invoke(EXPOSURE_WINDOW, 5)

            coVerify(exactly = 1) { randomNumberGenerator.nextInt(UPPER_BOUND) }
            coVerify(exactly = 10) { emptyApi.submit(EmptySubmissionRequest(EXPOSURE_WINDOW)) }
        }

    @Test
    fun `empty api is not called when stored epidemiology events is 15 or more`() = testScope.runBlockingTest {
        every { randomNumberGenerator.nextInt(any()) } returns 13

        testSubject.invoke(EXPOSURE_WINDOW, 15)

        coVerify(exactly = 1) { randomNumberGenerator.nextInt(UPPER_BOUND) }
        coVerify(exactly = 0) { emptyApi.submit(EmptySubmissionRequest(EXPOSURE_WINDOW)) }
    }

    companion object {
        private const val UPPER_BOUND = 14
    }
}
