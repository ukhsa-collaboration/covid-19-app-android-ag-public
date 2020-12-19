package uk.nhs.nhsx.covid19.android.app.analytics

import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.remote.AnalyticsApi
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsWindow
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.util.toISOSecondsFormat
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class SubmitOnboardingAnalytics @Inject constructor(
    private val analyticsApi: AnalyticsApi,
    private val metadataProvider: MetadataProvider,
    private val clock: Clock
) {

    suspend operator fun invoke(): Result<Unit> =
        runSafely {
            val startOfToday = Instant.now(clock).truncatedTo(ChronoUnit.DAYS).toISOSecondsFormat()
            val analyticsPayload = AnalyticsPayload(
                analyticsWindow = AnalyticsWindow(
                    startOfToday, startOfToday
                ),
                includesMultipleApplicationVersions = false,
                metadata = metadataProvider.getMetadata(),
                metrics = Metrics(completedOnboarding = 1)
            )
            analyticsApi.submitAnalytics(analyticsPayload)
        }
}
