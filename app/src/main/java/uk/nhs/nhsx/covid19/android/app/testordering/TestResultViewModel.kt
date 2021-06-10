package uk.nhs.nhsx.covid19.android.app.testordering

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.AskedToShareExposureKeysInTheInitialFlow
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.common.SubmitEmptyData
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.SubmitEpidemiologyDataForTestResult
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.SubmitObfuscationData
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.PLOD
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnTestResultAcknowledge
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.DoNotTransition
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.Transition
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.ButtonAction.FINISH
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.ButtonAction.ORDER_TEST
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.ButtonAction.SHARE_KEYS
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.Ignore
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeAfterPositiveOrSymptomaticWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PlodWillContinueWithCurrentState
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveContinueIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveContinueIsolationNoChange
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWillBeInIsolationAndOrderTest
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.VoidNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.VoidWillBeInIsolation
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

class TestResultViewModel @Inject constructor(
    private val unacknowledgedTestResultsProvider: UnacknowledgedTestResultsProvider,
    private val testResultIsolationHandler: TestResultIsolationHandler,
    private val stateMachine: IsolationStateMachine,
    private val submitObfuscationData: SubmitObfuscationData,
    private val submitEmptyData: SubmitEmptyData,
    private val submitEpidemiologyDataForTestResult: SubmitEpidemiologyDataForTestResult,
    private val clock: Clock,
    private val analyticsEventProcessor: AnalyticsEventProcessor,
) : BaseTestResultViewModel() {
    private var wasAcknowledged = false
    private var preventKeySubmission = false

    private lateinit var testResult: ReceivedTestResult

    override fun onCreate() {
        if (viewState.value != null) {
            return
        }

        val receivedTestResult = getHighestPriorityTestResult()
        if (receivedTestResult == null) {
            val remainingDaysInIsolation = stateMachine.remainingDaysInIsolation().toInt()
            viewState.postValue(ViewState(Ignore, remainingDaysInIsolation))
        } else {
            testResult = receivedTestResult

            val currentState = stateMachine.readLogicalState()

            val transition = testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                currentState,
                testResult,
                testAcknowledgedDate = Instant.now(clock)
            )
            val newState = when (transition) {
                is Transition -> IsolationLogicalState.from(transition.newState)
                is DoNotTransition -> currentState
            }
            preventKeySubmission = transition is DoNotTransition && transition.preventKeySubmission

            val mainState = when (testResult.testResult) {
                POSITIVE -> mainStateWhenPositive(currentState, newState)
                NEGATIVE -> mainStateWhenNegative(currentState, newState)
                VOID -> when {
                    currentState.isActiveIsolation(clock) -> VoidWillBeInIsolation // B
                    else -> VoidNotInIsolation // F
                }
                PLOD -> PlodWillContinueWithCurrentState
            }
            val remainingDaysInIsolation =
                stateMachine.remainingDaysInIsolation(newState).toInt()
            viewState.postValue(ViewState(mainState, remainingDaysInIsolation))
        }
    }

    private fun mainStateWhenPositive(
        currentState: IsolationLogicalState,
        newState: IsolationLogicalState
    ): TestResultViewState =
        if (newState.isActiveIsolation(clock)) {
            if (testResult.requiresConfirmatoryTest && !isKeySubmissionSupported()) {
                if (currentState.isIsolatingWithPositiveConfirmedTest()) PositiveContinueIsolationNoChange
                else PositiveWillBeInIsolationAndOrderTest
            } else {
                if (currentState.isActiveIsolation(clock)) PositiveContinueIsolation // C
                else PositiveWillBeInIsolation // H
            }
        } else {
            PositiveWontBeInIsolation // G
        }

    private fun mainStateWhenNegative(
        currentState: IsolationLogicalState,
        newState: IsolationLogicalState
    ): TestResultViewState {
        val wasInIsolation = currentState.isActiveIsolation(clock)
        return if (wasInIsolation) {
            val willContinueToIsolate = newState.isActiveIsolation(clock)
            if (willContinueToIsolate) {
                if (isTestResultBeingCompleted(currentState, newState))
                    NegativeWillBeInIsolation
                else if (newState is PossiblyIsolating && newState.isActiveIndexCase(clock))
                    NegativeAfterPositiveOrSymptomaticWillBeInIsolation // D
                else
                    NegativeWillBeInIsolation
            } else
                NegativeWontBeInIsolation // A
        } else {
            NegativeNotInIsolation // E
        }
    }

    private fun isTestResultBeingCompleted(
        currentState: IsolationLogicalState,
        newState: IsolationLogicalState
    ): Boolean {
        val currentStateTestResultIsNotCompleted =
            currentState.toIsolationState().indexInfo?.testResult?.confirmatoryTestCompletionStatus == null
        val newStateTestResultIsCompleted =
            newState.toIsolationState().indexInfo?.testResult?.confirmatoryTestCompletionStatus != null
        return currentStateTestResultIsNotCompleted && newStateTestResultIsCompleted
    }

    private fun IsolationLogicalState.isIsolatingWithPositiveConfirmedTest(): Boolean =
        this is PossiblyIsolating &&
            hasActiveConfirmedPositiveTestResult(clock)

    private fun isKeySubmissionSupported(): Boolean =
        testResult.diagnosisKeySubmissionSupported && !preventKeySubmission

    override fun onActionButtonClicked() {
        val currentState = stateMachine.readLogicalState()

        acknowledgeTestResult()

        val navigationEvent = getNavigationEvent(currentState)

        if (navigationEvent != null) {
            navigationEventLiveData.postValue(navigationEvent)
        } else {
            Timber.d("Unexpected button action ${viewState.value?.mainState?.buttonAction}")
        }
    }

    private fun getNavigationEvent(currentState: IsolationLogicalState): NavigationEvent? =
        when (viewState.value?.mainState?.buttonAction) {
            FINISH -> NavigationEvent.Finish
            SHARE_KEYS -> {
                if (isKeySubmissionSupported()) {
                    viewModelScope.launch {
                        analyticsEventProcessor.track(AskedToShareExposureKeysInTheInitialFlow)
                    }
                    NavigationEvent.NavigateToShareKeys(
                        bookFollowUpTest = testResult.requiresConfirmatoryTest &&
                            !currentState.isIsolatingWithPositiveConfirmedTest()
                    )
                } else NavigationEvent.Finish
            }
            ORDER_TEST -> NavigationEvent.NavigateToOrderTest
            else -> null
        }

    override fun onBackPressed() {
        acknowledgeTestResult()
    }

    private fun acknowledgeTestResult() {
        if (wasAcknowledged) {
            return
        }
        wasAcknowledged = true
        submitEpidemiologyData()
        stateMachine.processEvent(
            OnTestResultAcknowledge(testResult)
        )
    }

    private fun submitEpidemiologyData() {
        with(testResult) {
            if (isPositive()) {
                submitEpidemiologyDataForTestResult(testKitType, requiresConfirmatoryTest)
                if (!isKeySubmissionSupported()) {
                    submitEmptyData()
                }
            } else {
                submitObfuscationData()
            }
        }
    }

    private fun getHighestPriorityTestResult(): ReceivedTestResult? {
        val testPriority = listOf(POSITIVE, PLOD, NEGATIVE, VOID)
        unacknowledgedTestResultsProvider.testResults
            .minByOrNull { testPriority.indexOf(it.testResult) }
            ?.let { return it }

        return null
    }
}
