package uk.nhs.nhsx.covid19.android.app.state

import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.PositiveResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedUnconfirmedPositiveTestResult
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.VoidResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventTracker
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.PLOD
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import javax.inject.Inject

class TrackTestResultAnalyticsOnReceive @Inject constructor(
    private val analyticsEventTracker: AnalyticsEventTracker
) {

    operator fun invoke(
        receivedTestResult: ReceivedTestResult,
        testOrderType: TestOrderType
    ) {
        when (receivedTestResult.testResult) {
            POSITIVE -> {
                analyticsEventTracker.track(PositiveResultReceived)
                if (receivedTestResult.requiresConfirmatoryTest) {
                    analyticsEventTracker.track(ReceivedUnconfirmedPositiveTestResult)
                }
            }
            NEGATIVE -> analyticsEventTracker.track(NegativeResultReceived)
            VOID -> analyticsEventTracker.track(VoidResultReceived)
            PLOD -> {
            }
        }
        receivedTestResult.testKitType?.let {
            analyticsEventTracker.track(
                ResultReceived(
                    receivedTestResult.testResult,
                    receivedTestResult.testKitType,
                    testOrderType
                )
            )
        }
    }
}
