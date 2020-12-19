package uk.nhs.nhsx.covid19.android.app.analytics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsWindow
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.util.defaultFalse
import uk.nhs.nhsx.covid19.android.app.util.toISOSecondsFormat
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlin.collections.Map.Entry

class GroupAnalyticsEvents @Inject constructor(
    private val analyticsLogStorage: AnalyticsLogStorage,
    private val metadataProvider: MetadataProvider,
    private val updateStatusStorage: UpdateStatusStorage,
    private val getAnalyticsWindow: GetAnalyticsWindow,
    private val clock: Clock
) {

    suspend operator fun invoke(): Result<List<AnalyticsPayload>> =
        withContext(Dispatchers.IO) {
            runSafely {
                Timber.d("grouping analytics")

                val groupedMetrics: List<Pair<Metrics, AnalyticsWindow>> =
                    analyticsLogStorage.value
                        .groupBy { (instant, _) -> getAnalyticsWindow(instant) }
                        .filterNot { isToday(it.key) }
                        .map { entry: Entry<AnalyticsWindow, List<AnalyticsLogEntry>> ->
                            entry.value.toMetrics() to entry.key
                        }

                groupedMetrics.map { (metrics, analyticsWindow) ->
                    AnalyticsPayload(
                        analyticsWindow = analyticsWindow,
                        metrics = metrics,
                        metadata = metadataProvider.getMetadata(),
                        includesMultipleApplicationVersions = updateStatusStorage.value.defaultFalse()
                    )
                }
            }
        }

    private fun isToday(analyticsWindow: AnalyticsWindow): Boolean {
        return analyticsWindow == getAnalyticsWindow(Instant.now(clock))
    }

    private fun getAnalyticsWindow(instant: Instant): AnalyticsWindow {
        val window = getAnalyticsWindow.invoke(instant)

        return AnalyticsWindow(
            startDate = window.first.toISOSecondsFormat(),
            endDate = window.second.toISOSecondsFormat()
        )
    }
}
