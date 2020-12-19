package uk.nhs.nhsx.covid19.android.app.state

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModelTest
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class IsolationExpirationViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val stateMachine = mockk<IsolationStateMachine>(relaxed = true)

    private val fixedClock = Clock.fixed(Instant.parse("2020-09-01T10:00:00Z"), ZoneOffset.UTC)

    private val viewStateObserver = mockk<Observer<IsolationExpirationViewModel.ViewState>>(relaxed = true)

    private val testSubject = IsolationExpirationViewModel(
        stateMachine,
        fixedClock
    )

    private val isolationStateIndexCase = Isolation(
        isolationStart = symptomsOnsetDate.atStartOfDay(ZoneOffset.UTC).toInstant(),
        isolationConfiguration = DurationDays(),
        indexCase = IndexCase(
            symptomsOnsetDate = LocalDate.now(),
            expiryDate = TestResultViewModelTest.symptomsOnsetDate.plus(7, ChronoUnit.DAYS),
            selfAssessment = true
        )
    )

    private val isolationStateNotInIndexCase = Isolation(
        isolationStart = symptomsOnsetDate.atStartOfDay(ZoneOffset.UTC).toInstant(),
        isolationConfiguration = DurationDays()
    )

    @Test
    fun `non-parsable isolation expiry date string provided`() = runBlocking {
        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.checkState("non-parsable")

        verify(exactly = 0) { viewStateObserver.onChanged(any()) }
    }

    @Test
    fun `check after now in index case and show temperature notice`() = runBlocking {
        every { stateMachine.readState() } returns isolationStateIndexCase

        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.checkState("2020-09-02")

        val expired = false
        val expiryDate = LocalDate.parse("2020-09-02")
        val showTemperatureNotice = true

        verify { viewStateObserver.onChanged(IsolationExpirationViewModel.ViewState(expired, expiryDate, showTemperatureNotice)) }
    }

    @Test
    fun `check before now in index case and show temperature notice`() = runBlocking {
        every { stateMachine.readState() } returns isolationStateIndexCase

        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.checkState("2020-08-31")

        val expired = true
        val expiryDate = LocalDate.parse("2020-08-31")
        val showTemperatureNotice = true

        verify { viewStateObserver.onChanged(IsolationExpirationViewModel.ViewState(expired, expiryDate, showTemperatureNotice)) }
    }

    @Test
    fun `check after now not in index case and don't show temperature notice`() = runBlocking {
        every { stateMachine.readState() } returns isolationStateNotInIndexCase

        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.checkState("2020-09-02")

        val expired = false
        val expiryDate = LocalDate.parse("2020-09-02")
        val showTemperatureNotice = false

        verify { viewStateObserver.onChanged(IsolationExpirationViewModel.ViewState(expired, expiryDate, showTemperatureNotice)) }
    }

    @Test
    fun `check after now not in isolation and don't show temperature notice`() = runBlocking {
        every { stateMachine.readState() } returns Default()

        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.checkState("2020-09-02")

        val expired = false
        val expiryDate = LocalDate.parse("2020-09-02")
        val showTemperatureNotice = false

        verify { viewStateObserver.onChanged(IsolationExpirationViewModel.ViewState(expired, expiryDate, showTemperatureNotice)) }
    }

    companion object {
        val symptomsOnsetDate = LocalDate.parse("2020-08-20")!!
    }
}
