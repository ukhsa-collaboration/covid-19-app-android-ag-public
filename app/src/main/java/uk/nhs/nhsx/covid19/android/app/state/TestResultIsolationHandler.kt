package uk.nhs.nhsx.covid19.android.app.state

import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.CalculateKeySubmissionDateRange
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfo
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.NeverIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.PositiveTestResult
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.NegativeTest
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.IsolationUpdate.Confirm
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.IsolationUpdate.Ignore
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.IsolationUpdate.Nothing
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.IsolationUpdate.Overwrite
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.IsolationUpdate.Update
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.DoNotTransition
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.Transition
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.toRelevantVirologyTestResult
import uk.nhs.nhsx.covid19.android.app.util.isBeforeOrEqual
import uk.nhs.nhsx.covid19.android.app.util.selectEarliest
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

class TestResultIsolationHandler @Inject constructor(
    private val calculateKeySubmissionDateRange: CalculateKeySubmissionDateRange,
    private val createSelfAssessmentIndexCase: CreateSelfAssessmentIndexCase,
    private val clock: Clock
) {

    fun computeTransitionWithTestResultAcknowledgment(
        currentState: IsolationLogicalState,
        receivedTestResult: ReceivedTestResult,
        testAcknowledgedDate: Instant
    ): TransitionDueToTestResult {
        val isolationUpdate = computeIsolationUpdate(currentState, receivedTestResult, testAcknowledgedDate)
        return applyIsolationUpdate(currentState.toIsolationState(), receivedTestResult, isolationUpdate, testAcknowledgedDate)
    }

    private fun computeIsolationUpdate(
        currentState: IsolationLogicalState,
        receivedTestResult: ReceivedTestResult,
        testAcknowledgedDate: Instant
    ): IsolationUpdate {
        val testInfo = receivedTestResult.toAcknowledgedTest(acknowledgedDate = testAcknowledgedDate)
            ?: return Nothing

        val indexCase = (currentState as? PossiblyIsolating)?.indexInfo as? IndexCase
        val isOrWasIndexCase = indexCase != null
        val previousTestResult = currentState.getTestResult()

        return when (receivedTestResult.testResult) {
            VOID -> Nothing

            NEGATIVE ->
                if (isNegativeTestIrrelevant(receivedTestResult, currentState)) Nothing
                else if (!isOrWasIndexCase) Overwrite(testInfo)
                else when (previousTestResult?.testResult) {
                    RelevantVirologyTestResult.NEGATIVE -> Nothing
                    RelevantVirologyTestResult.POSITIVE ->
                        if (previousTestResult.isConfirmed()) Nothing
                        else Overwrite(testInfo)
                    else -> Update(testInfo)
                }

            POSITIVE ->
                when {
                    // If the app knows about an existing (active or expired) isolation, and “positive test isolation end
                    // date” <= “assumed isolation start date”, then test is considered “too old”
                    wouldTestIsolationEndBeforeOrOnStartOfExistingIsolation(receivedTestResult, currentState) -> Ignore

                    // If the app knows about symptoms, and “positive test end date” < “assumed symptom onset date”, then
                    // the positive test should be used for isolation
                    isTestOlderThanSelfAssessmentSymptoms(receivedTestResult, currentState) ->
                        getConfirmedDateIfNeeded(receivedTestResult, previousTestResult)?.let { confirmedDate ->
                            Overwrite(testInfo.confirm(confirmedDate))
                        } ?: Overwrite(testInfo)

                    // If the app knows about an existing (confirmed or unconfirmed) positive test, and “positive test end
                    // date” < “existing positive test end date”, then the newly entered positive test should be used for
                    // isolation
                    previousTestResult != null &&
                        previousTestResult.isPositive() &&
                        receivedTestResult.isOlderThan(previousTestResult) ->
                        when (indexCase?.isolationTrigger) {
                            is PositiveTestResult ->
                                getConfirmedDateIfNeeded(receivedTestResult, previousTestResult)?.let { confirmedDate ->
                                    Overwrite(testInfo.confirm(confirmedDate))
                                } ?: Overwrite(testInfo)
                            is SelfAssessment ->
                                getConfirmedDateIfNeeded(receivedTestResult, previousTestResult)?.let { confirmedDate ->
                                    Update(testInfo.confirm(confirmedDate))
                                } ?: Update(testInfo)
                            // This is not possible because it would mean index info is NegativeTest, so
                            // previousTestResult.isPositive() would return false
                            else -> Overwrite(testInfo)
                        }

                    // If the app knows about an existing negative test and “positive test end date” < “negative test end date”
                    previousTestResult != null &&
                        previousTestResult.testResult == RelevantVirologyTestResult.NEGATIVE &&
                        receivedTestResult.isOlderThan(previousTestResult) ->
                        if (receivedTestResult.isConfirmed()) {
                            when (indexCase?.isolationTrigger) {
                                is SelfAssessment -> Update(testInfo)
                                else -> Overwrite(testInfo)
                            }
                        } else Ignore

                    receivedTestResult.requiresConfirmatoryTest ->
                        if (indexCase?.hasExpired(clock) == false) Nothing
                        else Overwrite(testInfo)

                    else ->
                        if (!isOrWasIndexCase) Overwrite(testInfo)
                        else when (previousTestResult?.testResult) {
                            RelevantVirologyTestResult.NEGATIVE -> Overwrite(testInfo)
                            RelevantVirologyTestResult.POSITIVE ->
                                if (previousTestResult.isConfirmed()) Nothing
                                else Confirm(receivedTestResult.testEndDay(clock))
                            else -> Update(testInfo)
                        }
                }
        }
    }

    private fun applyIsolationUpdate(
        currentState: IsolationState,
        testResult: ReceivedTestResult,
        isolationUpdate: IsolationUpdate,
        testAcknowledgedDate: Instant
    ): TransitionDueToTestResult {
        if (isolationUpdate is Ignore) {
            val keySharingInfo = createKeySharingInfoOnTestAcknowledged(testResult, testAcknowledgedDate, currentState)
            return DoNotTransition(
                preventKeySubmission = isolationUpdate.preventKeySubmission,
                keySharingInfo
            )
        }

        // A confirmed positive test result (expired or not) removes any memory of a contact case (current or past)
        var newState =
            if (testResult.isConfirmed() && testResult.isPositive()) currentState.clearContactCase()
            else currentState

        newState = when (testResult.testResult) {
            POSITIVE ->
                when (isolationUpdate) {
                    is Update -> updateIndexCaseWithPositiveTestResult(newState, isolationUpdate.testInfoResult)
                    is Overwrite -> replaceIndexCaseWithPositiveTestResult(newState, isolationUpdate.testInfoResult)
                    is Confirm -> confirmTestResult(newState, isolationUpdate.confirmedDate)
                    else -> newState
                }
            NEGATIVE ->
                when (isolationUpdate) {
                    is Update -> newState.expireIndexCaseWithNegativeTestResult(isolationUpdate.testInfoResult.acknowledgedTestResult)
                    is Overwrite -> newState.copy(indexInfo = NegativeTest(isolationUpdate.testInfoResult.acknowledgedTestResult))
                    is Confirm -> {
                        Timber.e("Cannot confirm using a negative test result")
                        newState
                    }
                    else -> newState
                }
            VOID -> newState
        }

        val keySharingInfo = createKeySharingInfoOnTestAcknowledged(testResult, testAcknowledgedDate, newState)

        return if (newState == currentState) DoNotTransition(preventKeySubmission = false, keySharingInfo)
        else Transition(newState, keySharingInfo)
    }

    private fun ReceivedTestResult.toAcknowledgedTest(acknowledgedDate: Instant): TestInfo? =
        testResult.toRelevantVirologyTestResult()?.let { virologyTestResult ->
            TestInfo(
                receivedTestResult = this,
                acknowledgedTestResult =
                    AcknowledgedTestResult(
                        testEndDay(clock),
                        virologyTestResult,
                        testKitType,
                        acknowledgedDate = LocalDateTime.ofInstant(acknowledgedDate, clock.zone).toLocalDate(),
                        requiresConfirmatoryTest = requiresConfirmatoryTest,
                        confirmedDate = null
                    )
            )
        }

    private fun TestInfo.confirm(confirmedDate: LocalDate): TestInfo =
        copy(acknowledgedTestResult = acknowledgedTestResult.confirm(confirmedDate))

    private fun AcknowledgedTestResult.confirm(confirmedDate: LocalDate): AcknowledgedTestResult =
        copy(confirmedDate = confirmedDate)

    private fun getConfirmedDateIfNeeded(
        receivedTestResult: ReceivedTestResult,
        previousTestResult: AcknowledgedTestResult?
    ): LocalDate? =
        if (!receivedTestResult.isConfirmed() &&
            previousTestResult != null &&
            previousTestResult.isConfirmed()
        )
            previousTestResult.confirmedDate ?: previousTestResult.testEndDate
        else null

    private fun createKeySharingInfoOnTestAcknowledged(
        testResult: ReceivedTestResult,
        testAcknowledgedDate: Instant,
        state: IsolationState
    ): KeySharingInfo? {
        val symptomsOnsetDate = state.assumedOnsetDateForExposureKeys
        if (testResult.isPositive() &&
            testResult.isConfirmed() &&
            testResult.diagnosisKeySubmissionSupported &&
            testResult.diagnosisKeySubmissionToken != null &&
            symptomsOnsetDate != null
        ) {
            val dateRange = calculateKeySubmissionDateRange(testAcknowledgedDate, symptomsOnsetDate)
            if (dateRange.containsAtLeastOneDay()) {
                return KeySharingInfo(
                    diagnosisKeySubmissionToken = testResult.diagnosisKeySubmissionToken,
                    acknowledgedDate = testAcknowledgedDate,
                    notificationSentDate = null,
                    testKitType = testResult.testKitType,
                    requiresConfirmatoryTest = testResult.requiresConfirmatoryTest
                )
            }
        }
        return null
    }

    private fun wouldTestIsolationEndBeforeOrOnStartOfExistingIsolation(
        receivedTestResult: ReceivedTestResult,
        currentState: IsolationLogicalState
    ): Boolean =
        when (currentState) {
            is NeverIsolating -> false
            is PossiblyIsolating -> {
                val isolationExpiryDate =
                    getIsolationExpiryDateBasedOnTest(receivedTestResult, currentState.isolationConfiguration)
                isolationExpiryDate.isBeforeOrEqual(currentState.startDate)
            }
        }

    private fun isTestOlderThanSelfAssessmentSymptoms(
        testResult: ReceivedTestResult,
        currentState: IsolationLogicalState
    ): Boolean {
        val indexCase = currentState.getIndexCase()
        return if (indexCase != null && indexCase.isolationTrigger is SelfAssessment) {
            testResult.testEndDay(clock).isBefore(indexCase.isolationTrigger.assumedOnsetDate)
        } else {
            false
        }
    }

    private fun IsolationLogicalState.getIndexCase(): IndexCase? =
        when (this) {
            is NeverIsolating -> null
            is PossiblyIsolating -> indexInfo as? IndexCase
        }

    private fun IsolationLogicalState.getTestResult(): AcknowledgedTestResult? =
        when (this) {
            is NeverIsolating -> negativeTest?.testResult
            is PossiblyIsolating -> indexInfo?.testResult
        }

    private fun ReceivedTestResult.isOlderThan(otherTest: AcknowledgedTestResult): Boolean =
        testEndDay(clock).isBefore(otherTest.testEndDate)

    private fun isNegativeTestIrrelevant(
        testResult: ReceivedTestResult,
        currentState: IsolationLogicalState
    ): Boolean =
        isTestBeforeExistingUnconfirmedPositiveTest(testResult, currentState) ||
            isTestOlderThanSelfAssessmentSymptoms(testResult, currentState)

    private fun isTestBeforeExistingUnconfirmedPositiveTest(
        testResult: ReceivedTestResult,
        currentState: IsolationLogicalState
    ): Boolean {
        val existingTestResult = currentState.getIndexCase()?.testResult
        return existingTestResult != null &&
            existingTestResult.isPositive() &&
            !existingTestResult.isConfirmed() &&
            testResult.testEndDay(clock).isBefore(existingTestResult.testEndDate)
    }

    private fun IsolationState.clearContactCase(): IsolationState =
        copy(contactCase = null)

    private fun IsolationState.expireIndexCaseWithNegativeTestResult(testResult: AcknowledgedTestResult): IsolationState {
        val newIndexInfo = when (indexInfo) {
            is IndexCase -> {
                indexInfo.copy(
                    testResult = testResult,
                    expiryDate = selectEarliest(indexInfo.expiryDate, testResult.testEndDate)
                )
            }
            else -> NegativeTest(testResult)
        }
        return copy(indexInfo = newIndexInfo)
    }

    private fun createIndexCaseWithPositiveTestResult(
        currentState: IsolationState,
        testInfo: TestInfo
    ): IndexCase {
        val isolationConfiguration = currentState.isolationConfiguration
        val expiryDateBasedOnTest =
            getIsolationExpiryDateBasedOnTest(testInfo.receivedTestResult, isolationConfiguration)
        val currentLogicalState = IsolationLogicalState.from(currentState)

        return with(
            IndexCase(
                isolationTrigger = IndexCaseIsolationTrigger.from(
                    testInfo.receivedTestResult,
                    triggerDate = LocalDate.now(clock),
                    clock
                )!!,
                testResult = testInfo.acknowledgedTestResult,
                expiryDate = expiryDateBasedOnTest
            )
        ) {
            copy(expiryDate = currentLogicalState.capExpiryDate(this))
        }
    }

    private fun updateIndexCaseWithPositiveTestResult(
        currentState: IsolationState,
        testInfoResult: TestInfo
    ): IsolationState =
        // If we already have an index case...
        if (currentState.indexInfo is IndexCase) {

            // If it was a self-assessment with a negative test result, the test result may have prematurely expired the
            // index case. Now we need to re-calculate the expiration date as it would have been if we had not received
            // the negative test result => simply create the self-assessment again and add the positive test result to it
            if (currentState.indexInfo.isolationTrigger is SelfAssessment &&
                currentState.indexInfo.testResult?.testResult == RelevantVirologyTestResult.NEGATIVE
            ) {
                currentState.copy(
                    // Re-create the self-assessment based on the original trigger
                    indexInfo = createSelfAssessmentIndexCase(
                        currentState = IsolationLogicalState.from(currentState),
                        selfAssessment = currentState.indexInfo.isolationTrigger
                    ).copy(
                        // Add the test result to it
                        testResult = testInfoResult.acknowledgedTestResult
                    )
                )

                // Otherwise simply add the test to it
            } else {
                currentState.copy(
                    indexInfo = currentState.indexInfo.copy(
                        testResult = testInfoResult.acknowledgedTestResult
                    )
                )
            }

            // Otherwise, replace with a new index case
        } else {
            replaceIndexCaseWithPositiveTestResult(currentState, testInfoResult)
        }

    private fun replaceIndexCaseWithPositiveTestResult(
        currentState: IsolationState,
        testInfoResult: TestInfo
    ): IsolationState =
        currentState.copy(indexInfo = createIndexCaseWithPositiveTestResult(currentState, testInfoResult))

    private fun confirmTestResult(
        currentState: IsolationState,
        confirmedDate: LocalDate
    ): IsolationState =
        if (currentState.indexInfo is IndexCase && currentState.indexInfo.testResult != null) {
            currentState.copy(
                indexInfo = currentState.indexInfo.copy(
                    testResult = currentState.indexInfo.testResult?.confirm(confirmedDate)
                )
            )
        } else {
            Timber.e("There is no test result to confirm")
            currentState
        }

    private fun getIsolationExpiryDateBasedOnTest(
        testResult: ReceivedTestResult,
        isolationConfiguration: DurationDays
    ): LocalDate {
        return if (testResult.symptomsOnsetDate?.explicitDate != null) {
            testResult.symptomsOnsetDate.explicitDate
                .plusDays(isolationConfiguration.indexCaseSinceSelfDiagnosisOnset.toLong())
        } else {
            testResult.testEndDay(clock)
                .plusDays(isolationConfiguration.indexCaseSinceTestResultEndDate.toLong())
        }
    }

    private data class TestInfo(
        val receivedTestResult: ReceivedTestResult,
        val acknowledgedTestResult: AcknowledgedTestResult
    )

    private sealed class IsolationUpdate(val preventKeySubmission: Boolean = false) {
        data class Overwrite(val testInfoResult: TestInfo) : IsolationUpdate()
        data class Update(val testInfoResult: TestInfo) : IsolationUpdate()
        data class Confirm(val confirmedDate: LocalDate) : IsolationUpdate()
        object Nothing : IsolationUpdate()
        object Ignore : IsolationUpdate(preventKeySubmission = true)
    }

    sealed class TransitionDueToTestResult {
        abstract val keySharingInfo: KeySharingInfo?

        data class DoNotTransition(
            val preventKeySubmission: Boolean,
            override val keySharingInfo: KeySharingInfo?
        ) : TransitionDueToTestResult()

        data class Transition(
            val newState: IsolationState,
            override val keySharingInfo: KeySharingInfo?
        ) : TransitionDueToTestResult()
    }
}
