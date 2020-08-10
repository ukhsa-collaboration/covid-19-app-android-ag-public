package uk.nhs.nhsx.covid19.android.app.testordering

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Lce
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestOrderResponse
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class TestOrderingProgressViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val loadVirologyTestOrder = mockk<LoadVirologyTestOrder>()
    private val loadVirologyTestOrderResultObserver =
        mockk<Observer<Lce<String>>>(relaxed = true)
    private val testOrderTokensProvider = mockk<TestOrderingTokensProvider>(relaxed = true)
    private val clock = Clock.fixed(Instant.parse("2020-07-21T10:00:00Z"), ZoneId.systemDefault())

    private val testSubject =
        TestOrderingProgressViewModel(
            loadVirologyTestOrder,
            testOrderTokensProvider,
            clock
        )

    @Test
    fun `load virology test order returns success`() = runBlocking {
        testSubject.websiteUrlWithQuery().observeForever(loadVirologyTestOrderResultObserver)

        coEvery { loadVirologyTestOrder.invoke() } returns Success(
            VirologyTestOrderResponse(
                websiteUrlWithQuery = "https://a.b/c&d=e",
                tokenParameterValue = "e",
                testResultPollingToken = "f",
                diagnosisKeySubmissionToken = "g"
            )
        )

        testSubject.loadVirologyTestOrder()

        verify {
            testOrderTokensProvider.add(TestOrderPollingConfig(Instant.now(clock), "f", "g"))
        }

        verifyOrder {
            loadVirologyTestOrderResultObserver.onChanged(Lce.Loading)
            loadVirologyTestOrderResultObserver.onChanged(
                Lce.Success("https://a.b/c&d=e")
            )
        }
    }

    @Test
    fun `load virology test order returns failure`() = runBlocking {
        testSubject.websiteUrlWithQuery().observeForever(loadVirologyTestOrderResultObserver)

        val testException = Exception("Test error")

        coEvery { loadVirologyTestOrder.invoke() } returns Failure(testException)

        testSubject.loadVirologyTestOrder()

        verifyOrder {
            loadVirologyTestOrderResultObserver.onChanged(Lce.Loading)
            loadVirologyTestOrderResultObserver.onChanged(Lce.Error(testException))
        }
    }
}
