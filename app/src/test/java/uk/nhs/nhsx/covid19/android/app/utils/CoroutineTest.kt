package uk.nhs.nhsx.covid19.android.app.utils

import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After

open class CoroutineTest {

    private val testDispatcher = TestCoroutineDispatcher()
    val testScope = TestCoroutineScope(testDispatcher)

    @After
    fun tearDown() {
        testScope.cleanupTestCoroutines()
    }

    fun runBlockingTest(function: suspend TestCoroutineScope.() -> Unit) {
        testDispatcher.runBlockingTest(function)
    }
}
