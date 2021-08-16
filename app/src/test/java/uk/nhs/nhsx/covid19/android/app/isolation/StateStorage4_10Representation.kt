@file:Suppress("ClassName")

package uk.nhs.nhsx.covid19.android.app.isolation

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.junit.Assume
import uk.nhs.nhsx.covid19.android.app.isolation.Event.contactIsolationEnded
import uk.nhs.nhsx.covid19.android.app.isolation.Event.indexIsolationEnded
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedConfirmedPositiveTestWithEndDateOlderThanRememberedNegativeTestEndDate
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedConfirmedPositiveTestWithIsolationPeriodOlderThanAssumedIsolationStartDate
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedNegativeTestWithEndDateNDaysNewerThanRememberedUnconfirmedTestEndDate
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedNegativeTestWithEndDateNDaysNewerThanRememberedUnconfirmedTestEndDateButOlderThanAssumedSymptomOnsetDayIfAny
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedNegativeTestWithEndDateNewerThanAssumedSymptomOnsetDateAndAssumedSymptomOnsetDateNewerThanPositiveTestEndDate
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedNegativeTestWithEndDateOlderThanAssumedSymptomOnsetDate
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedUnconfirmedPositiveTestWithEndDateNDaysOlderThanRememberedNegativeTestEndDateAndOlderThanAssumedSymptomOnsetDayIfAny
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedUnconfirmedPositiveTestWithEndDateOlderThanAssumedSymptomOnsetDate
import uk.nhs.nhsx.covid19.android.app.isolation.Event.selfDiagnosedSymptomatic
import uk.nhs.nhsx.covid19.android.app.isolation.IsolationState.ACTIVE
import uk.nhs.nhsx.covid19.android.app.isolation.IsolationState.FINISHED
import uk.nhs.nhsx.covid19.android.app.isolation.IsolationState.NONE
import uk.nhs.nhsx.covid19.android.app.isolation.SymptomaticCaseState.notIsolatingAndHadSymptomsPreviously
import uk.nhs.nhsx.covid19.android.app.isolation.TestType.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.isolation.TestType.POSITIVE_CONFIRMED
import uk.nhs.nhsx.covid19.android.app.isolation.TestType.POSITIVE_UNCONFIRMED
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.OptOutOfContactIsolation
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateJson
import uk.nhs.nhsx.covid19.android.app.state.SymptomaticCase
import uk.nhs.nhsx.covid19.android.app.state.assumedDaysFromOnsetToSelfAssessment
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.LocalDateAdapter
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class StateStorage4_10Provider : StateRepresentationProvider {
    override fun getStateRepresentations(state: State, event: Event?): List<StateRepresentation> {
        return listOf(
            StateStorage4_10Representation(state, event)
        )
    }
}

/**
 * This representation creates a state in the format of the app version 4.10 and stores it in the shared preferences
 */
