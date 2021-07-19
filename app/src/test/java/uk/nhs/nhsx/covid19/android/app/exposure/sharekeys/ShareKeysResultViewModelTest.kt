package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SuccessfullySharedExposureKeys
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor

class ShareKeysResultViewModelTest {

    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxUnitFun = true)

    val testSubject = ShareKeysResultViewModel(
        analyticsEventProcessor = analyticsEventProcessor
    )

    @Test
    fun `tracks SuccessfullySharedExposureKeys once in onCreate method`() {
        testSubject.onCreate()
        testSubject.onCreate()

        verify(exactly = 1) { analyticsEventProcessor.track(SuccessfullySharedExposureKeys) }
    }
}
