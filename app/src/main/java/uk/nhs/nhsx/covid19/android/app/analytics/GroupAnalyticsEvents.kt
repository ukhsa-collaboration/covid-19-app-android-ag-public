package uk.nhs.nhsx.covid19.android.app.analytics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsWindow
import uk.nhs.nhsx.covid19.android.app.util.toISOSecondsFormat
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlin.collections.Map.Entry

class GroupAnalyticsEvents @Inject constructor(
    private val analyticsLogStorage: AnalyticsLogStorage,
    private val getAnalyticsWindow: GetAnalyticsWindow,
    private val clock: Clock
) {

    suspend operator fun invoke(): Result<List<AnalyticsEventsGroup>> =
        withContext(Dispatchers.IO) {
            runSafely {
                Timber.d("grouping analytics")
                analyticsLogStorage.value
                    .groupBy { (instant, _) -> getAnalyticsWindow(instant) }
                    .filterNot { isToday(it.key) }
                    .map { entry: Entry<AnalyticsWindow, List<AnalyticsLogEntry>> ->
                        AnalyticsEventsGroup(entry.key, entry.value)
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

data class AnalyticsEventsGroup(
    val analyticsWindow: AnalyticsWindow,
    val entries: List<AnalyticsLogEntry>
)
