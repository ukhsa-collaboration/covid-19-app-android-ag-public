package uk.nhs.nhsx.covid19.android.app.state

import com.squareup.moshi.JsonClass
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import java.time.LocalDate

const val assumedDaysFromOnsetToSelfAssessment: Long = 2

/**
 * Low-level isolation state, containing all information relevant to the isolation. This is intended to be used only
 * by classes that are very close to the isolation state machine and state storage. For a higher-level representation
 * of the isolation state use [IsolationLogicalState] instead.
 */
data class IsolationState(
    val isolationConfiguration: DurationDays,
    val selfAssessment: SelfAssessment? = null,
    val testResult: AcknowledgedTestResult? = null,
    val contact: Contact? = null,
    val hasAcknowledgedEndOfIsolation: Boolean = false
) {

    val assumedOnsetDateForExposureKeys: LocalDate?
        get() =
            when {
                testResult != null && testResult.isPositive() &&
                    (selfAssessment == null || testResult.testEndDate.isBefore(selfAssessment.assumedOnsetDate)) ->
                    testResult.testEndDate
                selfAssessment != null ->
                    selfAssessment.assumedOnsetDate
                else ->
                    null
            }

    @JsonClass(generateAdapter = true)
    data class Contact(
        val exposureDate: LocalDate,
        val notificationDate: LocalDate,
        val optOutOfContactIsolation: OptOutOfContactIsolation? = null
    )

    @JsonClass(generateAdapter = true)
    data class OptOutOfContactIsolation(
        val date: LocalDate,
    )

    data class SelfAssessment(
        val selfAssessmentDate: LocalDate,
        val onsetDate: LocalDate? = null
    ) {
        val assumedOnsetDate: LocalDate
            get() = onsetDate ?: selfAssessmentDate.minusDays(assumedDaysFromOnsetToSelfAssessment)
    }
}
