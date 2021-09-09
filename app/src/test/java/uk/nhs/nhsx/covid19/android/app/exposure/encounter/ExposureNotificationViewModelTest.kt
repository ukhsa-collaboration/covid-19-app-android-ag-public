package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ExposureNotificationViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val getRiskyContactEncounterDate = mockk<GetRiskyContactEncounterDate>()
    private val isolationStateMachine = mockk<IsolationStateMachine>()
    private val fixedClock = Clock.fixed(Instant.parse("2020-01-01T10:00:00Z"), ZoneOffset.UTC)

    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)
    private val finishActivityObserver = mockk<Observer<Void>>(relaxUnitFun = true)

    private val testSubject = ExposureNotificationViewModel(
        getRiskyContactEncounterDate,
        isolationStateMachine,
        fixedClock
    )

    @Before
    fun setUp() {
        testSubject.viewState.observeForever(viewStateObserver)
        testSubject.finishActivity.observeForever(finishActivityObserver)
    }

    @Test
    fun `when encounter date is present and not in active index case isolation then update view state to show isolation and testing advice`() {
        val expectedEncounterDate: LocalDate = mockk(relaxUnitFun = true)
        every { getRiskyContactEncounterDate() } returns expectedEncounterDate
        every { isolationStateMachine.readLogicalState().isActiveIndexCase(fixedClock) } returns false

        testSubject.updateViewState()

        val expectedViewState = ViewState(encounterDate = expectedEncounterDate, shouldShowTestingAndIsolationAdvice = true)
        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `when encounter date is present and in active index case isolation then update view state to hide isolation and testing advice`() {
        val expectedEncounterDate: LocalDate = mockk(relaxUnitFun = true)
        every { getRiskyContactEncounterDate() } returns expectedEncounterDate
        every { isolationStateMachine.readLogicalState().isActiveIndexCase(fixedClock) } returns true

        testSubject.updateViewState()

        val expectedViewState = ViewState(encounterDate = expectedEncounterDate, shouldShowTestingAndIsolationAdvice = false)
        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `finishes activity when encounter date not present`() {
        every { getRiskyContactEncounterDate() } returns null

        testSubject.updateViewState()

        verify { finishActivityObserver.onChanged(null) }
    }
}
