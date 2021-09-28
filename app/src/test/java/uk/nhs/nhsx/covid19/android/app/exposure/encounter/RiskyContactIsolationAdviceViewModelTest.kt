package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EvaluateTestingAdviceToShow.TestingAdviceToShow.Default
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EvaluateTestingAdviceToShow.TestingAdviceToShow.UnknownExposureDate
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra.FULLY_VACCINATED
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra.MEDICALLY_EXEMPT
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra.MINOR
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra.NONE
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.NavigationTarget.BookPcrTest
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.NavigationTarget.Home
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.ViewState.AlreadyIsolating
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.ViewState.NewlyIsolating
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.ViewState.NotIsolatingAsFullyVaccinated
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.ViewState.NotIsolatingAsMedicallyExempt
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.ViewState.NotIsolatingAsMinor
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RiskyContactIsolationAdviceViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)
    private val navigationTargetObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)
    private val isolationLogicalState = mockk<IsolationLogicalState>()
    private val isolationStateMachine = mockk<IsolationStateMachine>()
    private val evaluateTestingAdviceToShow = mockk<EvaluateTestingAdviceToShow>()
    private val fixedClock = Clock.fixed(Instant.parse("2020-01-01T10:00:00Z"), ZoneOffset.UTC)
    private val expectedDaysInIsolation = 5L
    private val expectedTestingAdviceToShow = Default

    @Before
    fun setUp() {
        every { isolationStateMachine.readLogicalState() } returns isolationLogicalState
        every { isolationStateMachine.remainingDaysInIsolation(isolationLogicalState) } returns expectedDaysInIsolation
        every { isolationLogicalState.isActiveIndexCase(fixedClock) } returns false
        coEvery { evaluateTestingAdviceToShow(fixedClock) } returns expectedTestingAdviceToShow
    }

    @Test
    fun `when in active index case, and receive NONE, then emit AlreadyIsolating`() {
        every { isolationLogicalState.isActiveIndexCase(fixedClock) } returns true

        createTestSubject(optOutOfContactIsolationExtra = NONE)

        verify {
            viewStateObserver.onChanged(AlreadyIsolating(expectedDaysInIsolation.toInt(), expectedTestingAdviceToShow))
        }
    }

    @Test
    fun `when in active index case, and receive FULLY_VACCINATED, then emit AlreadyIsolating`() {
        every { isolationLogicalState.isActiveIndexCase(fixedClock) } returns true

        createTestSubject(optOutOfContactIsolationExtra = FULLY_VACCINATED)

        verify {
            viewStateObserver.onChanged(AlreadyIsolating(expectedDaysInIsolation.toInt(), expectedTestingAdviceToShow))
        }
    }

    @Test
    fun `when in active index case, and receive MINOR, then emit AlreadyIsolating`() {
        every { isolationLogicalState.isActiveIndexCase(fixedClock) } returns true

        createTestSubject(optOutOfContactIsolationExtra = MINOR)

        verify {
            viewStateObserver.onChanged(AlreadyIsolating(expectedDaysInIsolation.toInt(), expectedTestingAdviceToShow))
        }
    }

    @Test
    fun `when in active index case, and receive MEDICALLY_EXEMPT, then emit AlreadyIsolating`() {
        every { isolationLogicalState.isActiveIndexCase(fixedClock) } returns true

        createTestSubject(optOutOfContactIsolationExtra = MEDICALLY_EXEMPT)

        verify {
            viewStateObserver.onChanged(AlreadyIsolating(expectedDaysInIsolation.toInt(), expectedTestingAdviceToShow))
        }
    }

    @Test
    fun `when in not active index case then emit NewlyIsolating`() {
        every { isolationLogicalState.isActiveIndexCase(fixedClock) } returns false

        createTestSubject(optOutOfContactIsolationExtra = NONE)

        verify {
            viewStateObserver.onChanged(NewlyIsolating(expectedDaysInIsolation.toInt(), expectedTestingAdviceToShow))
        }
    }

    @Test
    fun `when minor then emit NotIsolatingAsMinor`() {
        createTestSubject(optOutOfContactIsolationExtra = MINOR)

        verify { viewStateObserver.onChanged(NotIsolatingAsMinor(expectedTestingAdviceToShow)) }
    }

    @Test
    fun `when fully vaccinated then emit NotIsolatingAsFullyVaccinated`() {
        createTestSubject(optOutOfContactIsolationExtra = FULLY_VACCINATED)

        verify { viewStateObserver.onChanged(NotIsolatingAsFullyVaccinated(expectedTestingAdviceToShow)) }
    }

    @Test
    fun `when medically exempt then emit NotIsolatingAsMedicallyExempt`() {
        createTestSubject(optOutOfContactIsolationExtra = MEDICALLY_EXEMPT)

        verify { viewStateObserver.onChanged(NotIsolatingAsMedicallyExempt) }
    }

    @Test
    fun `when exposure date is unknown then navigate to home`() {
        coEvery { evaluateTestingAdviceToShow(fixedClock) } returns UnknownExposureDate

        createTestSubject(optOutOfContactIsolationExtra = FULLY_VACCINATED)

        verify { navigationTargetObserver.onChanged(Home) }
    }

    @Test
    fun `onBackToHomeClicked emit Home navigation`() {
        val testSubject = createTestSubject(MINOR)

        testSubject.onBackToHomeClicked()

        verify { navigationTargetObserver.onChanged(Home) }
    }

    @Test
    fun `onBookPcrTestClicked emit BookPcrTest navigation`() {
        val testSubject = createTestSubject(MINOR)

        testSubject.onBookPcrTestTestClicked()

        verify { navigationTargetObserver.onChanged(BookPcrTest) }
    }

    private fun createTestSubject(optOutOfContactIsolationExtra: OptOutOfContactIsolationExtra): RiskyContactIsolationAdviceViewModel {
        val testSubject =
            RiskyContactIsolationAdviceViewModel(
                isolationStateMachine,
                optOutOfContactIsolationExtra,
                evaluateTestingAdviceToShow,
                fixedClock
            )

        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigationTarget().observeForever(navigationTargetObserver)
        return testSubject
    }
}