class StateStorage4_10Representation(
    private val state: State,
    private val event: Event?
) : StateRepresentation {
    override val representationName = "4.10"

    override fun setupState(isolationTestContext: IsolationTestContext, isolationConfiguration: DurationDays) =
        StateProducer(isolationTestContext, isolationConfiguration).setupState(state, event)

    private class StateProducer(
        private val isolationTestContext: IsolationTestContext,
        private val isolationConfiguration: DurationDays
    ) {
        private val today = LocalDate.now(isolationTestContext.clock)

        private val moshi: Moshi = Moshi.Builder()
            .add(LocalDateAdapter())
            .add(InstantAdapter())
            .build()

        private val stateSerializationAdapter: JsonAdapter<IsolationStateJson> =
            moshi.adapter(IsolationStateJson::class.java)

        fun setupState(state: State, event: Event?) {
            val isolationState = computeIsolationState(state, event)
            val isolationStateJson = stateSerializationAdapter.toJson(isolationState)
            isolationTestContext.setStateStringStorage(isolationStateJson)

            // Invalidate state machine to make it read the updated state from the storage
            isolationTestContext.getIsolationStateMachine().invalidateStateMachine()
        }

        private fun computeIsolationState(state: State, event: Event?): IsolationStateJson {
            var contactCase = computeContactCase(state)
            var symptomaticCase = computeSymptomaticCase(state)
            var positiveTestCase = computePositiveTestCase(state)

            var indexExpiryDate = when {
                state.indexIsolationHasBeenTerminatedByNegativeTest -> positiveTestCase!!.testEndDate
                symptomaticCase != null -> symptomaticCase.selfDiagnosisDate.plusDays(isolationConfiguration.indexCaseSinceSelfDiagnosisUnknownOnset.toLong())
                else -> positiveTestCase?.testEndDate?.plusDays(isolationConfiguration.indexCaseSinceTestResultEndDate.toLong())
            }

            // Special handling for time-based events
            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (event) {
                selfDiagnosedSymptomatic -> {
                    if (state.positiveTest.isolationState == ACTIVE) {
                        // Self-diagnosis onset day needs to be later than testEndDate
                        val testEndDate = today.minusDays(assumedDaysFromOnsetToSelfAssessment + 1)
                        positiveTestCase = computePositiveTestCase(state, testEndDate)
                    }
                }
                indexIsolationEnded -> {
                    // Let indexCase expire earlier than contact case
                    indexExpiryDate = indexExpiryDate?.minusDays(4)
                }
                contactIsolationEnded -> {
                    // Let contact case expire earlier than index case
                    contactCase = contactCase?.copy(expiryDate = contactCase.expiryDate.minusDays(4))
                }
                receivedUnconfirmedPositiveTestWithEndDateOlderThanAssumedSymptomOnsetDate -> {
                    if (state.contact.isolationState == ACTIVE) {
                        // Let contact case isolation overlap with isolation triggered by symptoms/received test result
                        contactCase = createOverlappingContactCase()
                    }
                }
                receivedUnconfirmedPositiveTestWithEndDateNDaysOlderThanRememberedNegativeTestEndDateAndOlderThanAssumedSymptomOnsetDayIfAny -> {
                    if (state.symptomatic == notIsolatingAndHadSymptomsPreviously) {
                        // Symptomatic case has been terminated by negative test, thus let it start briefly before that
                        assertNotNull(positiveTestCase)
                        assertFalse(positiveTestCase.isPositive())
                        symptomaticCase = createSymptomaticCase(positiveTestCase.testEndDate.minusDays(1))
                    }
                }
                receivedConfirmedPositiveTestWithIsolationPeriodOlderThanAssumedIsolationStartDate -> {
                    if (state.contact.isolationState == ACTIVE && state.symptomatic == notIsolatingAndHadSymptomsPreviously) {
                        // Let contact case isolation overlap with isolation triggered by symptoms/received test result
                        contactCase = createOverlappingContactCase()
                    }
                    if (state.indexIsolationHasBeenTerminatedByNegativeTest) {
                        // Let isolation caused by test result be longer than symptomatic isolation which has been terminated by the negative test result
                        symptomaticCase = createOverlappingSymptomaticCase()
                    }
                }
                receivedConfirmedPositiveTestWithEndDateOlderThanRememberedNegativeTestEndDate -> {
                    if (state.indexIsolationHasBeenTerminatedByNegativeTest) {
                        // Let index case isolation be still active if it hadn't been terminated by negative test
                        symptomaticCase = createOverlappingSymptomaticCase()
                    }
                }
                receivedNegativeTestWithEndDateOlderThanAssumedSymptomOnsetDate -> {
                    // Create test older than onset date
                    val testEndDate =
                        symptomaticCase!!.selfDiagnosisDate.minusDays(assumedDaysFromOnsetToSelfAssessment + 1)
                    positiveTestCase = computePositiveTestCase(state, testEndDate)
                }
                receivedNegativeTestWithEndDateNDaysNewerThanRememberedUnconfirmedTestEndDate -> {
                    // Create test older than onset date and more than CONFIRMATORY_DAY_LIMIT in the past
                    val testEndDate =
                        symptomaticCase?.selfDiagnosisDate?.minusDays(assumedDaysFromOnsetToSelfAssessment + DEFAULT_CONFIRMATORY_DAY_LIMIT + 1)
                            ?: today.minusDays(DEFAULT_CONFIRMATORY_DAY_LIMIT + 1)
                    positiveTestCase = computePositiveTestCase(state, testEndDate)
                }
                receivedNegativeTestWithEndDateNDaysNewerThanRememberedUnconfirmedTestEndDateButOlderThanAssumedSymptomOnsetDayIfAny -> {
                    // Create test results with enough distance to the onsetDay (if available) to be able to receive another test result in between
                    val testEndDate =
                        symptomaticCase?.selfDiagnosisDate?.minusDays(assumedDaysFromOnsetToSelfAssessment + DEFAULT_CONFIRMATORY_DAY_LIMIT + 2)
                            ?: today.minusDays(DEFAULT_CONFIRMATORY_DAY_LIMIT + 2)
                    positiveTestCase = computePositiveTestCase(state, testEndDate)
                }
                receivedNegativeTestWithEndDateNewerThanAssumedSymptomOnsetDateAndAssumedSymptomOnsetDateNewerThanPositiveTestEndDate -> {
                    // Create test older than onset date
                    val testEndDate =
                        symptomaticCase!!.selfDiagnosisDate.minusDays(assumedDaysFromOnsetToSelfAssessment + 1)
                    positiveTestCase = computePositiveTestCase(state, testEndDate)
                }
            }

            return IsolationStateJson(
                isolationConfiguration,
                contact = contactCase,
                testResult = positiveTestCase,
                symptomatic = symptomaticCase,
                indexExpiryDate = indexExpiryDate,
                hasAcknowledgedEndOfIsolation = false
            )
        }

        private fun computePositiveTestCase(
            state: State,
            testEndDate: LocalDate = computeTestEndDate(state.positiveTest.isolationState)
        ): AcknowledgedTestResult? {
            return when (state.positiveTest.testType) {
                TestType.NONE -> null
                NEGATIVE -> AcknowledgedTestResult(
                    testEndDate = testEndDate,
                    acknowledgedDate = today,
                    testResult = RelevantVirologyTestResult.NEGATIVE,
                    testKitType = LAB_RESULT,
                    requiresConfirmatoryTest = false,
                    confirmedDate = null
                )
                POSITIVE_CONFIRMED -> AcknowledgedTestResult(
                    testEndDate = testEndDate,
                    acknowledgedDate = today,
                    testResult = POSITIVE,
                    testKitType = LAB_RESULT,
                    requiresConfirmatoryTest = false,
                    confirmedDate = null
                )
                POSITIVE_UNCONFIRMED -> AcknowledgedTestResult(
                    testEndDate = testEndDate,
                    acknowledgedDate = today,
                    testResult = POSITIVE,
                    testKitType = LAB_RESULT,
                    requiresConfirmatoryTest = true,
                    confirmedDate = null,
                    confirmatoryDayLimit = DEFAULT_CONFIRMATORY_DAY_LIMIT.toInt()
                )
            }
        }

        private fun computeTestEndDate(isolationState: IsolationState) =
            when (isolationState) {
                NONE -> today.minusDays(4)
                ACTIVE -> today.minusDays(1)
                FINISHED ->
                    today.minusDays(isolationConfiguration.indexCaseSinceTestResultEndDate.toLong() + 1)
            }

        private fun computeContactCase(state: State) =
            when (state.contact.isolationState) {
                NONE -> null
                ACTIVE -> createCurrentContactCase()
                FINISHED -> {
                    if (state.contact.terminatedEarly) {
                        createContactCaseTerminatedEarly()
                    } else {
                        createOldContactCase()
                    }
                }
            }

        private fun createCurrentContactCase(): ContactCase =
            createContactCase(exposureDate = today.minusDays(1))

        private fun createOverlappingContactCase(): ContactCase =
            createContactCase(exposureDate = today.minusDays(6))

        private fun createOldContactCase(): ContactCase =
            createContactCase(
                exposureDate = today
                    .minusDays(isolationConfiguration.contactCase.toLong() + 1)
            )

        private fun createContactCaseTerminatedEarly(): ContactCase =
            ContactCase(
                exposureDate = today.minusDays(6),
                notificationDate = today.minusDays(5),
                expiryDate = today.minusDays(6),
                optOutOfContactIsolation = OptOutOfContactIsolation(today.minusDays(6))
            )

        private fun createContactCase(exposureDate: LocalDate): ContactCase =
            ContactCase(
                exposureDate = exposureDate,
                notificationDate = exposureDate.plus(1, DAYS),
                expiryDate = exposureDate.plusDays(isolationConfiguration.contactCase.toLong())
            )

        private fun computeSymptomaticCase(state: State) =
            when (state.symptomatic.isolationState) {
                NONE -> null
                ACTIVE -> createCurrentSymptomaticCase()
                FINISHED -> createOldSymptomaticCase()
            }

        private fun createCurrentSymptomaticCase(): SymptomaticCase =
            createSymptomaticCase(selfDiagnosisDate = today)

        private fun createOverlappingSymptomaticCase() =
            createSymptomaticCase(selfDiagnosisDate = today.minusDays(6))

        private fun createOldSymptomaticCase(): SymptomaticCase =
            createSymptomaticCase(
                selfDiagnosisDate = today
                    .minusDays(isolationConfiguration.indexCaseSinceSelfDiagnosisUnknownOnset.toLong() + 1)
            )

        private fun createSymptomaticCase(selfDiagnosisDate: LocalDate): SymptomaticCase =
            SymptomaticCase(
                selfDiagnosisDate = selfDiagnosisDate
            )
    }

    override fun skipUnsupportedState(state: State) {
        Assume.assumeTrue("Don't skip anything :)", true)
    }

    override fun skipUnsupportedEvent(event: Event) {
        Assume.assumeTrue("Don't skip anything :)", true)
    }

    companion object {
        const val DEFAULT_CONFIRMATORY_DAY_LIMIT = 2L
    }
}
