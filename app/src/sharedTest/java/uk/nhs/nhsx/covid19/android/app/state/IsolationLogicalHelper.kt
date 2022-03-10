package uk.nhs.nhsx.covid19.android.app.state

import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.IndexInfo.NegativeTest
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.NeverIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.OptOutOfContactIsolation
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import java.time.Clock
import java.time.LocalDate

class IsolationLogicalHelper(
    val clock: Clock,
    val isolationConfiguration: IsolationConfiguration = IsolationConfiguration()
) {

    fun neverInIsolation(): IsolationLogicalState =
        NeverIsolating(isolationConfiguration, negativeTest = null)

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

    fun contactCaseWithOptOutDate(
        exposureDate: LocalDate = LocalDate.now(clock).minusDays(2),
        optOutOfContactIsolation: LocalDate
    ): ContactCase =
        ContactCase(
            exposureDate = exposureDate,
            notificationDate = exposureDate.plusDays(1),
            expiryDate = optOutOfContactIsolation,
            optOutOfContactIsolation = OptOutOfContactIsolation(optOutOfContactIsolation)
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
            selfAssessment = SelfAssessment(selfAssessmentDate, onsetDate),
            testResult = testResult,
            startDate = selfAssessmentDate,
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
            testResult = testResult,
            startDate = testResult.testEndDate,
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

fun IsolationLogicalState.addTestResult(testResult: AcknowledgedTestResult): IsolationLogicalState =
    (this as PossiblyIsolating).copy(indexInfo = (indexInfo as IndexCase).addTestResult(testResult))

fun IndexCase.addTestResult(testResult: AcknowledgedTestResult): IndexCase =
    copy(testResult = testResult)

fun ContactCase.asIsolation(
    hasAcknowledgedEndOfIsolation: Boolean = false,
    isolationConfiguration: IsolationConfiguration = IsolationConfiguration()
): IsolationLogicalState =
    PossiblyIsolating(
        isolationConfiguration,
        contactCase = this,
        hasAcknowledgedEndOfIsolation = hasAcknowledgedEndOfIsolation,
        startDate = startDate,
        expiryDate = expiryDate
    )

fun IndexCase.asIsolation(
    hasAcknowledgedEndOfIsolation: Boolean = false,
    isolationConfiguration: IsolationConfiguration = IsolationConfiguration()
): IsolationLogicalState =
    PossiblyIsolating(
        isolationConfiguration,
        indexInfo = this,
        hasAcknowledgedEndOfIsolation = hasAcknowledgedEndOfIsolation,
        startDate = startDate,
        expiryDate = expiryDate
    )

fun NegativeTest.asIsolation(
    isolationConfiguration: IsolationConfiguration = IsolationConfiguration()
): IsolationLogicalState =
    NeverIsolating(isolationConfiguration, negativeTest = this)
