package uk.nhs.nhsx.covid19.android.app.analytics

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.CalculateMissingSubmissionDays.Companion.SUBMISSION_LOG_CHECK_RANGE_MAX
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.remote.AnalyticsApi
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsWindow
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubmitAnalytics @Inject constructor(
    private val analyticsLogStorage: AnalyticsLogStorage,
    private val analyticsApi: AnalyticsApi,
    private val groupAnalyticsEvents: GroupAnalyticsEvents,
    private val migrateMetricsLogStorageToLogStorage: MigrateMetricsLogStorageToLogStorage,
    private val analyticsSubmissionLogStorage: AnalyticsSubmissionLogStorage,
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

                    val eventGroups = groupAnalyticsEvents
                        .invoke()
                        .getOrThrow()

                    eventGroups
                        .sortedBy { it.analyticsWindow.startDate }
                        .forEach { group ->
                            cleanupAnalyticsData(group.analyticsWindow)

                            try {
                                analyticsApi.submitAnalytics(createAnalyticsPayload(group))
                                analyticsSubmissionLogStorage.add(group.analyticsWindow.startDateToLocalDate())
                                cleanupAnalyticsSubmissionLogs(group.analyticsWindow.startDateToLocalDate())
                                onAfterSubmission()
                            } catch (e: Throwable) {
                                Timber.e(e, "Analytics submission failed. Aborting task.")
                                return@withContext
                            }
                        }
                }
            }
        }

    private fun cleanupAnalyticsData(analyticsWindow: AnalyticsWindow) {
        analyticsLogStorage.remove(
            startInclusive = Instant.parse(analyticsWindow.startDate),
            endExclusive = Instant.parse(analyticsWindow.endDate)
        )
    }

    private fun cleanupAnalyticsSubmissionLogs(currentPackageDate: LocalDate) {
        analyticsSubmissionLogStorage.removeBeforeOrEqual(
            currentPackageDate.minusDays(SUBMISSION_LOG_CHECK_RANGE_MAX.toLong())
        )
    }
}
