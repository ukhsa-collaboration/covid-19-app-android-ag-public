package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationAgeLimitViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationAgeLimitViewModel.NavigationTarget.IsolationResult
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationAgeLimitViewModel.NavigationTarget.VaccinationStatus
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationAgeLimitViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_1 as YES
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_2 as NO

class ExposureNotificationAgeLimitViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val mockAcknowledgeRiskyContact: AcknowledgeRiskyContact = mockk(relaxUnitFun = true)
    private val mockOptOutOfContactIsolation: OptOutOfContactIsolation = mockk(relaxUnitFun = true)

    private val testSubject = ExposureNotificationAgeLimitViewModel(mockAcknowledgeRiskyContact, mockOptOutOfContactIsolation)
    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)
    private val navigationTargetObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)

    @Before
    fun setUp() {
        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigationTarget().observeForever(navigationTargetObserver)
    }

    @Test
    fun `when the age option is selected, error state set to false`() {
        testSubject.onAgeLimitOptionChanged(YES)

        verify { viewStateObserver.onChanged(ViewState(YES, false)) }
    }

    @Test
    fun `when no option selected, error state set to true`() {
        testSubject.onClickContinue()
        verify { viewStateObserver.onChanged(ViewState(null, true)) }
    }

    @Test
    fun `when no option selected, error state remains true after valid selection`() {
        testSubject.onClickContinue()
        verify { viewStateObserver.onChanged(ViewState(null, true)) }

        testSubject.onAgeLimitOptionChanged(YES)
        verify { viewStateObserver.onChanged(ViewState(YES, true)) }
    }

    @Test
    fun `when no option selected, error state remains true after valid selection, but is cleared after click continue`() {
        testSubject.onClickContinue()
        verify { viewStateObserver.onChanged(ViewState(null, true)) }

        testSubject.onAgeLimitOptionChanged(YES)
        verify { viewStateObserver.onChanged(ViewState(YES, true)) }

        testSubject.onClickContinue()
        verify { viewStateObserver.onChanged(ViewState(YES, false)) }
    }

    @Test
    fun `when has selected YES and continue clicked navigate to vaccination status`() {
        testSubject.onAgeLimitOptionChanged(YES)
        testSubject.onClickContinue()
        verify { viewStateObserver.onChanged(ViewState(YES, false)) }
        verify { navigationTargetObserver.onChanged(VaccinationStatus) }
    }

    @Test
    fun `when has selected NO and continue clicked navigate to isolation result screen and acknowledge risky contact`() {
        testSubject.onAgeLimitOptionChanged(NO)
        testSubject.onClickContinue()
        verify { viewStateObserver.onChanged(ViewState(NO, false)) }
        verify { navigationTargetObserver.onChanged(IsolationResult) }
        verify { mockAcknowledgeRiskyContact.invoke() }
        verify { mockOptOutOfContactIsolation.invoke() }
    }
}
