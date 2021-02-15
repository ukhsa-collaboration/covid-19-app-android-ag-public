package uk.nhs.nhsx.covid19.android.app.state

import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.DoNotTransitionButStoreTestResult
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.Ignore
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.TransitionAndStoreTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantTestResultProvider
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation.CONFIRM
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation.IGNORE
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation.OVERWRITE
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

    fun computeNextStateWithTestResult(currentState: State, testResult: ReceivedTestResult): State {
        return when (val transition = computeTransitionWithTestResult(currentState, testResult)) {
            is TransitionAndStoreTestResult -> transition.newState
            is DoNotTransitionButStoreTestResult -> currentState
            Ignore -> currentState
        }
    }

    fun computeTransitionWithTestResult(currentState: State, testResult: ReceivedTestResult): TransitionDueToTestResult {
        if (testResult.requiresConfirmatoryTest &&
            currentState is Isolation &&
            currentState.isIndexCase()
        ) {
            return Ignore
        } else if (!testResult.requiresConfirmatoryTest &&
            testResult.testResult == POSITIVE &&
            currentState is Isolation &&
            currentState.isIndexCase() &&
            currentState.hasConfirmedPositiveTestResult(relevantTestResultProvider)
        ) {
            return Ignore
        }

        var newState = if (testResult.testResult == POSITIVE &&
            (testResult.requiresConfirmatoryTest || (currentState is Isolation && currentState.isContactCaseOnly()))
        ) {
            tryCreateIndexCaseWhenOnsetDataIsNotProvided(
                currentState,
                testResult.testEndDate
            )
        } else if (currentState is Isolation && testResult.testResult == NEGATIVE) {
            when {
                // TODO try to simplify
                !currentState.hasUnconfirmedPositiveTestResult(relevantTestResultProvider) &&
                    relevantTestResultProvider.isTestResultPositive() -> currentState
                currentState.isIndexCaseOnly() -> Default(
                    previousIsolation = currentState.copy(
                        indexCase = currentState.indexCase!!.copy(expiryDate = LocalDate.now(clock))
                    )
                )
                currentState.isBothCases() -> currentState.copy(indexCase = null)
                else -> currentState
            }
        } else if (currentState is Default && testResult.testResult == POSITIVE) {
            when {
                relevantTestResultProvider.isTestResultNegative() -> tryCreateIndexCaseWhenOnsetDataIsNotProvided(
                    currentState,
                    testResult.testEndDate
                )
                currentState.previousIsolationIsIndexCase() -> currentState
                else ->
                    tryCreateIndexCaseWhenOnsetDataIsNotProvided(currentState, testResult.testEndDate)
            }
        } else {
            currentState
        }

        // A confirmed positive test result (expired or not) removes any memory of a contact case (current or past)
        if (testResult.testResult == POSITIVE && !testResult.requiresConfirmatoryTest) {
            newState = when (newState) {
                is Default -> newState.clearPreviousContactCase()
                is Isolation -> newState.clearContactCase()
            }
        }

        val testStorageOperation = computeTestResultStorageOperation(currentState, testResult)

        return if (newState == currentState) DoNotTransitionButStoreTestResult(testStorageOperation)
        else TransitionAndStoreTestResult(newState, testStorageOperation)
    }

    private fun computeTestResultStorageOperation(
        currentState: State,
        receivedTestResult: ReceivedTestResult
    ): TestResultStorageOperation {
        val previousTestResult = relevantTestResultProvider.testResult
        val isOrWasIndexCase = (currentState is Isolation && currentState.isIndexCase()) ||
            (currentState is Default && currentState.previousIsolation?.isIndexCase() ?: false)

        return when (receivedTestResult.testResult) {
            VOID -> IGNORE

            NEGATIVE ->
                if (!isOrWasIndexCase) OVERWRITE
                else when (previousTestResult?.testResult) {
                    RelevantVirologyTestResult.NEGATIVE -> IGNORE
                    RelevantVirologyTestResult.POSITIVE ->
                        if (previousTestResult.isConfirmed()) IGNORE
                        else OVERWRITE
                    else -> OVERWRITE
                }

            POSITIVE ->
                if (receivedTestResult.requiresConfirmatoryTest) {
                    if (currentState is Isolation && currentState.isIndexCase()) IGNORE
                    else OVERWRITE
                } else {
                    if (!isOrWasIndexCase) OVERWRITE
                    else when (previousTestResult?.testResult) {
                        RelevantVirologyTestResult.NEGATIVE -> OVERWRITE
                        RelevantVirologyTestResult.POSITIVE ->
                            if (previousTestResult.isConfirmed()) IGNORE
                            else CONFIRM
                        else -> OVERWRITE
                    }
                }
        }
    }

    private fun Default.previousIsolationIsIndexCase(): Boolean =
        this.previousIsolation != null && this.previousIsolation.isIndexCase()

    private fun Default.clearPreviousContactCase(): Default =
        this.previousIsolation?.let { previousIsolation ->
            if (previousIsolation.isContactCaseOnly()) Default()
            else this.copy(previousIsolation = previousIsolation.copy(contactCase = null))
        } ?: this

    private fun Isolation.clearContactCase(): State =
        if (this.isContactCaseOnly()) Default()
        else this.copy(contactCase = null)

    private fun tryCreateIndexCaseWhenOnsetDataIsNotProvided(
        currentState: State,
        testResultEndDate: Instant
    ): State {
        val testResultDate = LocalDateTime.ofInstant(testResultEndDate, clock.zone).toLocalDate()
        val durationDays = isolationConfigurationProvider.durationDays
        val isolation = Isolation(
            isolationStart = testResultEndDate,
            isolationConfiguration = durationDays,
            indexCase = IndexCase(
                symptomsOnsetDate = testResultDate.minusDays(indexCaseOnsetDateBeforeTestResultDate),
                expiryDate = testResultDate.plusDays(durationDays.indexCaseSinceTestResultEndDate.toLong()),
                selfAssessment = (currentState as? Isolation)?.indexCase?.selfAssessment ?: false
            )
        )
        return if (isolation.hasExpired(clock)) {
            currentState
        } else {
            when (currentState) {
                is Default -> isolation
                is Isolation -> currentState.copy(indexCase = isolation.indexCase)
            }
        }
    }

    sealed class TransitionDueToTestResult {
        object Ignore : TransitionDueToTestResult()
        data class DoNotTransitionButStoreTestResult(
            val testResultStorageOperation: TestResultStorageOperation
        ) : TransitionDueToTestResult()
        data class TransitionAndStoreTestResult(
            val newState: State,
            val testResultStorageOperation: TestResultStorageOperation
        ) : TransitionDueToTestResult()
    }
}
