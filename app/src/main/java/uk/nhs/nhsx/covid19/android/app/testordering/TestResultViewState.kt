package uk.nhs.nhsx.covid19.android.app.testordering

import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.ButtonAction.FINISH
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.ButtonAction.ORDER_TEST
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.ButtonAction.SHARE_KEYS

sealed class TestResultViewState(val buttonAction: ButtonAction) {
    object NegativeNotInIsolation : TestResultViewState(buttonAction = FINISH) // E
    object NegativeWillBeInIsolation : TestResultViewState(buttonAction = FINISH) // ?
    object NegativeWontBeInIsolation : TestResultViewState(buttonAction = FINISH) // A
    object PositiveWillBeInIsolation : TestResultViewState(buttonAction = SHARE_KEYS) // H
    object PositiveContinueIsolation : TestResultViewState(buttonAction = SHARE_KEYS) // C
    object PositiveContinueIsolationNoChange : TestResultViewState(buttonAction = FINISH)
    object PositiveWontBeInIsolation : TestResultViewState(buttonAction = SHARE_KEYS) // G
    object NegativeAfterPositiveOrSymptomaticWillBeInIsolation : TestResultViewState(buttonAction = FINISH) // D
    object PositiveWillBeInIsolationAndOrderTest : TestResultViewState(buttonAction = ORDER_TEST)
    object VoidNotInIsolation : TestResultViewState(buttonAction = ORDER_TEST) // F
    object VoidWillBeInIsolation : TestResultViewState(buttonAction = ORDER_TEST) // B
    object Ignore : TestResultViewState(buttonAction = FINISH)

    enum class ButtonAction {
        SHARE_KEYS,
        ORDER_TEST,
        FINISH
    }
}
