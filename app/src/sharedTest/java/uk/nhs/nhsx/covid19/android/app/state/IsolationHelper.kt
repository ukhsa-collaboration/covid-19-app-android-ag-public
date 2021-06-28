package uk.nhs.nhsx.covid19.android.app.state

import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.PositiveTestResult
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.NegativeTest
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import java.time.Clock
import java.time.LocalDate

class IsolationHelper(
    val clock: Clock,
    val isolationConfiguration: DurationDays = DurationDays()
) {

    fun neverInIsolation(): IsolationState =
        IsolationState(isolationConfiguration)

    fun contactCase(expired: Boolean = false): ContactCase {
        val exposureDate = LocalDate.now(clock).minusDays(
            2 + if (expired) isolationConfiguration.contactCase.toLong() else 0
        )
        return contactCase(exposureDate)
    }

    fun contactCase(
        exposureDate: LocalDate,
        notificationDate: LocalDate = exposureDate.plusDays(1),
        expiryDate: LocalDate = exposureDate.plusDays(isolationConfiguration.contactCase.toLong())
    ): ContactCase =
        ContactCase(
            exposureDate = exposureDate,
            notificationDate = notificationDate,
            expiryDate = expiryDate
        )

    fun contactCaseWithDct(
        exposureDate: LocalDate = LocalDate.now(clock).minusDays(2),
        dailyContactTestingOptInDate: LocalDate
    ): ContactCase =
        ContactCase(
            exposureDate = exposureDate,
            notificationDate = exposureDate.plusDays(1),
            expiryDate = dailyContactTestingOptInDate,
            dailyContactTestingOptInDate = dailyContactTestingOptInDate
        )

    fun selfAssessment(
        expired: Boolean = false,
        onsetDate: LocalDate? = null,
        testResult: AcknowledgedTestResult? = null
    ): IndexCase {
        val selfAssessmentDate = LocalDate.now(clock).minusDays(
            2 + if (expired) isolationConfiguration.indexCaseSinceSelfDiagnosisUnknownOnset.toLong() else 0
        )
        return selfAssessment(selfAssessmentDate, onsetDate, testResult)
    }

    fun selfAssessment(
        selfAssessmentDate: LocalDate,
        onsetDate: LocalDate? = null,
        testResult: AcknowledgedTestResult? = null,
        expiryDate: LocalDate =
            if (onsetDate != null) onsetDate.plusDays(isolationConfiguration.indexCaseSinceSelfDiagnosisOnset.toLong())
            else selfAssessmentDate.plusDays(isolationConfiguration.indexCaseSinceSelfDiagnosisUnknownOnset.toLong())
    ): IndexCase =
        IndexCase(
            isolationTrigger = SelfAssessment(selfAssessmentDate, onsetDate),
            testResult = testResult,
            expiryDate = expiryDate
        )

    fun positiveTest(
        testResult: AcknowledgedTestResult
    ): IndexCase {
        if (testResult.testResult != RelevantVirologyTestResult.POSITIVE) {
            throw IllegalArgumentException("This function can only be called with a positive test result")
        }
        val testEndDay = testResult.testEndDate
        return IndexCase(
            isolationTrigger = PositiveTestResult(testEndDay),
            testResult = testResult,
            expiryDate = testEndDay.plusDays(isolationConfiguration.indexCaseSinceTestResultEndDate.toLong())
        )
    }

    fun negativeTest(testResult: AcknowledgedTestResult): NegativeTest {
        if (testResult.testResult != RelevantVirologyTestResult.NEGATIVE) {
            throw IllegalArgumentException("This function can only be called with a negative test result")
        }
        return NegativeTest(testResult)
    }
}

fun IsolationState.addTestResultToIndexCase(testResult: AcknowledgedTestResult): IsolationState =
    copy(indexInfo = (indexInfo as IndexCase).addTestResult(testResult))

fun IndexCase.addTestResult(testResult: AcknowledgedTestResult): IndexCase =
    copy(testResult = testResult)

fun ContactCase.asIsolation(
    hasAcknowledgedEndOfIsolation: Boolean = false,
    isolationConfiguration: DurationDays = DurationDays()
): IsolationState =
    IsolationState(
        isolationConfiguration,
        contactCase = this,
        hasAcknowledgedEndOfIsolation = hasAcknowledgedEndOfIsolation
    )

fun IndexInfo.asIsolation(
    hasAcknowledgedEndOfIsolation: Boolean = false,
    isolationConfiguration: DurationDays = DurationDays()
): IsolationState =
    IsolationState(
        isolationConfiguration,
        indexInfo = this,
        hasAcknowledgedEndOfIsolation = hasAcknowledgedEndOfIsolation
    )

fun IsolationState.asLogical(): IsolationLogicalState =
    IsolationLogicalState.from(this)
