package uk.nhs.nhsx.covid19.android.app.testordering

data class AcknowledgementCompletionActions(
    val suggestBookTest: BookTestOption,
    val shouldAllowKeySubmission: Boolean
)

enum class BookTestOption {
    NoTest,
    FollowUpTest,
    RegularTest
}

enum class TestResultViewState {
    NegativeNotInIsolation,
    NegativeWillBeInIsolation,
    NegativeWontBeInIsolation,
    NegativeAfterPositiveOrSymptomaticWillBeInIsolation,
    PositiveWillBeInIsolation,
    PositiveContinueIsolation,
    PositiveContinueIsolationNoChange,
    PositiveWontBeInIsolation,
    VoidNotInIsolation,
    VoidWillBeInIsolation,
    PlodWillContinueWithCurrentState,
    Ignore
}
