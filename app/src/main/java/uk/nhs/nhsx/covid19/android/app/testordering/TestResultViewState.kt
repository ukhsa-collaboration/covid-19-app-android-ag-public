package uk.nhs.nhsx.covid19.android.app.testordering

import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.ButtonAction.Finish
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.ButtonAction.OrderTest

sealed class TestResultViewState(open val buttonAction: ButtonAction) {
    object NegativeNotInIsolation : TestResultViewState(buttonAction = Finish)
    object NegativeWillBeInIsolation : TestResultViewState(buttonAction = Finish)
    object NegativeWontBeInIsolation : TestResultViewState(buttonAction = Finish)
    data class PositiveWillBeInIsolation(override val buttonAction: ButtonAction) : TestResultViewState(buttonAction)
    data class PositiveContinueIsolation(override val buttonAction: ButtonAction) : TestResultViewState(buttonAction)
    object PositiveContinueIsolationNoChange : TestResultViewState(buttonAction = Finish)
    data class PositiveWontBeInIsolation(override val buttonAction: ButtonAction) : TestResultViewState(buttonAction)
    object NegativeAfterPositiveOrSymptomaticWillBeInIsolation : TestResultViewState(buttonAction = Finish)
    object PositiveWillBeInIsolationAndOrderTest : TestResultViewState(buttonAction = OrderTest)
    object VoidNotInIsolation : TestResultViewState(buttonAction = OrderTest)
    object VoidWillBeInIsolation : TestResultViewState(buttonAction = OrderTest)
    object PlodWillContinueWithCurrentState : TestResultViewState(buttonAction = Finish)
    object Ignore : TestResultViewState(buttonAction = Finish)

    sealed class ButtonAction {
        data class ShareKeys(val bookFollowUpTest: Boolean) : ButtonAction()
        object OrderTest : ButtonAction()
        object Finish : ButtonAction()
    }
}
