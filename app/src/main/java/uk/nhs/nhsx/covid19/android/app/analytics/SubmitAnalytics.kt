package uk.nhs.nhsx.covid19.android.app.analytics

import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.remote.AnalyticsApi
import javax.inject.Inject

class SubmitAnalytics @Inject constructor(
    private val analyticsEventsStorage: AnalyticsEventsStorage,
    private val analyticsApi: AnalyticsApi
) {

    suspend operator fun invoke(): Result<Unit> = runSafely {

        runCatching { analyticsEventsStorage.value?.forEach { analyticsApi.submitAnalytics(it) } }

        analyticsEventsStorage.value = null
    }
}
