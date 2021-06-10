package uk.nhs.nhsx.covid19.android.app.status.testinghub

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.state.asLogical
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.ShowFindOutAboutTesting.DoNotShow
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.ShowFindOutAboutTesting.Show
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.util.DistrictAreaStringProvider
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class TestingHubViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxUnitFun = true)
    private val lastVisitedBookTestTypeVenueDateProvider =
        mockk<LastVisitedBookTestTypeVenueDateProvider>(relaxUnitFun = true)
    private val districtAreaStringProvider = mockk<DistrictAreaStringProvider>()
    private val fixedClock = Clock.fixed(Instant.parse("2020-05-22T10:00:00Z"), ZoneOffset.UTC)
    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)

    val isolationHelper = IsolationHelper(fixedClock)

    private val testSubject = TestingHubViewModel(
        isolationStateMachine,
        lastVisitedBookTestTypeVenueDateProvider,
        districtAreaStringProvider,
        fixedClock
    )

    private val expectedUrlResId = 0

    @Before
    fun setUp() {
        every { lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk() } returns false
        every { isolationStateMachine.readLogicalState() } returns isolationHelper.neverInIsolation().asLogical()
        coEvery { districtAreaStringProvider.provide(any()) } returns expectedUrlResId

        testSubject.viewState().observeForever(viewStateObserver)
    }

    @Test
    fun `do not show book when no active isolation and no book test type venue at risk`() {
        testSubject.onResume()

        verify {
            viewStateObserver.onChanged(
                ViewState(showBookTestButton = false, showFindOutAboutTestingButton = Show(expectedUrlResId))
            )
        }
    }

    @Test
    fun `show book test button and find out about testing if active isolation and book test type venue at risk`() {
        every { lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk() } returns true

        testSubject.onResume()

        verify {
            viewStateObserver.onChanged(
                ViewState(showBookTestButton = true, showFindOutAboutTestingButton = Show(expectedUrlResId))
            )
        }
    }

    @Test
    fun `show book test and do not show find out about testing if no book test type venue at risk but active isolation`() {
        val activeIsolation = isolationHelper.selfAssessment().asIsolation().asLogical()
        every { isolationStateMachine.readLogicalState() } returns activeIsolation
        every { lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk() } returns false

        testSubject.onResume()

        verify {
            viewStateObserver.onChanged(
                ViewState(showBookTestButton = true, showFindOutAboutTestingButton = DoNotShow)
            )
        }
    }
}
