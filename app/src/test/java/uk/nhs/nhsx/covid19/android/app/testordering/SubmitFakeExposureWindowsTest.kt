package uk.nhs.nhsx.covid19.android.app.testordering

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.SubmitEmptyData
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource.EXPOSURE_WINDOW

class SubmitFakeExposureWindowsTest {

    private val submitEmptyData = mockk<SubmitEmptyData>(relaxed = true)
    private val getEmptyExposureWindowSubmissionCount = mockk<GetEmptyExposureWindowSubmissionCount>(relaxed = true)

    private val testSubject = SubmitFakeExposureWindows(submitEmptyData, getEmptyExposureWindowSubmissionCount)

    @Test
    fun `empty api is not called if empty exposure window submission count returns zero`() {
        every { getEmptyExposureWindowSubmissionCount.invoke(any()) } returns 0

        testSubject.invoke(EXPOSURE_WINDOW, 0)

        coVerify(exactly = 0) { submitEmptyData(EXPOSURE_WINDOW) }
    }

    @Test
    fun `empty api is called once if empty exposure window submission count returns one`() {
        every { getEmptyExposureWindowSubmissionCount.invoke(any()) } returns 1

        testSubject.invoke(EXPOSURE_WINDOW, 0)

        coVerify(exactly = 1) { submitEmptyData(EXPOSURE_WINDOW) }
    }

    @Test
    fun `empty api is called 15 times if empty exposure window submission count returns 15`() {
        every { getEmptyExposureWindowSubmissionCount.invoke(any()) } returns 15

        testSubject.invoke(EXPOSURE_WINDOW, 0)

        coVerify(exactly = 15) { submitEmptyData(EXPOSURE_WINDOW) }
    }
}
