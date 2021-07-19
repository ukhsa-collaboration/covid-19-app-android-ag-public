@file:Suppress("PrivatePropertyName", "LocalVariableName")

package uk.nhs.nhsx.covid19.android.app.isolation

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(Parameterized::class)
class IsolationTests(
    private val transition: Transition,
    private val initialStateRepresentation: StateRepresentation,
    private val source: Source,
    @Suppress("unused") private val testName: String
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}_{3}")
        fun generateParameters(): Iterable<Array<Any>> {
            val testSingleTransition = false // Adjust this to manually test a single transition

            if (testSingleTransition) {
                return getTransitionsForManualTest()
            } else {
                val parameters = mutableListOf<Array<Any>>()
                val representationProvider = AllStateRepresentations()
                val rules = IsolationRuleLoader().loadIsolationRules()
                rules.transitions.forEach { transition ->
                    representationProvider.getStateRepresentations(transition.initialState, transition.event)
                        .forEach { initialStateRepresentation ->
                            parameters.add(
                                // Create one test for every combination of transitions and representations
                                createTestParameters(transition, initialStateRepresentation, rules.source)
                            )
                        }
                }
                return parameters
            }
        }

        private fun getTransitionsForManualTest(): Iterable<Array<Any>> {
            val initialState = State(
                contact = ContactCaseState.noIsolation,
                symptomatic = SymptomaticCaseState.noIsolation,
                positiveTest = PositiveTestCaseState.notIsolatingAndHasNegativeTest
            )
            val event =
                Event.receivedUnconfirmedPositiveTestWithEndDateNDaysOlderThanRememberedNegativeTestEndDateAndOlderThanAssumedSymptomOnsetDayIfAny
            val finalState = State(
                contact = ContactCaseState.noIsolation,
                symptomatic = SymptomaticCaseState.noIsolation,
                positiveTest = PositiveTestCaseState.isolatingWithUnconfirmedTest
            )
            val representation4_10 = StateStorage4_10Representation(initialState, event)

            val transition = Transition(initialState, event, finalState)
            return listOf(
                createTestParameters(transition, representation4_10),
            )
        }

        private fun createTestParameters(
            transition: Transition,
            initialStateRepresentation: StateRepresentation,
            source: Source = Source("", "")
        ) = arrayOf(
            transition,
            initialStateRepresentation,
            source,
            "Contact.${transition.initialState.contact}_Symptomatic.${transition.initialState.symptomatic}_PositiveTest.${transition.initialState.positiveTest}_Event.${transition.event}_v${initialStateRepresentation.representationName}"
        )
    }

    private val isolationTestContext = IsolationTestContext()
    private val isolationConfiguration = DurationDays()
    private val stateVerifier = StateVerifier(isolationTestContext)
    private val eventHandler = EventHandler(isolationTestContext, isolationConfiguration)

    @Before
    fun setUp() {
        isolationTestContext.setLocalAuthority(IsolationTestContext.ENGLISH_LOCAL_AUTHORITY)
        isolationTestContext.clock.currentInstant = LocalDate.of(2020, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
    }

    @After
    fun tearDown() {
        isolationTestContext.clock.reset()
    }

    @Test
    fun testTransition() {
        runBlocking {
            skipUnsupportedTransition(transition)
            initialStateRepresentation.skipUnsupportedState(transition.initialState)
            initialStateRepresentation.skipUnsupportedState(transition.finalState)
            initialStateRepresentation.skipUnsupportedEvent(transition.event)

            try {
                initialStateRepresentation.setupState(isolationTestContext, isolationConfiguration)
                stateVerifier.verifyState(transition.initialState)
                eventHandler.handleEvent(transition.event)
                stateVerifier.verifyState(transition.finalState)
            } catch (e: AssertionError) {
                throw AssertionError(errorMessage(), e)
            }
        }
    }

    // In order to print the full url to a rule definition, add the global gradle property "isolationModel.repo=http://url-to-repo..."
    private fun errorMessage() =
        """
        
        Error testing transition (represented by ${initialStateRepresentation::class.simpleName})
        
        Initial:   ${transition.initialState}
        Event:     ${transition.event}
        Expected:  ${transition.finalState}
        Reference: ${System.getProperty("isolationModel.repo") ?: "https://isolationModel.repo"}/blob/${source.commit}/${source.referenceBasePath}/${transition.reference!!.file}#L${transition.reference.line}

        ContactCase:    ${isolationTestContext.getCurrentState().contactCase}
        
        IndexInfo:      ${isolationTestContext.getCurrentState().indexInfo}
                
        Clock:          ${isolationTestContext.clock.currentInstant}
        Configuration:  ${isolationTestContext.getCurrentState().isolationConfiguration}

        """.trimIndent()

    private fun skipUnsupportedTransition(transition: Transition) {
        Assume.assumeFalse(
            "Skipping unsupported transition",
            transitionsToSkip.any {
                it.initialState == transition.initialState &&
                    it.event == transition.event &&
                    it.finalState == transition.finalState
            }
        )
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
