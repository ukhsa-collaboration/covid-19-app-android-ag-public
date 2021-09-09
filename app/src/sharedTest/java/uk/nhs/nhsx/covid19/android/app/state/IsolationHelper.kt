package uk.nhs.nhsx.covid19.android.app.state

import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.Contact
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.OptOutOfContactIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import java.time.Clock
import java.time.LocalDate

class IsolationHelper(
    val clock: Clock,
    val isolationConfiguration: DurationDays = DurationDays()
) {

    fun neverInIsolation(): IsolationState =
        IsolationState(isolationConfiguration)

    fun contact(expired: Boolean = false): Contact {
        val exposureDate = LocalDate.now(clock).minusDays(
            2 + if (expired) isolationConfiguration.contactCase.toLong() else 0
        )
        return contact(exposureDate)
    }

    fun contact(
        exposureDate: LocalDate,
        notificationDate: LocalDate = exposureDate.plusDays(1)
    ): Contact =
        Contact(
            exposureDate = exposureDate,
            notificationDate = notificationDate
        )

    fun contactWithOptOutDate(
        exposureDate: LocalDate = LocalDate.now(clock).minusDays(2),
        optOutOfContactIsolation: LocalDate
    ): Contact =
        Contact(
            exposureDate = exposureDate,
            notificationDate = exposureDate.plusDays(1),
            optOutOfContactIsolation = OptOutOfContactIsolation(optOutOfContactIsolation)
        )

    fun selfAssessment(
        expired: Boolean = false,
        onsetDate: LocalDate? = null
    ): SelfAssessment {
        val selfAssessmentDate = LocalDate.now(clock).minusDays(
            2 + if (expired) isolationConfiguration.indexCaseSinceSelfDiagnosisUnknownOnset.toLong() else 0
        )
        return SelfAssessment(selfAssessmentDate, onsetDate)
    }
}

fun IsolationState.addTestResult(testResult: AcknowledgedTestResult): IsolationState =
    copy(testResult = testResult)

fun Contact.asIsolation(
    hasAcknowledgedEndOfIsolation: Boolean = false,
    isolationConfiguration: DurationDays = DurationDays()
): IsolationState =
    IsolationState(
        isolationConfiguration,
        contact = this,
        hasAcknowledgedEndOfIsolation = hasAcknowledgedEndOfIsolation
    )

fun SelfAssessment.asIsolation(
    hasAcknowledgedEndOfIsolation: Boolean = false,
    isolationConfiguration: DurationDays = DurationDays()
): IsolationState =
    IsolationState(
        isolationConfiguration,
        selfAssessment = this,
        hasAcknowledgedEndOfIsolation = hasAcknowledgedEndOfIsolation
    )

fun AcknowledgedTestResult.asIsolation(
    hasAcknowledgedEndOfIsolation: Boolean = false,
    isolationConfiguration: DurationDays = DurationDays()
): IsolationState =
    IsolationState(
        isolationConfiguration,
        testResult = this,
        hasAcknowledgedEndOfIsolation = hasAcknowledgedEndOfIsolation
    )
