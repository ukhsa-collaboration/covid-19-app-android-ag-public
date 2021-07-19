package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.mockk
import io.mockk.verifyOrder
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedHasNoSymptomsM2Journey
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedHasSymptomsM2Journey
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.NavigationTarget.Finish
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.NavigationTarget.Home
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.NavigationTarget.OrderLfdTest
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.NavigationTarget.Questionnaire

class SymptomsAfterRiskyVenueViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxUnitFun = true)
    private val navigationTargetObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)
    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)

    @Test
    fun `when has symptoms is clicked`() {
        val testSubject = setupTestSubject(isCancelDialogEnabled = false)
        testSubject.onHasSymptomsClicked()

        verifyOrder {
            analyticsEventProcessor.track(SelectedHasSymptomsM2Journey)
            navigationTargetObserver.onChanged(Questionnaire)
        }
    }

    @Test
    fun `when has no symptoms is clicked`() {
        val testSubject = setupTestSubject(isCancelDialogEnabled = false)
        testSubject.onHasNoSymptomsClicked()

        verifyOrder {
            analyticsEventProcessor.track(SelectedHasNoSymptomsM2Journey)
            navigationTargetObserver.onChanged(OrderLfdTest)
        }
    }

    @Test
    fun `when cancel button is clicked with cancel dialog enabled`() {
        val testSubject = setupTestSubject(isCancelDialogEnabled = true)
        testSubject.onCancelButtonClicked()

        verify {
            viewStateObserver.onChanged(ViewState(showCancelDialog = true))
        }
    }

    @Test
    fun `when cancel button is clicked with cancel dialog disabled`() {
        val testSubject = setupTestSubject(isCancelDialogEnabled = false)
        testSubject.onCancelButtonClicked()

        verify {
            navigationTargetObserver.onChanged(Finish)
        }
    }

    @Test
    fun `when leave in dialog is clicked`() {
        val testSubject = setupTestSubject(isCancelDialogEnabled = false)
        testSubject.onDialogOptionLeaveClicked()

        verify {
            navigationTargetObserver.onChanged(Home)
        }
    }

    @Test
    fun `when stay in dialog is clicked`() {
        val testSubject = setupTestSubject(isCancelDialogEnabled = false)
        testSubject.onDialogOptionStayClicked()

        verify {
            viewStateObserver.onChanged(ViewState(showCancelDialog = false))
        }
    }

    private fun setupTestSubject(isCancelDialogEnabled: Boolean): SymptomsAfterRiskyVenueViewModel {
        val testSubject = SymptomsAfterRiskyVenueViewModel(
            analyticsEventProcessor,
            shouldShowCancelConfirmationDialogOnCancelButtonClick = isCancelDialogEnabled
        )
        testSubject.navigationTarget().observeForever(navigationTargetObserver)
        testSubject.viewState().observeForever(viewStateObserver)
        return testSubject
    }
}
