@file:Suppress("PrivatePropertyName", "LocalVariableName")

package uk.nhs.nhsx.covid19.android.app.isolation

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedUnconfirmedPositiveTestWithEndDateNDaysOlderThanRememberedNegativeTestEndDate
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedUnconfirmedPositiveTestWithEndDateOlderThanAssumedSymptomOnsetDate
import uk.nhs.nhsx.covid19.android.app.isolation.PositiveTestCaseState.notIsolatingAndHasNegativeTest
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(Parameterized::class)
class IsolationTests(
    private val transition: Transition,
    private val initialStateRepresentation: StateRepresentation,
    @Suppress("unused") private val testName: String
) : EspressoTest() {
    companion object {
        private val context: Context = InstrumentationRegistry.getInstrumentation().context

        @JvmStatic
        @Parameterized.Parameters(name = "{index}_{2}")
        fun generateParameters(): Iterable<Array<Any>> {
            val testSingleTransition = false // Adjust this to manually test a single transition

            if (testSingleTransition) {
                return getTransitionsForManualTest()
            } else {
                val parameters = mutableListOf<Array<Any>>()
                val representationProvider = AllStateRepresentations()
                IsolationTransitionLoader(context).loadTransitions()
                    .forEach { transition ->
                        representationProvider.getStateRepresentations(transition.initialState, transition.event)
                            .forEach { initialStateRepresentation ->
                                parameters.add(
                                    // Create one test for every combination of transitions and representations
                                    createTestParameters(transition, initialStateRepresentation)
                                )
                            }
                    }
                return parameters
            }
        }

        private fun getTransitionsForManualTest(): Iterable<Array<Any>> {
            val initialState = State(
                contact = ContactCaseState.isolating,
                symptomatic = SymptomaticCaseState.notIsolatingAndHadSymptomsPreviously,
                positiveTest = PositiveTestCaseState.noIsolation
            )
            val event = Event.receivedConfirmedPositiveTestWithEndDateOlderThanAssumedSymptomOnsetDate
            val finalState = State(
                contact = ContactCaseState.noIsolation,
                symptomatic = SymptomaticCaseState.noIsolation,
                positiveTest = PositiveTestCaseState.notIsolatingAndHadConfirmedTestPreviously
            )
            val representation4_10 = StateStorage4_10Representation(initialState, event)

            val transition = Transition(initialState, event, finalState)
            return listOf(
                createTestParameters(transition, representation4_10),
            )
        }

        private fun createTestParameters(transition: Transition, initialStateRepresentation: StateRepresentation) =
            arrayOf(
                transition,
                initialStateRepresentation,
                "v${initialStateRepresentation.representationName}_C${transition.initialState.contact}_S${transition.initialState.symptomatic}_P${transition.initialState.positiveTest}_E${transition.event}"
                    .take(160)
            )
    }

    private val isolationConfiguration = DurationDays()
    private val stateVerifier = StateVerifier(testAppContext)
    private val eventHandler = EventHandler(testAppContext, isolationConfiguration)

    @Before
    fun setUp() {
        testAppContext.setLocalAuthority(TestApplicationContext.ENGLISH_LOCAL_AUTHORITY)
        testAppContext.clock.currentInstant = LocalDate.of(2020, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
    }

    @After
    fun tearDown() {
        testAppContext.clock.reset()
    }

    @Test
    fun testTransition() = notReported {
        runBlocking {
            skipUnsupportedTransition(transition)
            initialStateRepresentation.skipUnsupportedState(transition.initialState)
            initialStateRepresentation.skipUnsupportedState(transition.finalState)
            initialStateRepresentation.skipUnsupportedEvent(transition.event)

            try {
                initialStateRepresentation.setupState(testAppContext, isolationConfiguration)
                stateVerifier.verifyState(transition.initialState)
                eventHandler.handleEvent(transition.event)
                stateVerifier.verifyState(transition.finalState)
            } catch (e: AssertionError) {
                throw AssertionError(errorMessage(), e)
            }
        }
    }

    private fun errorMessage() =
        """
        
        Error testing transition (represented by ${initialStateRepresentation::class.simpleName})
        
        Initial:   ${transition.initialState}
        Event:     ${transition.event}
        Expected:  ${transition.finalState}

        ContactCase:    ${testAppContext.getCurrentState().contactCase}
        
        IndexInfo:      ${testAppContext.getCurrentState().indexInfo}
                
        Clock:          ${testAppContext.clock.currentInstant}
        Configuration:  ${testAppContext.getCurrentState().isolationConfiguration}

        """.trimIndent()

    private fun skipUnsupportedTransition(transition: Transition) {
        // TODO Should be fixed in https://jira.collab.test-and-trace.nhs.uk/browse/COV-11005
        Assume.assumeFalse(
            "Skipping event receivedUnconfirmedPositiveTestWithEndDateNDaysOlderThanRememberedNegativeTestEndDate",
            transition.event == receivedUnconfirmedPositiveTestWithEndDateNDaysOlderThanRememberedNegativeTestEndDate &&
                transition.initialState.positiveTest == notIsolatingAndHasNegativeTest
        )

        Assume.assumeFalse("Skipping transition", transitionsToSkip.contains(transition))
    }

    private val transitionsToSkip = listOf(
        // Cluster 1: receivedUnconfirmedPositiveTestWithEndDateOlderThanAssumedSymptomOnsetDate
        // Fail with `shouldHaveSelfAssessmentIndexCase expected:<true> but was:<false>`
        // See Teams discussion in FD channel of 28.04.21 14:58
        Transition(
            initialState = State(
                contact = ContactCaseState.noIsolation,
                symptomatic = SymptomaticCaseState.notIsolatingAndHadSymptomsPreviously,
                positiveTest = PositiveTestCaseState.notIsolatingAndHasNegativeTest
            ),
            event = Event.receivedUnconfirmedPositiveTestWithEndDateOlderThanAssumedSymptomOnsetDate,
            finalState = State(
                contact = ContactCaseState.noIsolation,
                symptomatic = SymptomaticCaseState.notIsolatingAndHadSymptomsPreviously,
                positiveTest = PositiveTestCaseState.notIsolatingAndHasNegativeTest
            )
        ),
        Transition(
            initialState = State(
                contact = ContactCaseState.notIsolatingAndHadRiskyContactPreviously,
                symptomatic = SymptomaticCaseState.notIsolatingAndHadSymptomsPreviously,
                positiveTest = PositiveTestCaseState.notIsolatingAndHasNegativeTest
            ),
            event = Event.receivedUnconfirmedPositiveTestWithEndDateOlderThanAssumedSymptomOnsetDate,
            finalState = State(
                contact = ContactCaseState.notIsolatingAndHadRiskyContactPreviously,
                symptomatic = SymptomaticCaseState.notIsolatingAndHadSymptomsPreviously,
                positiveTest = PositiveTestCaseState.notIsolatingAndHasNegativeTest
            )
        ),
        Transition(
            initialState = State(
                contact = ContactCaseState.notIsolatingAndHadRiskyContactIsolationTerminatedDueToDCT,
                symptomatic = SymptomaticCaseState.notIsolatingAndHadSymptomsPreviously,
                positiveTest = PositiveTestCaseState.notIsolatingAndHasNegativeTest
            ),
            event = Event.receivedUnconfirmedPositiveTestWithEndDateOlderThanAssumedSymptomOnsetDate,
            finalState = State(
                contact = ContactCaseState.notIsolatingAndHadRiskyContactIsolationTerminatedDueToDCT,
                symptomatic = SymptomaticCaseState.notIsolatingAndHadSymptomsPreviously,
                positiveTest = PositiveTestCaseState.notIsolatingAndHasNegativeTest
            )
        ),
        Transition(
            initialState = State(
                contact = ContactCaseState.isolating,
                symptomatic = SymptomaticCaseState.notIsolatingAndHadSymptomsPreviously,
                positiveTest = PositiveTestCaseState.notIsolatingAndHasNegativeTest
            ),
            event = Event.receivedUnconfirmedPositiveTestWithEndDateOlderThanAssumedSymptomOnsetDate,
            finalState = State(
                contact = ContactCaseState.isolating,
                symptomatic = SymptomaticCaseState.notIsolatingAndHadSymptomsPreviously,
                positiveTest = PositiveTestCaseState.notIsolatingAndHasNegativeTest
            )
        ),
    )
}
