package uk.nhs.nhsx.covid19.android.app.analytics

import uk.nhs.nhsx.covid19.android.app.analytics.legacy.AggregateAnalytics
import uk.nhs.nhsx.covid19.android.app.analytics.legacy.AnalyticsAlarm
import uk.nhs.nhsx.covid19.android.app.analytics.legacy.AnalyticsEventsStorage
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.remote.AnalyticsApi
import java.time.Instant
import javax.inject.Inject

class SubmitAnalytics @Inject constructor(
    private val analyticsMetricsLogStorage: AnalyticsMetricsLogStorage,
    private val analyticsApi: AnalyticsApi,
    private val groupAnalyticsEvents: GroupAnalyticsEvents,
    private val aggregateAnalytics: AggregateAnalytics,
    private val analyticsEventsStorage: AnalyticsEventsStorage,
    private val analyticsAlarm: AnalyticsAlarm
) {

    suspend operator fun invoke(): Result<Unit> =
        runSafely {

            handleMigration()

            val analyticsEvents = groupAnalyticsEvents
                .invoke()
                .getOrThrow()

            analyticsEvents.forEach {
                runCatching {
                    analyticsApi.submitAnalytics(it)
                }
                analyticsMetricsLogStorage.remove(
                    startInclusive = Instant.parse(it.analyticsWindow.startDate),
                    endExclusive = Instant.parse(it.analyticsWindow.endDate)
                )
            }
        }

    private suspend fun handleMigration() {
        analyticsAlarm.cancel()
        aggregateAnalytics.invoke()
        analyticsEventsStorage.value?.let {
            it.forEach {
                runCatching {
                    analyticsApi.submitAnalytics(it)
                }
            }
            analyticsEventsStorage.value = null
        }
    }
}
