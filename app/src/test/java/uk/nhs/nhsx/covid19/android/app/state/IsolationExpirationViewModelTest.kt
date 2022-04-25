package uk.nhs.nhsx.covid19.android.app.state

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit.DAYS

class IsolationExpirationViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val stateMachine = mockk<IsolationStateMachine>(relaxUnitFun = true)

    private val fixedClock = Clock.fixed(Instant.parse("2020-09-01T10:00:00Z"), ZoneOffset.UTC)
    private val isolationHelper = IsolationLogicalHelper(fixedClock)
    private val localAuthorityPostCodeProvider = mockk<LocalAuthorityPostCodeProvider> {
        coEvery { requirePostCodeDistrict() } returns PostCodeDistrict.ENGLAND
    }

    private val viewStateObserver = mockk<Observer<IsolationExpirationViewModel.ViewState>>(relaxed = true)

    private val testSubject = IsolationExpirationViewModel(
        stateMachine,
        fixedClock,
        localAuthorityPostCodeProvider
    )

    private val isolationStateIndexCase = isolationHelper.selfAssessment(
        selfAssessmentDate = isolationStart,
        expiryDate = isolationStart.plus(7, DAYS)
    ).asIsolation()

    private val isolationStateContactCase = isolationHelper.contactCase(
        exposureDate = isolationStart,
        notificationDate = isolationStart,
        expiryDate = isolationStart.plus(7, DAYS),
    ).asIsolation()

    private val isolationStateExpiredIndexCase = isolationHelper.selfAssessment(
        selfAssessmentDate = isolationStart.minus(7, DAYS),
        expiryDate = isolationStart
    ).asIsolation()

    private val isolationStateExpiredContactCase = isolationHelper.contactCase(
        exposureDate = isolationStart,
        notificationDate = isolationStart.minus(7, DAYS),
        expiryDate = isolationStart
    ).asIsolation()

    @Test
    fun `non-parsable isolation expiry date string provided`() = runBlocking {
        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.checkState("non-parsable")

        verify(exactly = 0) { viewStateObserver.onChanged(any()) }
    }

    @Test
    fun `check after now in index case and show temperature notice`() = runBlocking {
        every { stateMachine.readLogicalState() } returns isolationStateIndexCase

        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.checkState("2020-09-02")

        val expired = false
        val expiryDate = LocalDate.parse("2020-09-02")
        val showTemperatureNotice = true
        val isActiveOrPreviousIndexCase = false

        verify { viewStateObserver.onChanged(IsolationExpirationViewModel.ViewState(expired, expiryDate, showTemperatureNotice, isActiveOrPreviousIndexCase)) }
    }

    @Test
    fun `check before now in index case and show temperature notice`() = runBlocking {
        every { stateMachine.readLogicalState() } returns isolationStateIndexCase

        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.checkState("2020-08-31")

        val expired = true
        val expiryDate = LocalDate.parse("2020-08-31")
        val showTemperatureNotice = true
        val isActiveOrPreviousIndexCase = false

        verify { viewStateObserver.onChanged(IsolationExpirationViewModel.ViewState(expired, expiryDate, showTemperatureNotice, isActiveOrPreviousIndexCase)) }
    }

    @Test
    fun `check after now not in index case and don't show temperature notice`() = runBlocking {
        every { stateMachine.readLogicalState() } returns isolationStateContactCase

        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.checkState("2020-09-02")

        val expired = false
        val expiryDate = LocalDate.parse("2020-09-02")
        val showTemperatureNotice = false
        val isActiveOrPreviousIndexCase = false

        verify { viewStateObserver.onChanged(IsolationExpirationViewModel.ViewState(expired, expiryDate, showTemperatureNotice, isActiveOrPreviousIndexCase)) }
    }

    @Test
    fun `check before now not in index case and don't show temperature notice`() = runBlocking {
        every { stateMachine.readLogicalState() } returns isolationStateContactCase

        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.checkState("2020-08-31")

        val expired = true
        val expiryDate = LocalDate.parse("2020-08-31")
        val showTemperatureNotice = false
        val isActiveOrPreviousIndexCase = false

        verify { viewStateObserver.onChanged(IsolationExpirationViewModel.ViewState(expired, expiryDate, showTemperatureNotice, isActiveOrPreviousIndexCase)) }
    }

    @Test
    fun `check after now not in isolation and don't show temperature notice`() = runBlocking {
        every { stateMachine.readLogicalState() } returns isolationHelper.neverInIsolation()

        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.checkState("2020-09-02")

        val expired = false
        val expiryDate = LocalDate.parse("2020-09-02")
        val showTemperatureNotice = false
        val isActiveOrPreviousIndexCase = false

        verify { viewStateObserver.onChanged(IsolationExpirationViewModel.ViewState(expired, expiryDate, showTemperatureNotice, isActiveOrPreviousIndexCase)) }
    }

    @Test
    fun `when acknowledging isolation expiration, notify state machine`() {
        testSubject.acknowledgeIsolationExpiration()

        verify { stateMachine.acknowledgeIsolationExpiration() }
    }

    @Test
    fun `when active index case in Wales set isActiveOrPreviousIndexCase to true`() {
        every { stateMachine.readLogicalState() } returns isolationStateIndexCase
        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns WALES

        verifyViewState(expired = false, expiryDate = "2020-09-02", showTemperatureNotice = true, isActiveOrPreviousIndexCase = true)
    }

    @Test
    fun `when active index case england set isActiveOrPreviousIndexCase to false`() {
        every { stateMachine.readLogicalState() } returns isolationStateIndexCase

        verifyViewState(expired = false, expiryDate = "2020-09-02", showTemperatureNotice = true, isActiveOrPreviousIndexCase = false)
    }

    @Test
    fun `when active contact case wales set isActiveOrPreviousIndexCase to false`() {
        every { stateMachine.readLogicalState() } returns isolationStateContactCase
        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns WALES

        verifyViewState(expired = false, expiryDate = "2020-09-02", showTemperatureNotice = false, isActiveOrPreviousIndexCase = false)
    }

    @Test
    fun `when active contact case england set isActiveOrPreviousIndexCase to false`() {
        every { stateMachine.readLogicalState() } returns isolationStateContactCase

        verifyViewState(expired = false, expiryDate = "2020-09-02", showTemperatureNotice = false, isActiveOrPreviousIndexCase = false)
    }

    @Test
    fun `when expired index case wales set isActiveOrPreviousIndexCase to true`() {
        every { stateMachine.readLogicalState() } returns isolationStateExpiredIndexCase
        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns WALES

        verifyViewState(expired = true, expiryDate = "2020-08-30", showTemperatureNotice = true, isActiveOrPreviousIndexCase = true)
    }

    @Test
    fun `when expired index case england set isActiveOrPreviousIndexCase to false`() {
        every { stateMachine.readLogicalState() } returns isolationStateExpiredIndexCase

        verifyViewState(expired = true, expiryDate = "2020-08-30", showTemperatureNotice = true, isActiveOrPreviousIndexCase = false)
    }

    @Test
    fun `when expired contact case wales set isActiveOrPreviousIndexCase to false`() {
        every { stateMachine.readLogicalState() } returns isolationStateExpiredContactCase
        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns WALES

        verifyViewState(expired = true, expiryDate = "2020-08-30", showTemperatureNotice = false, isActiveOrPreviousIndexCase = false)
    }

    @Test
    fun `when expired contact case england set isActiveOrPreviousIndexCase to false`() {
        every { stateMachine.readLogicalState() } returns isolationStateExpiredContactCase
        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns PostCodeDistrict.ENGLAND

        verifyViewState(expired = true, expiryDate = "2020-08-30", showTemperatureNotice = false, isActiveOrPreviousIndexCase = false)
    }

    private fun verifyViewState(expired: Boolean, expiryDate: String, showTemperatureNotice: Boolean, isActiveOrPreviousIndexCase: Boolean) {
        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.checkState(expiryDate)

        verify { viewStateObserver.onChanged(IsolationExpirationViewModel.ViewState(expired, LocalDate.parse(expiryDate), showTemperatureNotice, isActiveOrPreviousIndexCase)) }
    }

    companion object {
        private val isolationStart = LocalDate.parse("2020-08-30")!!
    }
}
