package uk.nhs.nhsx.covid19.android.app.isolation

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import kotlin.test.assertFails

/**
 * This test checks if, when setting up one state, the verification fails for all other possible initial states.
 * This ensures the quality of the verification logic.
 */
@RunWith(Parameterized::class)
class TransitionVerificationTest(
    private val initialState: State,
    private val otherStates: List<State>,
    private val initialStateRepresentation: StateRepresentation,
    @Suppress("unused") private val testName: String
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}_{3}")
        fun generateParameters(): Iterable<Array<Any>> {
            val initialStates = IsolationRuleLoader()
                .loadIsolationRules().transitions
                .map { it.initialState }
                .distinct()

            val parameters = mutableListOf<Array<Any>>()
            val representationProvider = AllStateRepresentations()
            initialStates.forEach { initialState ->
                representationProvider.getStateRepresentations(initialState)
                    .forEach { initialStateRepresentation ->
                        parameters.add(
                            createTestParameters(
                                initialState = initialState,
                                otherStates = initialStates.filter { it != initialState },
                                initialStateRepresentation = initialStateRepresentation
                            )
                        )
                    }
            }
            return parameters
        }

        private fun createTestParameters(
            initialState: State,
            otherStates: List<State>,
            initialStateRepresentation: StateRepresentation
        ) = arrayOf(
            initialState,
            otherStates,
            initialStateRepresentation,
            "v${initialStateRepresentation.representationName}_C${initialState.contact}_S${initialState.symptomatic}_P${initialState.positiveTest}"
                .take(160)
        )
    }

    private val testAppContext = IsolationTestContext()
    private val isolationConfiguration = DurationDays()
    private val stateVerifier = StateVerifier(testAppContext)

    @Test
    fun testVerification() {
        runBlocking {
            initialStateRepresentation.skipUnsupportedState(initialState)

            try {
                initialStateRepresentation.setupState(testAppContext, isolationConfiguration)
                stateVerifier.verifyState(initialState)
                otherStates.forEach {
                    assertFails("Verification succeeded incorrectly for: $it") {
                        stateVerifier.verifyState(it)
                    }
                }
            } catch (e: AssertionError) {
                throw AssertionError(errorMessage(), e)
            }
        }
    }

    private fun errorMessage() =
        """
        
        Error testing verification (represented by ${initialStateRepresentation::class.simpleName})
        
        Initial state:
            $initialState
        
        Current isolation state:
            ${testAppContext.getCurrentState()}
            
        Current contact case:
            ${testAppContext.getCurrentState().contactCase}
            
        Current index info:
            ${testAppContext.getCurrentState().indexInfo}
        
        """.trimIndent()
}
