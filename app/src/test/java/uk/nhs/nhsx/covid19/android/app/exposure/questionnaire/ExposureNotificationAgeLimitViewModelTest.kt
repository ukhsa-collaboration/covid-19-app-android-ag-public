package uk.nhs.nhsx.covid19.android.app.exposure.questionnaire

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationAgeLimitViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationAgeLimitViewModel.NavigationTarget.Finish
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationAgeLimitViewModel.NavigationTarget.Review
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationAgeLimitViewModel.NavigationTarget.VaccinationStatus
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationAgeLimitViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.Clock
import java.time.LocalDate
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_1 as YES
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_2 as NO

class ExposureNotificationAgeLimitViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val mockAgeLimitBeforeEncounter: GetAgeLimitBeforeEncounter = mockk(relaxUnitFun = true)
    private val mockIsolationStateMachine: IsolationStateMachine = mockk()
    private val mockLogicalState: IsolationLogicalState = mockk()
    private val mockClock: Clock = mockk()

    private val testSubject = ExposureNotificationAgeLimitViewModel(
        mockAgeLimitBeforeEncounter,
        mockIsolationStateMachine,
        mockClock
    )

    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)
    private val navigationTargetObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)

    private val testDate = LocalDate.of(2021, 8, 4)

    @Before
    fun setUp() {
        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigationTarget().observeForever(navigationTargetObserver)

        coEvery { mockAgeLimitBeforeEncounter() } returns testDate
        coEvery { mockIsolationStateMachine.readLogicalState() } returns mockLogicalState
    }

    @Test
    fun `when not in index case, showSubtitle is true`() {
        every { mockLogicalState.isActiveIndexCase(mockClock) } returns false
        testSubject.updateViewState()

        val expectedState = ViewState(ageLimitSelection = null, testDate, hasError = false, showSubtitle = true)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when in index case, showSubtitle is false`() {
        every { mockLogicalState.isActiveIndexCase(mockClock) } returns true

        testSubject.updateViewState()

        val expectedState = ViewState(ageLimitSelection = null, testDate, hasError = false, showSubtitle = false)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when no option selected, error state set to true`() {
        every { mockLogicalState.isActiveIndexCase(mockClock) } returns false

        testSubject.onClickContinue()
        val expectedState = ViewState(ageLimitSelection = null, testDate, hasError = true, showSubtitle = true)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when has selected YES and continue clicked navigate to vaccination status`() {
        testSubject.onAgeLimitOptionChanged(YES)
        testSubject.onClickContinue()
        verify { navigationTargetObserver.onChanged(VaccinationStatus) }
    }

    @Test
    fun `when selected YES, ageLimitSelection is set to YES`() {
        every { mockLogicalState.isActiveIndexCase(mockClock) } returns true
        testSubject.onAgeLimitOptionChanged(YES)

        val expectedState = ViewState(ageLimitSelection = YES, testDate, hasError = false, showSubtitle = false)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when selected NO, ageLimitSelection is set to NO`() {
        every { mockLogicalState.isActiveIndexCase(mockClock) } returns true
        testSubject.onAgeLimitOptionChanged(NO)

        val expectedState = ViewState(ageLimitSelection = NO, testDate, hasError = false, showSubtitle = false)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when has selected NO and continue clicked navigate to review screen`() {
        testSubject.onAgeLimitOptionChanged(NO)
        testSubject.onClickContinue()
        verify { navigationTargetObserver.onChanged(Review) }
    }

    @Test
    fun `when get age limit returns null navigate to finish`() {
        coEvery { mockAgeLimitBeforeEncounter() } returns null

        testSubject.updateViewState()

        verify { navigationTargetObserver.onChanged(Finish) }
    }
}
