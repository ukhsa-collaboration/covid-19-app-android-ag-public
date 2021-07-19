@file:Suppress("EnumEntryName")

package uk.nhs.nhsx.covid19.android.app.isolation

import com.squareup.moshi.JsonClass
import uk.nhs.nhsx.covid19.android.app.isolation.IsolationState.ACTIVE
import uk.nhs.nhsx.covid19.android.app.isolation.IsolationState.FINISHED
import uk.nhs.nhsx.covid19.android.app.isolation.IsolationState.NONE
import uk.nhs.nhsx.covid19.android.app.isolation.PositiveTestCaseState.notIsolatingAndHasNegativeTest
import uk.nhs.nhsx.covid19.android.app.isolation.TestType.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.isolation.TestType.POSITIVE_CONFIRMED
import uk.nhs.nhsx.covid19.android.app.isolation.TestType.POSITIVE_UNCONFIRMED

@JsonClass(generateAdapter = true)
data class Transition(
    val initialState: State,
    val event: Event,
    val finalState: State,
    val reference: TransitionReference = TransitionReference("", 0)
)

@JsonClass(generateAdapter = true)
data class TransitionReference(
    val file: String,
    val line: Int
)

@JsonClass(generateAdapter = true)
data class State(
    val contact: ContactCaseState,
    val symptomatic: SymptomaticCaseState,
    val positiveTest: PositiveTestCaseState,
) {
    val combinedIsolationState = setOf(contact.isolationState, symptomatic.isolationState, positiveTest.isolationState)
    val combinedIndexIsolationState = setOf(symptomatic.isolationState, positiveTest.isolationState)
    val indexIsolationHasBeenTerminatedByNegativeTest = symptomatic.isolationState == FINISHED && positiveTest == notIsolatingAndHasNegativeTest
    val strongestIsolationState = when {
        combinedIsolationState.contains(ACTIVE) -> ACTIVE
        combinedIsolationState.contains(FINISHED) -> FINISHED
        else -> NONE
    }
}

enum class ContactCaseState(
    val isolationState: IsolationState,
    val terminatedDueToDCT: Boolean = false
) {
    noIsolation(NONE),
    isolating(ACTIVE),
    notIsolatingAndHadRiskyContactPreviously(FINISHED),
    notIsolatingAndHadRiskyContactIsolationTerminatedDueToDCT(FINISHED, true)
}

enum class SymptomaticCaseState(
    val isolationState: IsolationState
) {
    noIsolation(NONE),
    isolating(ACTIVE),
    notIsolatingAndHadSymptomsPreviously(FINISHED)
}

enum class PositiveTestCaseState(
    val isolationState: IsolationState,
    val testType: TestType = TestType.NONE
) {
    noIsolation(NONE),
    isolatingWithConfirmedTest(ACTIVE, POSITIVE_CONFIRMED),
    isolatingWithUnconfirmedTest(ACTIVE, POSITIVE_UNCONFIRMED),
    notIsolatingAndHadConfirmedTestPreviously(FINISHED, POSITIVE_CONFIRMED),
    notIsolatingAndHadUnconfirmedTestPreviously(FINISHED, POSITIVE_UNCONFIRMED),
    notIsolatingAndHasNegativeTest(NONE, NEGATIVE)
}

enum class IsolationState {
    NONE,
    ACTIVE,
    FINISHED
}

enum class TestType {
    NONE,
    NEGATIVE,
    POSITIVE_CONFIRMED,
    POSITIVE_UNCONFIRMED
}

enum class Event {
    // External:
    riskyContact,
    riskyContactWithExposureDayOlderThanIsolationTerminationDueToDCT,

    selfDiagnosedSymptomatic,
    selfDiagnosedSymptomaticWithAssumedOnsetDateOlderThanPositiveTestEndDate,

    terminateRiskyContactDueToDCT,

    receivedConfirmedPositiveTest,
    receivedConfirmedPositiveTestWithEndDateOlderThanRememberedNegativeTestEndDate,
    receivedConfirmedPositiveTestWithEndDateOlderThanAssumedSymptomOnsetDate,
    receivedConfirmedPositiveTestWithIsolationPeriodOlderThanAssumedSymptomOnsetDate,

    receivedUnconfirmedPositiveTest,
    receivedUnconfirmedPositiveTestWithEndDateOlderThanRememberedNegativeTestEndDate,
    receivedUnconfirmedPositiveTestWithEndDateOlderThanAssumedSymptomOnsetDate,
    receivedUnconfirmedPositiveTestWithIsolationPeriodOlderThanAssumedSymptomOnsetDate,
    receivedUnconfirmedPositiveTestWithEndDateNDaysOlderThanRememberedNegativeTestEndDateAndOlderThanAssumedSymptomOnsetDayIfAny,

    receivedNegativeTest,
    receivedNegativeTestWithEndDateOlderThanRememberedUnconfirmedTestEndDate,
    receivedNegativeTestWithEndDateOlderThanAssumedSymptomOnsetDate,
    receivedNegativeTestWithEndDateNDaysNewerThanRememberedUnconfirmedTestEndDate,
    receivedNegativeTestWithEndDateOlderThanRememberedUnconfirmedTestEndDateAndOlderThanAssumedSymptomOnsetDayIfAny,
    receivedNegativeTestWithEndDateNDaysNewerThanRememberedUnconfirmedTestEndDateButOlderThanAssumedSymptomOnsetDayIfAny,
    receivedNegativeTestWithEndDateNewerThanAssumedSymptomOnsetDateAndAssumedSymptomOnsetDateNewerThanPositiveTestEndDate,

    receivedVoidTest,

    // Time-based:
    contactIsolationEnded,
    indexIsolationEnded,
    retentionPeriodEnded
}
