package uk.nhs.nhsx.covid19.android.app.isolation

import uk.nhs.nhsx.covid19.android.app.isolation.IsolationState.ACTIVE
import uk.nhs.nhsx.covid19.android.app.isolation.IsolationState.FINISHED
import uk.nhs.nhsx.covid19.android.app.isolation.IsolationState.NONE
import uk.nhs.nhsx.covid19.android.app.isolation.TestType.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.isolation.TestType.POSITIVE_CONFIRMED
import uk.nhs.nhsx.covid19.android.app.isolation.TestType.POSITIVE_UNCONFIRMED
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.NeverIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

typealias IsolationMachineState = uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState

class StateVerifier(
    private val isolationTestContext: IsolationTestContext
) {
    fun verifyState(state: State) {
        verifyIsolationState(state)
        verifyTestResult(state)
    }

    private fun verifyIsolationState(expectedState: State) {
        val logicalState = isolationTestContext.getCurrentLogicalState()

        when (expectedState.strongestIsolationState) {
            ACTIVE -> {
                assertTrue(logicalState is PossiblyIsolating, "Expect logicalState to be PossiblyIsolating but was $logicalState")
                assertTrue(logicalState.isActiveIsolation(isolationTestContext.clock), "Assert is active isolation")
            }
            FINISHED -> {
                assertTrue(logicalState is PossiblyIsolating, "Expect logicalState to be PossiblyIsolating, but was $logicalState")
                assertFalse(
                    logicalState.isActiveIsolation(isolationTestContext.clock),
                    "Assert is not active isolation anymore"
                )
            }
            NONE -> assertTrue(logicalState is NeverIsolating, "Assert isolationState is Default")
        }

        val shouldHaveContactCase = expectedState.contact.isolationState != NONE
        assertEquals(shouldHaveContactCase, logicalState.hasContactCase(), "shouldHaveContactCase")

        val shouldHaveActiveContactCase = expectedState.contact.isolationState == ACTIVE
        assertEquals(shouldHaveActiveContactCase, logicalState.hasActiveContactCase(), "shouldHaveActiveContactCase")

        val shouldHaveBeenTerminatedEarly = expectedState.contact.terminatedEarly
        assertEquals(
            shouldHaveBeenTerminatedEarly,
            logicalState.contactCaseHasBeenTerminatedEarly(),
            "shouldHaveBeenTerminatedEarly"
        )

        val shouldHaveExpiredIndexCase = expectedState.combinedIndexIsolationState.contains(FINISHED) &&
            !expectedState.combinedIndexIsolationState.contains(ACTIVE)
        assertEquals(
            shouldHaveExpiredIndexCase,
            logicalState.hasExpiredIndexCase(),
            "shouldHaveExpiredIndexCase"
        )

        val shouldHaveSelfAssessmentIndexCase = expectedState.symptomatic.isolationState != NONE
        assertEquals(
            shouldHaveSelfAssessmentIndexCase,
            logicalState.hasSelfAssessmentIndexCase(),
            "shouldHaveSelfAssessmentIndexCase"
        )

        val shouldHaveNonSelfAssessmentIndexCase =
            expectedState.positiveTest.isolationState != NONE && !shouldHaveSelfAssessmentIndexCase
        assertEquals(
            shouldHaveNonSelfAssessmentIndexCase,
            logicalState.hasNonSelfAssessmentIndexCase(),
            "shouldHaveNonSelfAssessmentIndexCase"
        )
    }

    private fun verifyTestResult(state: State) {
        val acknowledgedTestResult = isolationTestContext.getCurrentState().testResult
        when (state.positiveTest.testType) {
            TestType.NONE -> assertNull(acknowledgedTestResult, "Expected no test result to be stored")
            NEGATIVE -> {
                assertNotNull(acknowledgedTestResult, "Expected test result is not stored")
                assertEquals(acknowledgedTestResult.testResult, RelevantVirologyTestResult.NEGATIVE, "Test result is not negative")
                assertTrue(acknowledgedTestResult.isConfirmed(), "Test result is not confirmed")
            }
            POSITIVE_CONFIRMED -> {
                assertNotNull(acknowledgedTestResult, "Expected test result is not stored")
                assertEquals(acknowledgedTestResult.testResult, POSITIVE, "Test result is not positive")
                assertTrue(acknowledgedTestResult.isConfirmed(), "Test result is not confirmed")
            }
            POSITIVE_UNCONFIRMED -> {
                assertNotNull(acknowledgedTestResult, "Expected test result is not stored")
                assertEquals(acknowledgedTestResult.testResult, POSITIVE, "Test result is not positive")
                assertFalse(acknowledgedTestResult.isConfirmed(), "Test result is not unconfirmed")
            }
        }
    }

    private fun IsolationMachineState.hasContactCase(): Boolean =
        when (this) {
            is PossiblyIsolating -> contactCase != null
            is NeverIsolating -> false
        }

    private fun IsolationMachineState.hasActiveContactCase(): Boolean =
        when (this) {
            is PossiblyIsolating -> contactCase?.hasExpired(isolationTestContext.clock) == false
            is NeverIsolating -> false
        }

    private fun IsolationMachineState.contactCaseHasBeenTerminatedEarly(): Boolean =
        when (this) {
            is PossiblyIsolating -> contactCase?.optOutOfContactIsolation?.date != null
            is NeverIsolating -> false
        }

    private fun IsolationMachineState.hasExpiredIndexCase(): Boolean =
        when (this) {
            is PossiblyIsolating -> {
                val indexCase = indexInfo as? IndexCase
                indexCase?.hasExpired(isolationTestContext.clock) ?: false
            }
            is NeverIsolating -> false
        }

    private fun IsolationMachineState.hasSelfAssessmentIndexCase(): Boolean =
        when (this) {
            is PossiblyIsolating -> {
                val indexCase = indexInfo as? IndexCase
                indexCase?.isSelfAssessment() ?: false
            }
            is NeverIsolating -> false
        }

    private fun IsolationMachineState.hasNonSelfAssessmentIndexCase(): Boolean =
        when (this) {
            is PossiblyIsolating -> indexInfo is IndexCase && !hasSelfAssessmentIndexCase()
            is NeverIsolating -> false
        }
}
