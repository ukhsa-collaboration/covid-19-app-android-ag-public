package uk.nhs.nhsx.covid19.android.app.analytics

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

class AnalyticsEventTrackerTest {

    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxUnitFun = true)
    private val testCoroutineScope = TestCoroutineScope()

    private val testSubject = AnalyticsEventTracker(
        analyticsEventProcessor,
        testCoroutineScope,
    )

    @Test
    fun `calling track delegates tracking of analytics event to AnalyticsEventProcessor`() =
        testCoroutineScope.runBlockingTest {
            val expected = AnalyticsEvent.ReceivedRiskyContactNotification

            testSubject.track(expected)

            coVerify { analyticsEventProcessor.track(expected) }
        }
}
