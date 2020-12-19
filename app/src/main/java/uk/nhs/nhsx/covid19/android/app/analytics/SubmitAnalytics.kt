package uk.nhs.nhsx.covid19.android.app.analytics

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.remote.AnalyticsApi
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubmitAnalytics @Inject constructor(
    private val analyticsLogStorage: AnalyticsLogStorage,
    private val analyticsApi: AnalyticsApi,
    private val groupAnalyticsEvents: GroupAnalyticsEvents,
    private val migrateMetricsLogStorageToLogStorage: MigrateMetricsLogStorageToLogStorage
) {
    private val mutex = Mutex()

    suspend operator fun invoke(onAfterSubmission: suspend () -> Unit = {}): Result<Unit> =
        runSafely {
            mutex.withLock {
                withContext(NonCancellable) {
                    migrateMetricsLogStorageToLogStorage()

                    val analyticsPayloads = groupAnalyticsEvents
                        .invoke()
                        .getOrThrow()

                    analyticsPayloads.forEach { payload ->
                        analyticsLogStorage.remove(
                            startInclusive = Instant.parse(payload.analyticsWindow.startDate),
                            endExclusive = Instant.parse(payload.analyticsWindow.endDate)
                        )

                        runCatching {
                            analyticsApi.submitAnalytics(payload)
                        }

                        onAfterSubmission()
                    }
                }
            }
        }
}
