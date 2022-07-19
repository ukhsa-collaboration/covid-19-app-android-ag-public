package uk.nhs.nhsx.covid19.android.app.state

import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.CalculateKeySubmissionDateRange
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfo
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.PLOD
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
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
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class TestResultIsolationHandler @Inject constructor(
    private val calculateKeySubmissionDateRange: CalculateKeySubmissionDateRange,
    private val wouldTestIsolationEndBeforeOrOnStartOfExistingIsolation: WouldTestIsolationEndBeforeOrOnStartOfExistingIsolation,
    private val createIsolationLogicalState: CreateIsolationLogicalState,
    private val clock: Clock
) {

    fun computeTransitionWithTestResultAcknowledgment(
        currentState: IsolationState,
        receivedTestResult: ReceivedTestResult,
        testAcknowledgedDate: Instant
    ): TransitionDueToTestResult {
        val isolationUpdate = computeIsolationUpdate(
            createIsolationLogicalState(currentState),
            receivedTestResult,
            testAcknowledgedDate
        )
        return applyIsolationUpdate(
            currentState,
            currentState.toIsolationInfo(),
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
                    wouldTestIsolationEndBeforeOrOnStartOfExistingIsolation(receivedTestResult, currentState) -> Ignore

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
                        when {
                            indexCase?.isSelfAssessment() == true -> {
                                if (indexCase.hasExpired(clock) && receivedTestResult.isConfirmed()) {
                                    return Nothing
                                }
                                getConfirmedDateIfNeeded(receivedTestResult, previousTestResult)?.let { confirmedDate ->
                                    Update(testInfo.confirm(confirmedDate))
                                } ?: Update(testInfo) }
                            indexCase?.testResult?.isPositive() == true -> {
                                if (indexCase.hasExpired(clock) && receivedTestResult.isConfirmed()) {
                                    return Nothing
                                }
                                getConfirmedDateIfNeeded(receivedTestResult, previousTestResult)?.let { confirmedDate ->
                                    Overwrite(testInfo.confirm(confirmedDate))
                                } ?: Overwrite(testInfo)
                            }
                            // This is not possible because it would mean index info is NegativeTest, so
                            // isOlderThanPositive() would return false
                            else -> Overwrite(testInfo)
                        }

                    // If the app knows about an existing negative test and “positive test end date” < “negative test end date”
                    receivedTestResult.isOlderThanNegative(previousTestResult) ->
                        if (receivedTestResult.isConfirmed()) {
                            when {
                                indexCase?.isSelfAssessment() == true -> Update(testInfo)
                                else -> Overwrite(testInfo)
                            }
                        } else if (isTestOlderThanSelfAssessmentSymptoms(
                                receivedTestResult, currentState, clock, defaultIfNoSymptoms = true
                            ) && previousTestResult != null &&
                            isNegativeConfirmedTestAfterPositiveUnconfirmedConfirmatoryDayLimit(
                                testInfo.acknowledgedTestResult, previousTestResult
                            )
                        ) {
                            Overwrite(testInfo.complete(completedDate = previousTestResult.testEndDate))
                        } else {
                            Ignore
                        }

                    receivedTestResult.requiresConfirmatoryTest ->
                        if (indexCase?.hasExpired(clock) == false) Nothing
                        else Overwrite(testInfo)

                    receivedTestResult.isPositiveConfirmedAndOlderThanIndexIsolationEndDate(currentState) -> {
                        when {
                            previousTestResult == null -> {
                                Update(testInfo)
                            }
                            !previousTestResult.isConfirmed() -> {
                                Confirm(receivedTestResult.testEndDate(clock))
                            }
                            else -> {
                                Nothing
                            }
                        }
                    }

                    else ->
                        if (!isOrWasIndexCase) Overwrite(testInfo)
                        else if (indexCase?.isSelfAssessment() == true && indexCase.hasExpired(clock))
                            Overwrite(testInfo)
                        else when (previousTestResult?.testResult) {
                            RelevantVirologyTestResult.NEGATIVE -> Overwrite(testInfo)
                            RelevantVirologyTestResult.POSITIVE ->
                                when {
                                    indexCase?.isSelfAssessment() == false && indexCase.hasExpired(clock) -> Overwrite(
                                        testInfo
                                    )
                                    previousTestResult.isConfirmed() -> Nothing
                                    else -> Confirm(receivedTestResult.testEndDate(clock))
                                }
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

            // The new test result is newer than the already stored result and symptoms, but is after isolation expiry
            // should start new isolation
            testInfo.receivedTestResult.isConfirmed() -> {
                currentState.getIndexCase()?.let { indexCase ->
                    if (!currentState.isActiveIsolation(clock) && !testInfo.receivedTestResult.testEndDate(clock)
                            .isBefore(indexCase.expiryDate)
                    ) {
                        Overwrite(testInfo)
                    } else {
                        Update(testInfo)
                    }
                } ?: Update(testInfo)
            }

            currentState.isActiveIndexCase(clock) -> Nothing

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
        isolationState: IsolationState,
        currentInfo: IsolationInfo,
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
        var newInfo =
            if (testResult.isConfirmed() && testResult.isPositive() &&
                !testResult.isPositiveConfirmedAndOlderThanIndexIsolationEndDate(
                    createIsolationLogicalState(isolationState)
                )
            ) currentInfo.clearContactCase()
            else currentInfo

        newInfo = when (testResult.testResult) {
            POSITIVE ->
                when (isolationUpdate) {
                    is Update -> updateIndexCaseWithPositiveTestResult(newInfo, isolationUpdate.testInfoResult)
                    is Overwrite -> replaceIndexCaseWithPositiveTestResult(newInfo, isolationUpdate.testInfoResult)
                    is Confirm -> confirmTestResult(newInfo, isolationUpdate.confirmedDate)
                    is Nothing -> currentInfo
                    else -> newInfo
                }
            NEGATIVE ->
                when (isolationUpdate) {
                    is Update -> newInfo.copy(
                        testResult = isolationUpdate.testInfoResult.acknowledgedTestResult
                    )
                    is Overwrite -> newInfo.copy(
                        selfAssessment = null,
                        testResult = isolationUpdate.testInfoResult.acknowledgedTestResult
                    )
                    is Confirm -> {
                        Timber.e("Cannot confirm using a negative test result")
                        newInfo
                    }
                    is Complete -> completeTestResult(newInfo, isolationUpdate.completedDate)
                    is CompleteAndDeleteSymptoms -> completeTestResultAndDeleteSymptoms(
                        newInfo,
                        isolationUpdate.completedDate
                    )
                    DeleteSymptoms -> deleteSymptoms(newInfo)
                    DeleteTest -> deleteTest(newInfo)
                    else -> newInfo
                }
            VOID, PLOD -> newInfo
        }

        val keySharingInfo = createKeySharingInfoOnTestAcknowledged(
            testResult,
            testAcknowledgedDate,
            newInfo.assumedOnsetDateForExposureKeys
        )

        return if (newInfo == currentInfo) DoNotTransition(preventKeySubmission = false, keySharingInfo)
        else Transition(newInfo, keySharingInfo)
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
                    confirmatoryDayLimit = confirmatoryDayLimit,
                    shouldOfferFollowUpTest = shouldOfferFollowUpTest
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
        assumedOnsetDate: LocalDate?
    ): KeySharingInfo? {
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

    private fun ReceivedTestResult.isPositiveConfirmedAndOlderThanIndexIsolationEndDate(currentIsolationState: IsolationLogicalState): Boolean {
        val indexCase = currentIsolationState.getIndexCase()

        return if (indexCase == null || !isConfirmed() || !indexCase.hasExpired(clock) || indexCase.testResult?.isPositive() == false) {
            false
        } else {
            testEndDate(clock).isBefore(indexCase.expiryDate)
        }
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

    private fun IsolationInfo.clearContactCase(): IsolationInfo =
        copy(contact = null)

    private fun updateIndexCaseWithPositiveTestResult(
        isolationInfo: IsolationInfo,
        testInfoResult: TestInfo
    ): IsolationInfo =
        if (isolationInfo.selfAssessment != null) {
            isolationInfo.copy(testResult = testInfoResult.acknowledgedTestResult)
        } else {
            replaceIndexCaseWithPositiveTestResult(isolationInfo, testInfoResult)
        }

    private fun replaceIndexCaseWithPositiveTestResult(
        isolationInfo: IsolationInfo,
        testInfoResult: TestInfo
    ): IsolationInfo =
        isolationInfo.copy(
            testResult = testInfoResult.acknowledgedTestResult,
            selfAssessment = createSelfAssessmentFromTestResult(
                testInfoResult.receivedTestResult,
                testInfoResult.acknowledgedTestResult.acknowledgedDate
            )
        )

    private fun confirmTestResult(
        isolationInfo: IsolationInfo,
        confirmedDate: LocalDate
    ): IsolationInfo =
        if (isolationInfo.testResult != null && isolationInfo.testResult.isPositive()) {
            isolationInfo.copy(
                testResult = isolationInfo.testResult.confirm(confirmedDate),
            )
        } else {
            Timber.e("There is no positive test result to confirm. Test result: ${isolationInfo.testResult}")
            isolationInfo
        }

    private fun completeTestResult(
        isolationInfo: IsolationInfo,
        completedDate: LocalDate
    ): IsolationInfo =
        if (isolationInfo.testResult != null && isolationInfo.testResult.isPositive()) {
            isolationInfo.copy(
                testResult = isolationInfo.testResult.complete(completedDate),
            )
        } else {
            Timber.e("There is no positive test result to confirm. Test result: ${isolationInfo.testResult}")
            isolationInfo
        }

    private fun completeTestResultAndDeleteSymptoms(
        isolationInfo: IsolationInfo,
        completedDate: LocalDate
    ): IsolationInfo =
        completeTestResult(isolationInfo, completedDate)
            .copy(selfAssessment = null)

    private fun deleteSymptoms(isolationInfo: IsolationInfo): IsolationInfo =
        isolationInfo.copy(selfAssessment = null)

    private fun deleteTest(isolationInfo: IsolationInfo): IsolationInfo =
        isolationInfo.copy(testResult = null)

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
            val newIsolationInfo: IsolationInfo,
            override val keySharingInfo: KeySharingInfo?
        ) : TransitionDueToTestResult()
    }
}
