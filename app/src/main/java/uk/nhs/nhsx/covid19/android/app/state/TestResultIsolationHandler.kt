package uk.nhs.nhsx.covid19.android.app.state

import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.CalculateKeySubmissionDateRange
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfo
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.PLOD
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.PositiveTestResult
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.NegativeTest
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.IsolationUpdate.Complete
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.IsolationUpdate.CompleteAndDeleteSymptoms
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.IsolationUpdate.Confirm
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.IsolationUpdate.DeleteSymptoms
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.IsolationUpdate.DeleteTest
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.IsolationUpdate.Ignore
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.IsolationUpdate.Nothing
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.IsolationUpdate.Overwrite
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.IsolationUpdate.Update
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.DoNotTransition
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.Transition
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ConfirmatoryTestCompletionStatus.COMPLETED
import uk.nhs.nhsx.covid19.android.app.testordering.ConfirmatoryTestCompletionStatus.COMPLETED_AND_CONFIRMED
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.toRelevantVirologyTestResult
import uk.nhs.nhsx.covid19.android.app.util.selectEarliest
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
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
        return applyIsolationUpdate(
            currentState.toIsolationState(),
            receivedTestResult,
            isolationUpdate,
            testAcknowledgedDate
        )
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
            VOID, PLOD -> Nothing

            NEGATIVE ->
                when {
                    !isOrWasIndexCase -> Overwrite(testInfo)

                    hasBecomeSymptomaticAfterPositiveTest(currentState) ->
                        computeIsolationUpdateWhenSymptomaticAfterPositiveThenNegative(currentState, testInfo)

                    isNegativeTestIrrelevant(receivedTestResult, currentState) -> Nothing

                    isConfirmedTestAfterConfirmatoryDayLimit(testInfo.acknowledgedTestResult, currentState) ->
                        Complete(receivedTestResult.testEndDate(clock))

                    else -> when (previousTestResult?.testResult) {
                        RelevantVirologyTestResult.NEGATIVE -> Nothing
                        RelevantVirologyTestResult.POSITIVE ->
                            if (previousTestResult.isConfirmed()) Nothing
                            else Update(testInfo)
                        else -> Update(testInfo)
                    }
                }

            POSITIVE ->
                when {
                    // If the app knows about an existing (active or expired) isolation, and “positive test isolation end
                    // date” <= “assumed isolation start date”, then test is considered “too old”
                    wouldTestIsolationEndBeforeOrOnStartOfExistingIsolation(receivedTestResult, currentState, clock) -> Ignore

                    hasBecomeSymptomaticAfterPositiveTest(currentState) ->
                        computeIsolationUpdateWhenSymptomaticAfterPositiveThenPositive(currentState, testInfo)

                    // If the app knows about symptoms, and “positive test end date” < “assumed symptom onset date”, then
                    // save the test and preserve the symptoms.
                    // Remarks:
                    //  * Here we want to skip the case where we already have a negative test result stored and the positive
                    //    test result we have just received is unconfirmed and older than the negative, hence the check for
                    //    !receivedTestResult.isPositiveUnconfirmedOlderThanNegative(previousTestResult)
                    //    That case is handled further below.
                    //  * If there is a positive test stored, we know that the stored test is older than the symptoms,
                    //    otherwise we would have gone into the branch hasBecomeSymptomaticAfterPositiveTest
                    !receivedTestResult.isPositiveUnconfirmedOlderThanNegative(previousTestResult) &&
                        isTestOlderThanSelfAssessmentSymptoms(receivedTestResult, currentState, clock) ->
                        getConfirmedDateIfNeeded(receivedTestResult, previousTestResult)?.let { confirmedDate ->
                            Update(testInfo.confirm(confirmedDate))
                        } ?: Update(testInfo)

                    // If the app knows about an existing (confirmed or unconfirmed) positive test, and “positive test end
                    // date” < “existing positive test end date”, then the newly entered positive test should be used for
                    // isolation
                    receivedTestResult.isOlderThanPositive(previousTestResult) ->
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
                    receivedTestResult.isOlderThanNegative(previousTestResult) ->
                        if (receivedTestResult.isConfirmed()) {
                            when (indexCase?.isolationTrigger) {
                                is SelfAssessment -> Update(testInfo)
                                else -> Overwrite(testInfo)
                            }
                        } else if (isTestOlderThanSelfAssessmentSymptoms(receivedTestResult, currentState, clock, defaultIfNoSymptoms = true) &&
                            previousTestResult != null &&
                            isNegativeConfirmedTestAfterPositiveUnconfirmedConfirmatoryDayLimit(testInfo.acknowledgedTestResult, previousTestResult)
                        ) {
                            Overwrite(testInfo.complete(completedDate = previousTestResult.testEndDate))
                        } else {
                            Ignore
                        }

                    receivedTestResult.requiresConfirmatoryTest ->
                        if (indexCase?.hasExpired(clock) == false) Nothing
                        else Overwrite(testInfo)

                    else ->
                        if (!isOrWasIndexCase) Overwrite(testInfo)
                        else when (previousTestResult?.testResult) {
                            RelevantVirologyTestResult.NEGATIVE -> Overwrite(testInfo)
                            RelevantVirologyTestResult.POSITIVE ->
                                if (previousTestResult.isConfirmed()) Nothing
                                else Confirm(receivedTestResult.testEndDate(clock))
                            else -> Update(testInfo)
                        }
                }
        }
    }

    /**
     * Compute isolation update when the user became symptomatic after having a positive test result, and then receives
     * a positive test result.
     *
     * Preconditions for calling this function:
     * <ul>
     *     <li>Isolation state contains a positive test result</li>
     *     <li>Isolation state contains symptoms (self-assessment)</li>
     *     <li>The positive test result's end date ({@link AcknowledgedTestResult#testEndDate}) is before the symptoms
     *     onset date ({@link SelfAssessment#assumedOnsetDate})</li>
     *     <li>[testInfo] is positive</li>
     * </ul>
     */
    private fun computeIsolationUpdateWhenSymptomaticAfterPositiveThenPositive(
        currentState: IsolationLogicalState,
        testInfo: TestInfo
    ): IsolationUpdate {
        val previousTestResult = currentState.getTestResult()

        return when {
            // If the app knows about an existing (confirmed or unconfirmed) positive test, and “positive test end
            // date” < “existing positive test end date”, then the newly entered positive test should be used for
            // isolation, but preserve the symptoms
            previousTestResult != null &&
                previousTestResult.isPositive() &&
                testInfo.receivedTestResult.isOlderThan(previousTestResult, clock) ->
                getConfirmedDateIfNeeded(testInfo.receivedTestResult, previousTestResult)?.let { confirmedDate ->
                    Update(testInfo.confirm(confirmedDate))
                } ?: Update(testInfo)

            // The new test result is newer than the already stored result but older than symptoms
            isTestOlderThanSelfAssessmentSymptoms(testInfo.receivedTestResult, currentState, clock) ->
                if (testInfo.receivedTestResult.isConfirmed() && previousTestResult?.isConfirmed() == false)
                    Confirm(confirmedDate = testInfo.receivedTestResult.testEndDate(clock))
                else Nothing

            testInfo.receivedTestResult.isConfirmed() -> Update(testInfo)

            currentState is PossiblyIsolating && currentState.isActiveIndexCase(clock) -> Nothing

            else -> Overwrite(testInfo)
        }
    }

    /**
     * Compute isolation update when the user became symptomatic after having a positive test result, and then receives
     * a negative test result.
     *
     * Preconditions for calling this function:
     * <ul>
     *     <li>Isolation state contains a positive test result</li>
     *     <li>Isolation state contains symptoms (self-assessment)</li>
     *     <li>The positive test result's end date ({@link AcknowledgedTestResult#testEndDate}) is before the symptoms
     *     onset date ({@link SelfAssessment#assumedOnsetDate})</li>
     *     <li>[testInfo] is negative</li>
     * </ul>
     */
    private fun computeIsolationUpdateWhenSymptomaticAfterPositiveThenNegative(
        currentState: IsolationLogicalState,
        testInfo: TestInfo
    ): IsolationUpdate {
        val previousTestResult = currentState.getTestResult()

        return if (previousTestResult?.isConfirmed() == true) {
            if (isTestOlderThanSelfAssessmentSymptoms(testInfo.receivedTestResult, currentState, clock)) Nothing
            else DeleteSymptoms
        } else {
            when {
                isTestBeforeExistingUnconfirmedPositiveTest(testInfo.receivedTestResult, currentState) -> Nothing

                isConfirmedTestAfterConfirmatoryDayLimit(testInfo.acknowledgedTestResult, currentState) -> {
                    if (isTestOlderThanSelfAssessmentSymptoms(testInfo.receivedTestResult, currentState, clock))
                        Complete(testInfo.receivedTestResult.testEndDate(clock))
                    else CompleteAndDeleteSymptoms(testInfo.receivedTestResult.testEndDate(clock))
                }

                isTestOlderThanSelfAssessmentSymptoms(testInfo.receivedTestResult, currentState, clock) -> DeleteTest

                else -> Update(testInfo)
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
            return DoNotTransition(
                preventKeySubmission = true,
                keySharingInfo = null
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
                    is Complete -> completeTestResult(newState, isolationUpdate.completedDate)
                    is CompleteAndDeleteSymptoms -> completeTestResultAndDeleteSymptoms(newState, isolationUpdate.completedDate)
                    DeleteSymptoms -> deleteSymptoms(newState)
                    DeleteTest -> deleteTest(newState)
                    else -> newState
                }
            VOID, PLOD -> newState
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
                        testEndDate(clock),
                        virologyTestResult,
                        testKitType,
                        acknowledgedDate = acknowledgedDate.toLocalDate(clock.zone),
                        requiresConfirmatoryTest = requiresConfirmatoryTest,
                        confirmedDate = null,
                        confirmatoryDayLimit = confirmatoryDayLimit
                    )
            )
        }

    private fun TestInfo.confirm(confirmedDate: LocalDate): TestInfo =
        copy(acknowledgedTestResult = acknowledgedTestResult.confirm(confirmedDate))

    private fun AcknowledgedTestResult.confirm(confirmedDate: LocalDate): AcknowledgedTestResult =
        copy(confirmedDate = confirmedDate, confirmatoryTestCompletionStatus = COMPLETED_AND_CONFIRMED)

    private fun TestInfo.complete(completedDate: LocalDate): TestInfo =
        copy(acknowledgedTestResult = acknowledgedTestResult.complete(completedDate))

    private fun AcknowledgedTestResult.complete(completedDate: LocalDate): AcknowledgedTestResult =
        copy(confirmedDate = completedDate, confirmatoryTestCompletionStatus = COMPLETED)

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
        val assumedOnsetDate = state.assumedOnsetDateForExposureKeys
        if (testResult.isPositive() &&
            testResult.diagnosisKeySubmissionSupported &&
            testResult.diagnosisKeySubmissionToken != null &&
            assumedOnsetDate != null
        ) {
            val dateRange = calculateKeySubmissionDateRange(testAcknowledgedDate, assumedOnsetDate)
            if (dateRange.containsAtLeastOneDay()) {
                return KeySharingInfo(
                    diagnosisKeySubmissionToken = testResult.diagnosisKeySubmissionToken,
                    acknowledgedDate = testAcknowledgedDate,
                    notificationSentDate = null
                )
            }
        }
        return null
    }

    private fun hasBecomeSymptomaticAfterPositiveTest(currentState: IsolationLogicalState): Boolean {
        val indexCase = currentState.getIndexCase()
        val selfAssessmentOnsetDate = indexCase?.getSelfAssessmentOnsetDate()
        val previousTestResult = indexCase?.testResult
        return selfAssessmentOnsetDate != null &&
            previousTestResult != null &&
            previousTestResult.isPositive() &&
            selfAssessmentOnsetDate.isAfter(previousTestResult.testEndDate)
    }

    private fun ReceivedTestResult.isOlderThanPositive(otherTest: AcknowledgedTestResult?): Boolean =
        otherTest != null &&
            otherTest.testResult == RelevantVirologyTestResult.POSITIVE &&
            this.isOlderThan(otherTest, clock)

    private fun ReceivedTestResult.isOlderThanNegative(otherTest: AcknowledgedTestResult?): Boolean =
        otherTest != null &&
            otherTest.testResult == RelevantVirologyTestResult.NEGATIVE &&
            this.isOlderThan(otherTest, clock)

    private fun ReceivedTestResult.isPositiveUnconfirmedOlderThanNegative(otherTest: AcknowledgedTestResult?): Boolean =
        isPositive() &&
            !isConfirmed() &&
            this.isOlderThanNegative(otherTest)

    private fun isNegativeTestIrrelevant(
        testResult: ReceivedTestResult,
        currentState: IsolationLogicalState
    ): Boolean =
        isTestBeforeExistingUnconfirmedPositiveTest(testResult, currentState) ||
            isTestOlderThanSelfAssessmentSymptoms(testResult, currentState, clock)

    private fun isTestBeforeExistingUnconfirmedPositiveTest(
        testResult: ReceivedTestResult,
        currentState: IsolationLogicalState
    ): Boolean {
        val existingTestResult = currentState.getIndexCase()?.testResult
        return existingTestResult != null &&
            existingTestResult.isPositive() &&
            !existingTestResult.isConfirmed() &&
            testResult.testEndDate(clock).isBefore(existingTestResult.testEndDate)
    }

    private fun isConfirmedTestAfterConfirmatoryDayLimit(
        testResult: AcknowledgedTestResult,
        currentState: IsolationLogicalState
    ): Boolean {
        val existingTestResult = currentState.getIndexCase()?.testResult
        return existingTestResult != null &&
            isNegativeConfirmedTestAfterPositiveUnconfirmedConfirmatoryDayLimit(existingTestResult, testResult)
    }

    private fun isNegativeConfirmedTestAfterPositiveUnconfirmedConfirmatoryDayLimit(
        testResult1: AcknowledgedTestResult,
        testResult2: AcknowledgedTestResult
    ): Boolean {
        val oldTest: AcknowledgedTestResult
        val newTest: AcknowledgedTestResult
        if (testResult1.isOlderThan(testResult2, clock)) {
            oldTest = testResult1
            newTest = testResult2
        } else {
            oldTest = testResult2
            newTest = testResult1
        }

        return oldTest.isPositive() &&
            !oldTest.isConfirmed() &&
            oldTest.confirmatoryDayLimit != null &&
            newTest.testResult == RelevantVirologyTestResult.NEGATIVE &&
            newTest.isConfirmed() &&
            !oldTest.isDateWithinConfirmatoryDayLimit(newTest.testEndDate, clock)
    }

    private fun IsolationState.clearContactCase(): IsolationState =
        copy(contactCase = null)

    private fun IsolationState.expireIndexCaseWithNegativeTestResult(testResult: AcknowledgedTestResult): IsolationState {
        val newIndexInfo = when {
            indexInfo is IndexCase && indexInfo.isolationTrigger is SelfAssessment -> {
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
        testResult: AcknowledgedTestResult
    ): IndexCase {
        val isolationConfiguration = currentState.isolationConfiguration
        val expiryDateBasedOnTest =
            getIsolationExpiryDateBasedOnTestEndDate(testResult.testEndDate, isolationConfiguration)
        val currentLogicalState = IsolationLogicalState.from(currentState)

        return IndexCase(
            isolationTrigger = PositiveTestResult(testResult.testEndDate),
            testResult = testResult,
            expiryDate = expiryDateBasedOnTest
        ).capExpiryDate(currentLogicalState)
    }

    private fun createIndexCaseWithPositiveTestResult(
        currentState: IsolationState,
        testInfo: TestInfo
    ): IndexCase {
        val isolationConfiguration = currentState.isolationConfiguration
        val expiryDateBasedOnTest =
            getIsolationExpiryDateBasedOnTest(testInfo.receivedTestResult, isolationConfiguration, clock)
        val currentLogicalState = IsolationLogicalState.from(currentState)

        return IndexCase(
            isolationTrigger = IndexCaseIsolationTrigger.from(
                testInfo.receivedTestResult,
                triggerDate = LocalDate.now(clock),
                clock
            )!!,
            testResult = testInfo.acknowledgedTestResult,
            expiryDate = expiryDateBasedOnTest
        ).capExpiryDate(currentLogicalState)
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
                        selfAssessment = currentState.indexInfo.isolationTrigger,
                        discardTestResultIfPresent = true
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

    private fun completeTestResult(
        currentState: IsolationState,
        completedDate: LocalDate
    ): IsolationState {
        val indexInfo = currentState.indexInfo
        return if (indexInfo is IndexCase) {
            currentState.copy(
                indexInfo = indexInfo.copy(
                    testResult = indexInfo.testResult?.complete(completedDate)
                )
            )
        } else {
            currentState
        }
    }

    private fun completeTestResultAndDeleteSymptoms(
        currentState: IsolationState,
        completedDate: LocalDate
    ): IsolationState =
        (currentState.indexInfo as? IndexCase)?.testResult?.let { previousTestResult ->
            currentState.copy(
                indexInfo = createIndexCaseWithPositiveTestResult(
                    currentState,
                    testResult = previousTestResult.complete(completedDate)
                )
            )
        } ?: currentState

    private fun deleteSymptoms(currentState: IsolationState): IsolationState {
        val previousTestResult = (currentState.indexInfo as? IndexCase)?.testResult
        return when (previousTestResult?.testResult) {
            RelevantVirologyTestResult.POSITIVE ->
                currentState.copy(
                    indexInfo = createIndexCaseWithPositiveTestResult(
                        currentState,
                        testResult = previousTestResult
                    )
                )
            RelevantVirologyTestResult.NEGATIVE ->
                currentState.copy(
                    indexInfo = NegativeTest(previousTestResult)
                )
            else -> currentState.copy(indexInfo = null)
        }
    }

    private fun deleteTest(currentState: IsolationState): IsolationState =
        when (currentState.indexInfo) {
            is NegativeTest -> currentState.copy(indexInfo = null)
            is IndexCase -> {
                when (currentState.indexInfo.isolationTrigger) {
                    is SelfAssessment -> {
                        currentState.copy(
                            // Re-create the self-assessment based on the original trigger
                            indexInfo = createSelfAssessmentIndexCase(
                                currentState = IsolationLogicalState.from(currentState),
                                selfAssessment = currentState.indexInfo.isolationTrigger,
                                discardTestResultIfPresent = true
                            )
                        )
                    }
                    is PositiveTestResult -> currentState.copy(indexInfo = null)
                }
            }
            else -> currentState
        }

    private fun IndexCase.capExpiryDate(logicalState: IsolationLogicalState): IndexCase =
        copy(expiryDate = logicalState.capExpiryDate(this))

    private data class TestInfo(
        val receivedTestResult: ReceivedTestResult,
        val acknowledgedTestResult: AcknowledgedTestResult
    )

    private sealed class IsolationUpdate {
        data class Overwrite(val testInfoResult: TestInfo) : IsolationUpdate()
        data class Update(val testInfoResult: TestInfo) : IsolationUpdate()
        data class Confirm(val confirmedDate: LocalDate) : IsolationUpdate()
        data class Complete(val completedDate: LocalDate) : IsolationUpdate()
        data class CompleteAndDeleteSymptoms(val completedDate: LocalDate) : IsolationUpdate()
        object DeleteSymptoms : IsolationUpdate()
        object DeleteTest : IsolationUpdate()
        object Nothing : IsolationUpdate()
        object Ignore : IsolationUpdate()
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
