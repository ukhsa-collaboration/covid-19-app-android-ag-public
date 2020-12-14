package uk.nhs.nhsx.covid19.android.app.payment

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.payment.RedirectToIsolationPaymentWebsiteViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationPaymentUrlRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationPaymentUrlResponse
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import java.time.Instant
import java.time.LocalDate

class RedirectToIsolationPaymentWebsiteViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val requestIsolationPaymentUrl = mockk<RequestIsolationPaymentUrl>(relaxed = true)
    private val isolationPaymentTokenStateProvider = mockk<IsolationPaymentTokenStateProvider>(relaxed = true)
    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val loadPaymentUrlResultObserver = mockk<Observer<ViewState>>(relaxed = true)

    private val testSubject = RedirectToIsolationPaymentWebsiteViewModel(
        requestIsolationPaymentUrl,
        isolationPaymentTokenStateProvider,
        isolationStateMachine
    )

    private val isolationStateContactCase = State.Isolation(
        isolationStart = Instant.now(),
        isolationConfiguration = DurationDays(),
        contactCase = ContactCase(
            startDate = Instant.parse("2020-05-14T10:32:12Z"),
            notificationDate = Instant.parse("2020-05-15T11:42:32Z"),
            expiryDate = LocalDate.of(2020, 5, 21)
        )
    )

    @Test
    fun `load isolation payment returns success`() = runBlocking {
        testSubject.fetchWebsiteUrl().observeForever(loadPaymentUrlResultObserver)

        every { isolationPaymentTokenStateProvider.tokenState } returns IsolationPaymentTokenState.Token("abc")
        coEvery {
            requestIsolationPaymentUrl.invoke(
                IsolationPaymentUrlRequest(
                    "abc",
                    Instant.parse("2020-05-14T00:00:00Z"),
                    Instant.parse("2020-05-21T00:00:00Z")
                )
            )
        } returns Success(
            IsolationPaymentUrlResponse(websiteUrlWithQuery = "https://website/abc")
        )
        every { isolationStateMachine.readState() } returns isolationStateContactCase

        testSubject.loadIsolationPaymentUrl()

        verifyOrder {
            loadPaymentUrlResultObserver.onChanged(ViewState.Loading)
            loadPaymentUrlResultObserver.onChanged(
                ViewState.Success("https://website/abc")
            )
        }
    }

    @Test
    fun `load isolation payment returns failure`() = runBlocking {
        testSubject.fetchWebsiteUrl().observeForever(loadPaymentUrlResultObserver)

        val testException = Exception("Test error")

        coEvery { requestIsolationPaymentUrl.invoke(any()) } returns Failure(testException)

        testSubject.loadIsolationPaymentUrl()

        verifyOrder {
            loadPaymentUrlResultObserver.onChanged(ViewState.Loading)
            loadPaymentUrlResultObserver.onChanged(ViewState.Error)
        }
    }
}
