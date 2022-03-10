package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import uk.nhs.nhsx.covid19.android.app.state.GetLatestConfiguration
import uk.nhs.nhsx.covid19.android.app.util.isBeforeOrEqual
import uk.nhs.nhsx.covid19.android.app.util.selectNewest
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

class CalculateKeySubmissionDateRange @Inject constructor(
    private val getLatestConfiguration: GetLatestConfiguration,
    val clock: Clock
) {

    operator fun invoke(acknowledgedDate: Instant, assumedOnsetDate: LocalDate): SubmissionDateRange {
        val onsetDateBasedStartDate = assumedOnsetDate.minusDays(DAYS_PRIOR_ONSET_DATE_FOR_FIRST_SUBMISSION)
        val contactCaseIsolationDuration = getLatestConfiguration().contactCase.toLong()
        val isolationDurationBasedStartDate =
            LocalDate.now(clock).minusDays(contactCaseIsolationDuration - 1) // Minus 1 for excluding today

        val firstSubmissionDate = selectNewest(onsetDateBasedStartDate, isolationDurationBasedStartDate)
        val lastSubmissionDate =
            acknowledgedDate.toLocalDate(ZoneOffset.UTC).minusDays(DAYS_PRIOR_ACKNOWLEDGE_DATE_FOR_LAST_SUBMISSION)

        return SubmissionDateRange(firstSubmissionDate, lastSubmissionDate)
    }

    companion object {
        const val DAYS_PRIOR_ONSET_DATE_FOR_FIRST_SUBMISSION = 2L
        const val DAYS_PRIOR_ACKNOWLEDGE_DATE_FOR_LAST_SUBMISSION = 1L
    }
}

data class SubmissionDateRange(
    val firstSubmissionDate: LocalDate,
    val lastSubmissionDate: LocalDate
) {
    fun includes(keyDate: LocalDate): Boolean {
        return firstSubmissionDate.isBeforeOrEqual(keyDate) && keyDate.isBeforeOrEqual(lastSubmissionDate)
    }

    fun containsAtLeastOneDay(): Boolean {
        return firstSubmissionDate.isBeforeOrEqual(lastSubmissionDate)
    }
}
