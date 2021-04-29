package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.SubmitEmptyData
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitFakeExposureWindows

class SubmitObfuscationDataTest {
    private val submitEmptyData = mockk<SubmitEmptyData>(relaxUnitFun = true)
    private val submitFakeExposureWindows = mockk<SubmitFakeExposureWindows>(relaxUnitFun = true)

    private val testSubject = SubmitObfuscationData(submitEmptyData, submitFakeExposureWindows)

    @Test
    fun `invoking SubmitFakeData triggers empty data and fake exposure window submission`() {
        testSubject()

        verify(exactly = 1) {
            submitEmptyData()
            submitFakeExposureWindows()
        }
    }
}
