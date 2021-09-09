package uk.nhs.nhsx.covid19.android.app.status.isolationhub

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedIsolationPaymentsButton
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.payment.CanClaimIsolationPayment
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Disabled
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Token
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Unresolved
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenStateProvider
import uk.nhs.nhsx.covid19.android.app.status.isolationhub.IsolationHubViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.status.isolationhub.IsolationHubViewModel.NavigationTarget.BookTest
import uk.nhs.nhsx.covid19.android.app.status.isolationhub.IsolationHubViewModel.NavigationTarget.IsolationPayment
import uk.nhs.nhsx.covid19.android.app.status.isolationhub.IsolationHubViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.status.testinghub.CanBookPcrTest
import uk.nhs.nhsx.covid19.android.app.status.testinghub.EvaluateBookTestNavigation

class IsolationHubViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val isolationPaymentTokenStateProvider = mockk<IsolationPaymentTokenStateProvider>(relaxUnitFun = true)
    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxUnitFun = true)
    private val canBookPcrTest = mockk<CanBookPcrTest>()
    private val canClaimIsolationPayment = mockk<CanClaimIsolationPayment>()
    private val evaluateBookTestNavigation = mockk<EvaluateBookTestNavigation>()
    private val notificationProvider = mockk<NotificationProvider>(relaxUnitFun = true)
    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)
    private val navigationTargetObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)

    private lateinit var testSubject: IsolationHubViewModel

    @Before
    fun setUp() {
        every { canClaimIsolationPayment() } returns false
        every { isolationPaymentTokenStateProvider.tokenState } returns Unresolved
        coEvery { canBookPcrTest() } returns false
    }

    @Test
    fun `cancel isolation hub reminder notification on create`() {
        createTestSubjectAndStartListeningToLiveData()

        testSubject.onCreate()

        verify { notificationProvider.cancelIsolationHubReminderNotification() }
    }

    @Test
    fun `isolation payment button is displayed when user can claim isolation payment and token state is Token`() {
        every { canClaimIsolationPayment() } returns true
        every { isolationPaymentTokenStateProvider.tokenState } returns Token("token")

        createTestSubjectAndStartListeningToLiveData()

        val expectedViewState = ViewState(showIsolationPaymentButton = true, showBookTestButton = false)

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `isolation payment button is not displayed when user can not claim isolation payment`() {
        every { canClaimIsolationPayment() } returns false
        every { isolationPaymentTokenStateProvider.tokenState } returns Token("token")

        createTestSubjectAndStartListeningToLiveData()

        val expectedViewState = ViewState(showIsolationPaymentButton = false, showBookTestButton = false)

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `isolation payment button is not displayed when token state is Disabled`() {
        every { canClaimIsolationPayment() } returns true
        every { isolationPaymentTokenStateProvider.tokenState } returns Disabled

        createTestSubjectAndStartListeningToLiveData()

        val expectedViewState = ViewState(showIsolationPaymentButton = false, showBookTestButton = false)

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `book a test button is displayed when user can book a PCR test`() {
        coEvery { canBookPcrTest() } returns true

        createTestSubjectAndStartListeningToLiveData()

        val expectedViewState = ViewState(showIsolationPaymentButton = false, showBookTestButton = true)

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `book a test button is not displayed when user can not book a PCR test`() {
        coEvery { canBookPcrTest() } returns false

        createTestSubjectAndStartListeningToLiveData()

        val expectedViewState = ViewState(showIsolationPaymentButton = false, showBookTestButton = false)

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `when isolation payment clicked then track event and emit navigation event`() {
        createTestSubjectAndStartListeningToLiveData()

        testSubject.onItemIsolationPaymentClicked()

        verifyOrder {
            analyticsEventProcessor.track(SelectedIsolationPaymentsButton)
            navigationTargetObserver.onChanged(IsolationPayment)
        }
    }

    @Test
    fun `when book a test clicked then emit navigation event returned by EvaluateBookTestNavigation`() {
        createTestSubjectAndStartListeningToLiveData()

        val expectedNavigationTarget = EvaluateBookTestNavigation.NavigationTarget.BookPcrTest
        every { evaluateBookTestNavigation() } returns expectedNavigationTarget

        testSubject.onItemBookTestClicked()

        verify { navigationTargetObserver.onChanged(BookTest(navigationTarget = expectedNavigationTarget)) }
    }

    private fun createTestSubjectAndStartListeningToLiveData() {
        testSubject = IsolationHubViewModel(
            isolationPaymentTokenStateProvider,
            analyticsEventProcessor,
            canBookPcrTest,
            canClaimIsolationPayment,
            evaluateBookTestNavigation,
            notificationProvider
        )
        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigationTarget().observeForever(navigationTargetObserver)
    }
}