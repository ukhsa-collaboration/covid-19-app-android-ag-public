package uk.nhs.nhsx.covid19.android.app.analytics

import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult

sealed class AnalyticsEvent {
    object QrCodeCheckIn : AnalyticsEvent()
    object CanceledCheckIn : AnalyticsEvent()
    object CompletedQuestionnaireAndStartedIsolation : AnalyticsEvent()
    object CompletedQuestionnaireButDidNotStartIsolation : AnalyticsEvent()
    object BackgroundTaskCompletion : AnalyticsEvent()
    object PositiveResultReceived : AnalyticsEvent()
    object NegativeResultReceived : AnalyticsEvent()
    object VoidResultReceived : AnalyticsEvent()
    data class ResultReceived(val result: VirologyTestResult, val testOrderType: TestOrderType) : AnalyticsEvent()
    object UpdateNetworkStats : AnalyticsEvent()
}
