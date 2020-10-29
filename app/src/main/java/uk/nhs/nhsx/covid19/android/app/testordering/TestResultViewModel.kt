package uk.nhs.nhsx.covid19.android.app.testordering

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnTestResultAcknowledge
import uk.nhs.nhsx.covid19.android.app.state.State
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.newStateWithTestResult
import uk.nhs.nhsx.covid19.android.app.state.previousIsolationIsIndexCase
import uk.nhs.nhsx.covid19.android.app.state.remainingDaysInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.Ignore
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.NegativeNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.NegativeWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.NegativeWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.PositiveContinueIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.PositiveThenNegativeWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.PositiveWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.PositiveWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.VoidNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.VoidWillBeInIsolation
import java.time.Clock
import javax.inject.Inject

class TestResultViewModel constructor(
    private val testResultsProvider: TestResultsProvider,
    private val isolationConfigurationProvider: IsolationConfigurationProvider,
    private val stateMachine: IsolationStateMachine,
    private val clock: Clock
) : ViewModel() {

    @Inject
    constructor(
        testResultsProvider: TestResultsProvider,
        isolationConfigurationProvider: IsolationConfigurationProvider,
        stateMachine: IsolationStateMachine
    ) : this(testResultsProvider, isolationConfigurationProvider, stateMachine, Clock.systemDefaultZone())

    private val viewState = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewState

    private var wasAcknowledged = false

    private lateinit var testResult: ReceivedTestResult

    fun onCreate() {
        if (viewState.value != null) {
            return
        }

        val receivedTestResult = getHighestPriorityTestResult()
        if (receivedTestResult == null) {
            val remainingDaysInIsolation = stateMachine.remainingDaysInIsolation().toInt()
            viewState.postValue(ViewState(Ignore, remainingDaysInIsolation))
        } else {
            testResult = receivedTestResult

            val state = stateMachine.readState()
            val newStateWithTestResult =
                state.newStateWithTestResult(testResultsProvider, isolationConfigurationProvider, testResult, clock)
            val willBeInIsolation = newStateWithTestResult is Isolation
            val isLastTestResultPositive = testResultsProvider.isLastTestResultPositive()

            val mainState = when (testResult.testResult) {
                POSITIVE -> mainStateWhenPositive(willBeInIsolation, state)
                NEGATIVE -> when (state) {
                    is Isolation -> when (isLastTestResultPositive) {
                        true -> PositiveThenNegativeWillBeInIsolation
                        false -> {
                            when (willBeInIsolation) {
                                true -> NegativeWillBeInIsolation
                                false -> NegativeWontBeInIsolation
                            }
                        }
                    }
                    is Default -> NegativeNotInIsolation
                }
                VOID -> when (state) {
                    is Isolation -> VoidWillBeInIsolation
                    is Default -> VoidNotInIsolation
                }
            }
            val remainingDaysInIsolation =
                stateMachine.remainingDaysInIsolation(newStateWithTestResult).toInt()
            viewState.postValue(ViewState(mainState, remainingDaysInIsolation))
        }
    }

    private fun mainStateWhenPositive(
        willBeInIsolation: Boolean,
        state: State
    ): MainState {
        val isLastTestResultNegative = testResultsProvider.isLastTestResultNegative()

        return when (state) {
            is Isolation -> mainStateWhenPositiveIgnoringLastTestResult(willBeInIsolation)
            is Default -> {
                if (!willBeInIsolation) {
                    PositiveWontBeInIsolation(testResult.diagnosisKeySubmissionToken)
                } else {
                    if (state.previousIsolationIsIndexCase() && !isLastTestResultNegative) {
                        PositiveContinueIsolation(testResult.diagnosisKeySubmissionToken)
                    } else {
                        PositiveWillBeInIsolation(testResult.diagnosisKeySubmissionToken)
                    }
                }
            }
        }
    }

    private fun mainStateWhenPositiveIgnoringLastTestResult(willBeInIsolation: Boolean): MainState {
        return when (willBeInIsolation) {
            true -> PositiveContinueIsolation(testResult.diagnosisKeySubmissionToken)
            false -> PositiveWontBeInIsolation(testResult.diagnosisKeySubmissionToken)
        }
    }

    fun acknowledgeTestResult() {
        if (wasAcknowledged) {
            return
        }

        wasAcknowledged = true

        viewState.value?.let {
            when (it.mainState) {
                PositiveThenNegativeWillBeInIsolation ->
                    stateMachine.processEvent(
                        OnTestResultAcknowledge(testResult, removeTestResult = true)
                    )
                else ->
                    stateMachine.processEvent(
                        OnTestResultAcknowledge(testResult, removeTestResult = false)
                    )
            }
        }
    }

    private fun getHighestPriorityTestResult(): ReceivedTestResult? {
        testResultsProvider.testResults.values
            .filter { it.acknowledgedDate == null }
            .firstOrNull { it.testResult == POSITIVE }
            ?.let { return it }

        testResultsProvider.testResults.values
            .filter { it.acknowledgedDate == null }
            .firstOrNull { it.testResult == NEGATIVE }
            ?.let { return it }

        testResultsProvider.testResults.values
            .filter { it.acknowledgedDate == null }
            .firstOrNull { it.testResult == VOID }
            ?.let { return it }

        return null
    }

    data class ViewState(
        val mainState: MainState,
        val remainingDaysInIsolation: Int
    )

    sealed class MainState {
        object NegativeNotInIsolation : MainState()
        object NegativeWillBeInIsolation : MainState()
        object NegativeWontBeInIsolation : MainState()
        data class PositiveWillBeInIsolation(val diagnosisKeySubmissionToken: String) : MainState()
        data class PositiveContinueIsolation(val diagnosisKeySubmissionToken: String) : MainState()
        data class PositiveWontBeInIsolation(val diagnosisKeySubmissionToken: String) : MainState()
        object PositiveThenNegativeWillBeInIsolation : MainState()
        object VoidNotInIsolation : MainState()
        object VoidWillBeInIsolation : MainState()
        object Ignore : MainState()
    }
}
