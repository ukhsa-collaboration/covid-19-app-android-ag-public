package uk.nhs.nhsx.covid19.android.app.analytics

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.analytics.CalculateMissingSubmissionDays.Companion.SUBMISSION_LOG_CHECK_RANGE_MAX
import uk.nhs.nhsx.covid19.android.app.analytics.ConsumeOldestAnalyticsResult.Consumed
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.remote.AnalyticsApi
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubmitAnalytics @Inject constructor(
    private val analyticsApi: AnalyticsApi,
    private val migrateMetricsLogStorageToLogStorage: MigrateMetricsLogStorageToLogStorage,
    private val analyticsSubmissionLogStorage: AnalyticsSubmissionLogStorage,
    private val consumeOldestAnalytics: ConsumeOldestAnalytics,
    private val createAnalyticsPayload: CreateAnalyticsPayload
) {
    private val mutex = Mutex()

    suspend operator fun invoke(
        onAfterSubmission: suspend () -> Unit = {}
    ): Result<Unit> =
        runSafely {
            mutex.withLock {
                withContext(NonCancellable) {
                    migrateMetricsLogStorageToLogStorage()

                    // Consume analytics chronologically up to yesterday
                    // Abort on failure, not successfully submitted analytics are lost
                    var oldestAnalyticsResult = consumeOldestAnalytics.invoke()
                    while (oldestAnalyticsResult is Consumed) {
                        val analyticsPayload = createAnalyticsPayload(oldestAnalyticsResult.analyticsEventsGroup)
                        analyticsApi.submitAnalytics(analyticsPayload)
                        analyticsSubmissionLogStorage.addDate(analyticsPayload.analyticsWindow.startDateToLocalDate())
                        cleanupAnalyticsSubmissionLogs(analyticsPayload.analyticsWindow.startDateToLocalDate())
                        onAfterSubmission()
                        oldestAnalyticsResult = consumeOldestAnalytics.invoke()
                    }
                }
            }
        }

    private fun cleanupAnalyticsSubmissionLogs(currentPackageDate: LocalDate) {
        analyticsSubmissionLogStorage.removeBeforeOrEqual(
            currentPackageDate.minusDays(SUBMISSION_LOG_CHECK_RANGE_MAX.toLong())
        )
    }
}
