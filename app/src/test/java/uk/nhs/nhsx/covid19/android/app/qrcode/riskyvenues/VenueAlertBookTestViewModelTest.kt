package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedTakeTestLaterM2Journey
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedTakeTestM2Journey
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.notifications.RiskyVenueAlertProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertBookTestViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertBookTestViewModel.NavigationTarget.BookTest
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertBookTestViewModel.NavigationTarget.Finish
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertBookTestViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertBookTestViewModel.ViewState.KnownVisit
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertBookTestViewModel.ViewState.UnknownVisit
import java.time.Instant

class VenueAlertBookTestViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val venuesStorage = mockk<VisitedVenuesStorage>()
    private val riskyVenueAlertProvider = mockk<RiskyVenueAlertProvider>(relaxUnitFun = true)
    private val evaluateVenueAlertNavigation = mockk<EvaluateVenueAlertNavigation>()
    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxUnitFun = true)

    private val testSubject =
        VenueAlertBookTestViewModel(
            venuesStorage,
            riskyVenueAlertProvider,
            evaluateVenueAlertNavigation,
            analyticsEventProcessor
        )

    private val venueVisitObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)

    private val navigationEventObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)

    @Before
    fun setUp() {
        testSubject.navigationEvent().observeForever(navigationEventObserver)
    }

    @Test
    fun `update venue visit state when there is venue visit with provided venue id`() = runBlocking {
        val venueId = "venueId"
        val venueVisit = VenueVisit(venue = Venue(venueId, ""), from = Instant.now(), to = Instant.now())

        coEvery { venuesStorage.getVisitByVenueId(venueId) } returns venueVisit

        testSubject.venueVisitState().observeForever(venueVisitObserver)

        testSubject.updateVenueVisitState(venueId)

        coVerify { venueVisitObserver.onChanged(KnownVisit(venueVisit)) }
    }

    @Test
    fun `update venue visit state when there is no venue visit with provided venue id`() = runBlocking {
        val venueId = "venueId"

        coEvery { venuesStorage.getVisitByVenueId(venueId) } returns null

        testSubject.venueVisitState().observeForever(venueVisitObserver)

        testSubject.updateVenueVisitState(venueId)

        coVerifyOrder {
            riskyVenueAlertProvider setProperty "riskyVenueAlert" value null
            venueVisitObserver.onChanged(UnknownVisit)
        }
    }

    @Test
    fun `acknowledge alert removes risky venue alert from storage`() {
        testSubject.acknowledgeVenueAlert()

        verify { riskyVenueAlertProvider setProperty "riskyVenueAlert" value null }
    }

    @Test
    fun `navigation target is book a test`() {
        every { evaluateVenueAlertNavigation.invoke() } returns EvaluateVenueAlertNavigation.NavigationTarget.BookATest

        testSubject.onBookATestClicked()

        verifyOrder {
            analyticsEventProcessor.track(SelectedTakeTestM2Journey)
            riskyVenueAlertProvider setProperty "riskyVenueAlert" value null
            navigationEventObserver.onChanged(
                BookTest(navigationTarget = EvaluateVenueAlertNavigation.NavigationTarget.BookATest)
            )
        }
    }

    @Test
    fun `navigation target is SymptomsAfterRiskyVenue`() {
        every { evaluateVenueAlertNavigation.invoke() } returns EvaluateVenueAlertNavigation.NavigationTarget.SymptomsAfterRiskyVenue

        testSubject.onBookATestClicked()

        verifyOrder {
            analyticsEventProcessor.track(SelectedTakeTestM2Journey)
            riskyVenueAlertProvider setProperty "riskyVenueAlert" value null
            navigationEventObserver.onChanged(
                BookTest(navigationTarget = EvaluateVenueAlertNavigation.NavigationTarget.SymptomsAfterRiskyVenue)
            )
        }
    }

    @Test
    fun `on return home clicked`() {
        testSubject.onReturnToHomeClicked()

        verifyOrder {
            analyticsEventProcessor.track(SelectedTakeTestLaterM2Journey)
            riskyVenueAlertProvider setProperty "riskyVenueAlert" value null
            navigationEventObserver.onChanged(Finish)
        }
    }
}
