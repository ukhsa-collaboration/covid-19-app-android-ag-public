package uk.nhs.nhsx.covid19.android.app.analytics

import uk.nhs.nhsx.covid19.android.app.analytics.legacy.AggregateAnalytics
import uk.nhs.nhsx.covid19.android.app.analytics.legacy.AnalyticsEventsStorage
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.remote.AnalyticsApi
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsWindow
import uk.nhs.nhsx.covid19.android.app.util.toISOSecondsFormat
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class SubmitAnalytics(
    private val analyticsMetricsLogStorage: AnalyticsMetricsLogStorage,
    private val analyticsApi: AnalyticsApi,
    private val groupAnalyticsEvents: GroupAnalyticsEvents,
    private val aggregateAnalytics: AggregateAnalytics,
    private val analyticsEventsStorage: AnalyticsEventsStorage,
    private val clock: Clock
) {

    @Inject
    constructor(
        analyticsMetricsLogStorage: AnalyticsMetricsLogStorage,
        analyticsApi: AnalyticsApi,
        groupAnalyticsEvents: GroupAnalyticsEvents,
        aggregateAnalytics: AggregateAnalytics,
        analyticsEventsStorage: AnalyticsEventsStorage
    ) : this(
        analyticsMetricsLogStorage,
        analyticsApi,
        groupAnalyticsEvents,
        aggregateAnalytics,
        analyticsEventsStorage,
        Clock.systemUTC()
    )

    suspend operator fun invoke(isOnboardingAnalyticsEvent: Boolean): Result<Unit> =
        runSafely {

            handleMigration()

            val analyticsEvents = groupAnalyticsEvents
                .invoke(shallIncludeCurrentWindow = isOnboardingAnalyticsEvent)
                .getOrThrow()

            analyticsEvents.forEach {
                runCatching {
                    val analyticsPayload = if (isOnboardingAnalyticsEvent) {
                        analyticsPayloadForOnboarding(it)
                    } else {
                        it
                    }
                    analyticsApi.submitAnalytics(analyticsPayload)
                }
                analyticsMetricsLogStorage.remove(
                    startInclusive = Instant.parse(it.analyticsWindow.startDate),
                    endExclusive = Instant.parse(it.analyticsWindow.endDate)
                )
            }
        }

    private suspend fun handleMigration() {
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

    private fun analyticsPayloadForOnboarding(analyticsPayload: AnalyticsPayload): AnalyticsPayload {
        val startOfToday = Instant.now(clock).truncatedTo(ChronoUnit.DAYS).toISOSecondsFormat()
        return analyticsPayload.copy(
            analyticsWindow = AnalyticsWindow(
                startDate = startOfToday,
                endDate = startOfToday
            )
        )
    }
}
