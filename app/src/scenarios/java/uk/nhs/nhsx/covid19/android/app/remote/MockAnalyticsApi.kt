package uk.nhs.nhsx.covid19.android.app.remote

import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsPayload

class MockAnalyticsApi : AnalyticsApi {
    override suspend fun submitAnalytics(analyticsPayload: AnalyticsPayload) {}
}
