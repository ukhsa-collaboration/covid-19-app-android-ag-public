package uk.nhs.nhsx.covid19.android.app.util

import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.NeverIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsolationChecker(private val testAppContext: TestApplicationContext) {

    fun assertNeverIsolating() {
        assertTrue(testAppContext.getCurrentLogicalState() is NeverIsolating)
    }

    fun assertActiveIndexAndContact() {
        val state = testAppContext.getCurrentLogicalState()
        assertTrue(state is PossiblyIsolating)
        assertTrue(state.isActiveIndexCase(testAppContext.clock))
        assertTrue(state.isActiveContactCase(testAppContext.clock))
    }

    fun assertActiveIndexNoContact() {
        val state = testAppContext.getCurrentLogicalState()
        assertTrue(state is PossiblyIsolating)
        assertTrue(state.isActiveIndexCase(testAppContext.clock))
        assertFalse(state.remembersContactCase())
    }

    fun assertExpiredIndexNoContact() {
        val state = testAppContext.getCurrentLogicalState()
        assertTrue(state is PossiblyIsolating)
        assertTrue(state.remembersIndexCase())
        assertFalse(state.isActiveIndexCase(testAppContext.clock))
        assertFalse(state.remembersContactCase())
    }

    fun assertActiveContactNoIndex() {
        val state = testAppContext.getCurrentLogicalState()
        assertTrue(state is PossiblyIsolating)
        assertTrue(state.isActiveContactCase(testAppContext.clock))
        assertFalse(state.remembersIndexCase())
    }

    fun assertExpiredContactNoIndex() {
        val state = testAppContext.getCurrentLogicalState()
        assertTrue(state is PossiblyIsolating)
        assertTrue(state.remembersContactCase())
        assertFalse(state.isActiveContactCase(testAppContext.clock))
        assertFalse(state.remembersIndexCase())
    }

    fun assertExpiredContactAndIndex() {
        val state = testAppContext.getCurrentLogicalState()
        assertTrue(state is PossiblyIsolating)
        assertTrue(state.remembersBothCases())
    }
}
