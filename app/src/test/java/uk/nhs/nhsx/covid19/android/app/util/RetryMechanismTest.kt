package uk.nhs.nhsx.covid19.android.app.util

import io.mockk.spyk
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertEquals

class RetryMechanismTest {
    @Test
    fun `don't retry on success`() {
        val function: () -> Unit = {}
        val spyFunction = spyk(function)

        RetryMechanism.retryWithBackOff(maxTotalDelay = 100, action = spyFunction)

        verify(exactly = 1) { spyFunction.invoke() }
    }

    @Test
    fun `retry on exception`() {
        val exception = Exception()
        val function: () -> Unit = {
            throw exception
        }
        val spyFunction = spyk(function)

        val result = runCatching {
            RetryMechanism.retryWithBackOff(maxTotalDelay = 100, action = spyFunction)
        }

        verify(atLeast = 2) { spyFunction.invoke() }
        assertEquals(Result.failure(exception), result)
    }
}
