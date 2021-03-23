package uk.nhs.nhsx.covid19.android.app.analytics

import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsWindow
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

class CalculateMissingSubmissionDays @Inject constructor(
    private val analyticsSubmissionLogStorage: AnalyticsSubmissionLogStorage
) {
    private val rangeToCheck = 1..SUBMISSION_LOG_CHECK_RANGE_MAX

    operator fun invoke(analyticsWindow: AnalyticsWindow): Int {
        val analyticsWindowDate = analyticsWindow.startDateToLocalDate()
        val submissionLog = analyticsSubmissionLogStorage.getLogForAnalyticsWindow(analyticsWindowDate)
        var missedPacketsLast7Days = 0
        for (i in rangeToCheck) {
            val dateToCheck = analyticsWindowDate.minusDays(i.toLong())
            if (!submissionLog.contains(dateToCheck)) {
                missedPacketsLast7Days++
            }
        }
        return missedPacketsLast7Days
    }

    companion object {
        const val SUBMISSION_LOG_CHECK_RANGE_MAX = 7
    }
}

fun AnalyticsWindow.startDateToLocalDate(): LocalDate {
    return Instant.parse(startDate).toLocalDate(ZoneOffset.UTC)
}
