package uk.nhs.nhsx.covid19.android.app.common

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.EmptyApi

class SubmitEmptyDataTest {

    private val emptyApi = mockk<EmptyApi>(relaxed = true)
    private val obfuscationRateLimiter = mockk<RandomObfuscationRateLimiter>()
    private val testDispatcher = TestCoroutineDispatcher()
    private val testScope = TestCoroutineScope()

    private val testSubject = SubmitEmptyData(emptyApi, obfuscationRateLimiter, testScope, testDispatcher)

    @Test
    fun `when invoking empty data submission then empty api is called`() = testScope.runBlockingTest {
        every { obfuscationRateLimiter.allow } returns true
        testSubject.invoke()

        coVerify { emptyApi.submit() }
    }

    @Test
    fun `when invoking empty data submission then empty api is called 10 times`() = testScope.runBlockingTest {
        every { obfuscationRateLimiter.allow } returns true
        testSubject.invoke(10)

        coVerify(exactly = 10) { emptyApi.submit() }
    }

    @Test
    fun `when invoking empty data submission then empty api is not called`() = testScope.runBlockingTest {
        every { obfuscationRateLimiter.allow } returns false
        testSubject.invoke()

        coVerify(exactly = 0) { emptyApi.submit() }
    }

    @Test
    fun `when invoking empty data submission then empty api is not called 10 times`() = testScope.runBlockingTest {
        every { obfuscationRateLimiter.allow } returns false
        testSubject.invoke(10)

        coVerify(exactly = 0) { emptyApi.submit() }
    }
}
