package uk.nhs.nhsx.covid19.android.app.analytics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.analytics.ConsumeOldestAnalyticsResult.Consumed
import uk.nhs.nhsx.covid19.android.app.analytics.ConsumeOldestAnalyticsResult.NothingToConsume
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsWindow
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

class ConsumeOldestAnalytics @Inject constructor(
    private val analyticsLogStorage: AnalyticsLogStorage,
    private val getAnalyticsWindow: GetAnalyticsWindow,
    private val nextAnalyticsWindowToSubmitStorage: NextAnalyticsWindowToSubmitStorage,
    private val clock: Clock
) {

    suspend operator fun invoke(): ConsumeOldestAnalyticsResult =
        withContext(Dispatchers.IO) {
            runCatching {
                // Get oldest window that has not been attempted to be transmitted
                val currentAnalyticsWindow = getAnalyticsWindow(nextAnalyticsWindowToSubmitStorage.windowStartDate ?: return@withContext NothingToConsume)
                // Return NothingToConsume if today is reached
                if (isToday(currentAnalyticsWindow)) return@withContext NothingToConsume

                // Fetch logEntries for that window, use empty list if there are none
                val entries = analyticsLogStorage.value
                    .groupBy { (instant, _) -> getAnalyticsWindow(instant) }
                    .getOrDefault(currentAnalyticsWindow, emptyList())

                // Remove entries for that window
                analyticsLogStorage.remove(
                    startInclusive = currentAnalyticsWindow.startDateToInstant(),
                    endExclusive = currentAnalyticsWindow.endDateToInstant()
                )

                // Update next window to be submitted to the end of the current window which is the start of the next window (next day midnight).
                nextAnalyticsWindowToSubmitStorage.windowStartDate = currentAnalyticsWindow.endDateToInstant()

                // Return consumed group
                Consumed(AnalyticsEventsGroup(currentAnalyticsWindow, entries))
            }.getOrElse { NothingToConsume }
        }

    private fun isToday(analyticsWindow: AnalyticsWindow): Boolean {
        return analyticsWindow == getAnalyticsWindow(Instant.now(clock))
    }

    private fun getAnalyticsWindow(instant: Instant): AnalyticsWindow {
        return getAnalyticsWindow.invoke(instant).toAnalyticsWindow()
    }
}

sealed class ConsumeOldestAnalyticsResult {
    object NothingToConsume : ConsumeOldestAnalyticsResult()
    data class Consumed(val analyticsEventsGroup: AnalyticsEventsGroup) : ConsumeOldestAnalyticsResult()
}

data class AnalyticsEventsGroup(
    val analyticsWindow: AnalyticsWindow,
    val entries: List<AnalyticsLogEntry>
)
