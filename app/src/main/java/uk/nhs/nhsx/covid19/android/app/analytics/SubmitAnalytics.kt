package uk.nhs.nhsx.covid19.android.app.analytics

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uk.nhs.nhsx.covid19.android.app.analytics.legacy.AggregateAnalytics
import uk.nhs.nhsx.covid19.android.app.analytics.legacy.AnalyticsAlarm
import uk.nhs.nhsx.covid19.android.app.analytics.legacy.AnalyticsEventsStorage
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.remote.AnalyticsApi
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubmitAnalytics @Inject constructor(
    private val analyticsMetricsLogStorage: AnalyticsMetricsLogStorage,
    private val analyticsApi: AnalyticsApi,
    private val groupAnalyticsEvents: GroupAnalyticsEvents,
    private val aggregateAnalytics: AggregateAnalytics,
    private val analyticsEventsStorage: AnalyticsEventsStorage,
    private val analyticsAlarm: AnalyticsAlarm
) {
    private val mutex = Mutex()

    suspend operator fun invoke(): Result<Unit> =
        runSafely {
            mutex.withLock {
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
