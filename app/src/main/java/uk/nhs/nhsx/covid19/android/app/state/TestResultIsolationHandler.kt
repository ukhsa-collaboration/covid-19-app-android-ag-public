package uk.nhs.nhsx.covid19.android.app.state

import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.IsolationUpdate.Confirm
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.IsolationUpdate.Ignore
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.IsolationUpdate.Nothing
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.IsolationUpdate.Overwrite
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.IsolationUpdate.OverwriteAndConfirm
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.IsolationUpdate.Update
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.IsolationUpdate.UpdateAndConfirm
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.DoNotTransitionButStoreTestResult
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.TransitionAndStoreTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantTestResultProvider
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation
import uk.nhs.nhsx.covid19.android.app.util.selectEarliest
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

class TestResultIsolationHandler @Inject constructor(
    val relevantTestResultProvider: RelevantTestResultProvider,
    val isolationConfigurationProvider: IsolationConfigurationProvider,
    val clock: Clock
) {

    private val indexCaseOnsetDateBeforeTestResultDate: Long = 3

    fun computeTransitionWithTestResult(currentState: State, testResult: ReceivedTestResult): TransitionDueToTestResult {
        val isolationUpdate = computeIsolationUpdate(currentState, testResult)
        return applyIsolationUpdate(currentState, testResult, isolationUpdate)
    }

    fun symptomsOnsetDateFromTestResult(testResult: ReceivedTestResult): LocalDate =
        if (testResult.symptomsOnsetDate?.explicitDate != null) {
            testResult.symptomsOnsetDate.explicitDate
        } else {
            val testResultDate = LocalDateTime.ofInstant(testResult.testEndDate, clock.zone).toLocalDate()
            testResultDate.minusDays(indexCaseOnsetDateBeforeTestResultDate)
        }

    private fun computeIsolationUpdate(
        currentState: State,
        receivedTestResult: ReceivedTestResult
    ): IsolationUpdate {
        val previousTestResult = relevantTestResultProvider.testResult
        val isOrWasIndexCase = (currentState is Isolation && currentState.isIndexCase()) ||
            (currentState is Default && currentState.previousIsolation?.isIndexCase() ?: false)

        return when (receivedTestResult.testResult) {
            VOID -> Nothing

            NEGATIVE ->
                if (isNegativeTestIrrelevant(receivedTestResult, currentState)) Nothing
                else if (!isOrWasIndexCase) Overwrite
                else when (previousTestResult?.testResult) {
                    RelevantVirologyTestResult.NEGATIVE -> Nothing
                    RelevantVirologyTestResult.POSITIVE ->
                        if (previousTestResult.isConfirmed()) Nothing
                        else Overwrite
                    else -> Update
                }

            POSITIVE ->
                when {
                    // If the app knows about an existing (active or expired) isolation, and “positive test isolation end
                    // date” <= “assumed isolation start date”, then test is considered “too old”
                    wouldTestIsolationEndBeforeStartOfExistingIsolation(receivedTestResult, currentState) -> Ignore

                    // If the app knows about symptoms, and “positive test end date” < “assumed symptom onset date”, then
                    // the positive test should be used for isolation
                    isTestOlderThanSelfAssessmentSymptoms(receivedTestResult, currentState) ->
                        getConfirmedDateIfNeeded(receivedTestResult, previousTestResult)?.let { confirmedDate ->
                            OverwriteAndConfirm(confirmedDate)
                        } ?: Overwrite

                    // If the app knows about an existing (confirmed or unconfirmed) positive test, and “positive test end
                    // date” < “existing positive test end date”, then the newly entered positive test should be used for
                    // isolation
                    previousTestResult != null &&
                        previousTestResult.isPositive() &&
                        receivedTestResult.isOlderThan(previousTestResult) ->
                        // TODO allo in iOS here they return UpdateXY if the trigger is self-assessment, and OverwriteXY if it's test. Not necessary for us atm
                        getConfirmedDateIfNeeded(receivedTestResult, previousTestResult)?.let { confirmedDate ->
                            UpdateAndConfirm(confirmedDate)
                        } ?: Update

                    // If the app knows about an existing negative test and “positive test end date” < “negative test end date”
                    previousTestResult != null &&
                        previousTestResult.testResult == RelevantVirologyTestResult.NEGATIVE &&
                        receivedTestResult.isOlderThan(previousTestResult) ->
                        if (receivedTestResult.isConfirmed()) Update
                        else Ignore

                    receivedTestResult.requiresConfirmatoryTest ->
                        if (currentState is Isolation && currentState.isIndexCase()) Nothing
                        else Overwrite

                    else ->
                        if (!isOrWasIndexCase) Overwrite
                        else when (previousTestResult?.testResult) {
                            RelevantVirologyTestResult.NEGATIVE -> Overwrite
                            RelevantVirologyTestResult.POSITIVE ->
                                if (previousTestResult.isConfirmed()) Nothing
                                else Confirm(receivedTestResult.testEndDate)
                            else -> Update
                        }
                }
        }
    }

    private fun applyIsolationUpdate(
        currentState: State,
        testResult: ReceivedTestResult,
        isolationUpdate: IsolationUpdate
    ): TransitionDueToTestResult {
        if (!isolationUpdate.requiresStateUpdate &&
            isolationUpdate.testStorageOperation == TestResultStorageOperation.Ignore
        ) {
            return TransitionDueToTestResult.Ignore(preventKeySubmission = isolationUpdate.preventKeySubmission)
        }

        // A confirmed positive test result (expired or not) removes any memory of a contact case (current or past)
        var newState =
            if (testResult.isConfirmed() && testResult.isPositive() && isolationUpdate != Ignore) currentState.clearContactCase()
            else currentState

        newState = when (testResult.testResult) {
            POSITIVE ->
                when (isolationUpdate) {
                    Update, is UpdateAndConfirm -> updateIndexCaseWithTest(newState, testResult)
                    Overwrite, is OverwriteAndConfirm -> replaceIndexCase(newState, testResult)
                    else -> newState
                }
            NEGATIVE ->
                // TODO allo: we will distinguish between overwrite and update once we change the storage for index case
                if (isolationUpdate.requiresStateUpdate) newState.expireIndexCase(testEndDay(testResult))
                else newState
            VOID -> newState
        }

        return if (newState == currentState) DoNotTransitionButStoreTestResult(isolationUpdate.testStorageOperation)
        else TransitionAndStoreTestResult(newState, isolationUpdate.testStorageOperation)
    }

    private fun getConfirmedDateIfNeeded(
        receivedTestResult: ReceivedTestResult,
        previousTestResult: AcknowledgedTestResult?
    ): Instant? =
        if (!receivedTestResult.isConfirmed() &&
            previousTestResult != null &&
            previousTestResult.isConfirmed()
        )
            previousTestResult.confirmedDate ?: previousTestResult.testEndDate
        else null

    private fun wouldTestIsolationEndBeforeStartOfExistingIsolation(
        receivedTestResult: ReceivedTestResult,
        currentState: State
    ): Boolean {
        val isolation = when (currentState) {
            is Default -> currentState.previousIsolation
            is Isolation -> currentState
        }
        val previousPositiveTestEndDate = relevantTestResultProvider.getTestResultIfPositive()?.testEndDate?.let {
            LocalDateTime.ofInstant(it, clock.zone).toLocalDate()
        }
        val previousSymptomsOnsetDate = isolation?.indexCase?.let { indexCase ->
            if (indexCase.selfAssessment) indexCase.symptomsOnsetDate
            else null
        }
        val previousContactStartDate = isolation?.contactCase?.startDate?.let {
            LocalDateTime.ofInstant(it, clock.zone).toLocalDate()
        }

        val startOfPreviousIsolation = selectEarliest(
            previousPositiveTestEndDate,
            previousSymptomsOnsetDate,
            previousContactStartDate
        )

        return if (startOfPreviousIsolation != null) {
            val isolationExpiryDate = getIsolationExpiryDateBasedOnTest(receivedTestResult)
            isolationExpiryDate.isBefore(startOfPreviousIsolation)
        } else false
    }

    private fun isTestOlderThanSelfAssessmentSymptoms(
        testResult: ReceivedTestResult,
        currentState: State
    ): Boolean {
        val indexCase = currentState.getIndexCase()
        return if (indexCase != null && indexCase.selfAssessment) testEndDay(testResult).isBefore(indexCase.symptomsOnsetDate)
        else false
    }

    private fun State.getIndexCase(): IndexCase? =
        when (this) {
            is Default -> previousIsolation?.indexCase
            is Isolation -> indexCase
        }

    private fun ReceivedTestResult.isOlderThan(otherTest: AcknowledgedTestResult): Boolean =
        testEndDate.isBefore(otherTest.testEndDate)

    private fun isNegativeTestIrrelevant(
        testResult: ReceivedTestResult,
        currentState: State
    ): Boolean =
        isTestBeforeExistingUnconfirmedPositiveTest(testResult) ||
            isTestOlderThanSelfAssessmentSymptoms(testResult, currentState)

    private fun isTestBeforeExistingUnconfirmedPositiveTest(testResult: ReceivedTestResult): Boolean {
        val existingTestResult = relevantTestResultProvider.testResult
        return existingTestResult != null &&
            existingTestResult.isPositive() &&
            !existingTestResult.isConfirmed() &&
            testResult.testEndDate.isBefore(existingTestResult.testEndDate)
    }

    private fun State.clearContactCase(): State =
        when (this) {
            is Default -> clearPreviousContactCase()
            is Isolation -> clearContactCase()
        }

    private fun Default.clearPreviousContactCase(): Default =
        this.previousIsolation?.let { previousIsolation ->
            if (previousIsolation.isContactCaseOnly()) Default()
            else this.copy(previousIsolation = previousIsolation.copy(contactCase = null))
        } ?: this

    private fun Isolation.clearContactCase(): State =
        if (this.isContactCaseOnly()) Default()
        else this.copy(contactCase = null)

    private fun State.expireIndexCase(expiryDate: LocalDate): State =
        when (this) {
            is Default -> expirePreviousIndexCase(expiryDate)
            is Isolation -> expireIndexCase(expiryDate)
        }

    private fun Default.expirePreviousIndexCase(expiryDate: LocalDate): Default =
        copy(
            previousIsolation = this.previousIsolation?.let {
                it.copy(indexCase = it.indexCase?.expire(expiryDate))
            }
        )

    private fun Isolation.expireIndexCase(expiryDate: LocalDate): State =
        when {
            isIndexCaseOnly() -> Default(
                previousIsolation = copy(
                    indexCase = indexCase?.expire(expiryDate)
                )
            )
            isBothCases() -> copy(indexCase = null)
            else -> this
        }

    private fun IndexCase.expire(newExpiryDate: LocalDate): IndexCase =
        copy(expiryDate = selectEarliest(newExpiryDate, expiryDate))

    private fun createIndexCase(
        currentState: State,
        testResult: ReceivedTestResult
    ): Isolation {
        val durationDays = isolationConfigurationProvider.durationDays
        val expiryDateBasedOnTest = getIsolationExpiryDateBasedOnTest(testResult, durationDays)

        val expiryDate =
            if (currentState is Isolation) currentState.capExpiryDate(expiryDateBasedOnTest)
            else expiryDateBasedOnTest

        return Isolation(
            isolationStart = testResult.testEndDate,
            isolationConfiguration = durationDays,
            indexCase = IndexCase(
                symptomsOnsetDate = symptomsOnsetDateFromTestResult(testResult),
                expiryDate = expiryDate,
                selfAssessment = testResult.symptomsOnsetDate != null
            )
        )
    }

    private fun updateIndexCaseWithTest(
        currentState: State,
        testResult: ReceivedTestResult
    ): State {
        // If this was a self-assessment index case, do not update it
        val indexCase = currentState.getIndexCase()
        if (indexCase != null && indexCase.selfAssessment) {
            return currentState
        }

        // Else, replace with a new index case
        return replaceIndexCase(currentState, testResult)
    }

    private fun replaceIndexCase(
        currentState: State,
        testResult: ReceivedTestResult
    ): State {
        val indexCaseIsolation = createIndexCase(currentState, testResult)
        return if (indexCaseIsolation.hasExpired(clock)) {
            when (currentState) {
                is Default ->
                    if (currentState.previousIsolation != null)
                        Default(previousIsolation = mergeIsolations(currentState.previousIsolation, indexCaseIsolation))
                    else
                        Default(previousIsolation = indexCaseIsolation)
                is Isolation -> currentState
            }
        } else {
            when (currentState) {
                is Default -> indexCaseIsolation
                is Isolation -> mergeIsolations(currentState, indexCaseIsolation)
            }
        }
    }

    private fun mergeIsolations(possiblyContactIsolation: Isolation, indexIsolation: Isolation): Isolation =
        Isolation(
            isolationStart = selectEarliest(possiblyContactIsolation.isolationStart, indexIsolation.isolationStart),
            isolationConfiguration = indexIsolation.isolationConfiguration,
            contactCase = possiblyContactIsolation.contactCase,
            indexCase = indexIsolation.indexCase
        )

    private fun getIsolationExpiryDateBasedOnTest(
        testResult: ReceivedTestResult,
        durationDays: DurationDays = isolationConfigurationProvider.durationDays
    ): LocalDate {
        val indexCaseSinceTestResultEndDate = durationDays.indexCaseSinceTestResultEndDate.toLong()
        return if (testResult.symptomsOnsetDate?.explicitDate != null) {
            testResult.symptomsOnsetDate.explicitDate.plusDays(indexCaseSinceTestResultEndDate)
        } else {
            val testResultDate = LocalDateTime.ofInstant(testResult.testEndDate, clock.zone).toLocalDate()
            testResultDate.plusDays(indexCaseSinceTestResultEndDate)
        }
    }

    private fun testEndDay(testResult: ReceivedTestResult): LocalDate =
        LocalDateTime.ofInstant(testResult.testEndDate, clock.zone).toLocalDate()

    sealed class TransitionDueToTestResult {
        data class Ignore(
            val preventKeySubmission: Boolean
        ) : TransitionDueToTestResult()
        data class DoNotTransitionButStoreTestResult(
            val testResultStorageOperation: TestResultStorageOperation
        ) : TransitionDueToTestResult()
        data class TransitionAndStoreTestResult(
            val newState: State,
            val testResultStorageOperation: TestResultStorageOperation
        ) : TransitionDueToTestResult()
    }

    sealed class IsolationUpdate(
        val requiresStateUpdate: Boolean,
        val testStorageOperation: TestResultStorageOperation,
        val preventKeySubmission: Boolean = false
    ) {
        object Overwrite : IsolationUpdate(
            requiresStateUpdate = true,
            testStorageOperation = TestResultStorageOperation.Overwrite
        )
        data class OverwriteAndConfirm(val confirmedDate: Instant) : IsolationUpdate(
            requiresStateUpdate = true,
            testStorageOperation = TestResultStorageOperation.OverwriteAndConfirm(confirmedDate)
        )
        object Update : IsolationUpdate(
            requiresStateUpdate = true,
            testStorageOperation = TestResultStorageOperation.Overwrite
        )
        data class UpdateAndConfirm(val confirmedDate: Instant) : IsolationUpdate(
            requiresStateUpdate = true,
            testStorageOperation = TestResultStorageOperation.OverwriteAndConfirm(confirmedDate)
        )
        data class Confirm(val confirmedDate: Instant) : IsolationUpdate(
            requiresStateUpdate = false,
            testStorageOperation = TestResultStorageOperation.Confirm(confirmedDate)
        )
        object Nothing : IsolationUpdate(
            requiresStateUpdate = false,
            testStorageOperation = TestResultStorageOperation.Ignore
        )
        object Ignore : IsolationUpdate(
            requiresStateUpdate = false,
            testStorageOperation = TestResultStorageOperation.Ignore,
            preventKeySubmission = true
        )
    }
}
