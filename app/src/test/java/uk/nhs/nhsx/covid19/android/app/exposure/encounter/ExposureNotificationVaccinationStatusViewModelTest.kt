package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationVaccinationStatusViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationVaccinationStatusViewModel.NavigationTarget.FullyVaccinated
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationVaccinationStatusViewModel.NavigationTarget.Isolating
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationVaccinationStatusViewModel.ViewState
import java.time.LocalDate
import kotlin.test.assertEquals
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_1 as YES
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_2 as NO

class ExposureNotificationVaccinationStatusViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val mockAcknowledgeRiskyContact: AcknowledgeRiskyContact = mockk(relaxUnitFun = true)
    private val mockOptOutOfContactIsolation: OptOutOfContactIsolation = mockk(relaxUnitFun = true)
    private val mockGetLastDoseDateLimit: GetLastDoseDateLimit = mockk(relaxed = true)

    private val testSubject = ExposureNotificationVaccinationStatusViewModel(mockAcknowledgeRiskyContact, mockOptOutOfContactIsolation, mockGetLastDoseDateLimit)

    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)
    private val navigateObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)

    @Before
    fun setUp() {
        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigate().observeForever(navigateObserver)
    }

    @Test
    fun `when doses option selected, show error set to false`() {
        testSubject.onAllDosesOptionChanged(YES)

        verify { viewStateObserver.onChanged(ViewState(allDosesSelection = YES, showError = false)) }
    }

    @Test
    fun `when no option selected, on click continue sets show error to true`() {
        testSubject.onClickContinue()

        verify { viewStateObserver.onChanged(ViewState(showError = true)) }
    }

    @Test
    fun `when doses option selected yes, but no date, on click continue sets show error to true`() {
        testSubject.onAllDosesOptionChanged(YES)
        testSubject.onClickContinue()

        verify { viewStateObserver.onChanged(ViewState(allDosesSelection = YES, showError = true)) }
    }

    @Test
    fun `when doses option selected no, on click continue navigates to result`() {
        testSubject.onAllDosesOptionChanged(NO)

        verify { viewStateObserver.onChanged(ViewState(allDosesSelection = NO)) }

        testSubject.onClickContinue()

        confirmVerified(viewStateObserver)
        verify { navigateObserver.onChanged(Isolating) }
        verify(exactly = 0) { mockOptOutOfContactIsolation() }
    }

    @Test
    fun `when date option selected yes, view state is updated`() {
        testSubject.onDoseDateOptionChanged(YES)

        verify { viewStateObserver.onChanged(ViewState(doseDateSelection = YES)) }
    }

    @Test
    fun `when doses option selected yes, date option selected yes, on click continue navigates to result, acknowledges risky contact and opts out of contact isolation`() {
        testSubject.onAllDosesOptionChanged(YES)
        testSubject.onDoseDateOptionChanged(YES)
        testSubject.onClickContinue()

        verify {
            navigateObserver.onChanged(FullyVaccinated)
            mockAcknowledgeRiskyContact()
            mockOptOutOfContactIsolation()
        }
    }

    @Test
    fun `when doses option selected yes, date option selected no, on click continue navigates to result and acknowledges risky contact and does not opts out of contact isolation`() {
        testSubject.onAllDosesOptionChanged(YES)
        testSubject.onDoseDateOptionChanged(NO)
        testSubject.onClickContinue()

        verify {
            navigateObserver.onChanged(Isolating)
            mockAcknowledgeRiskyContact()
        }
        verify(exactly = 0) { mockOptOutOfContactIsolation() }
    }

    @Test
    fun `when doses option selected yes, then date option selected no, doses option changed to no should clear date option`() {
        testSubject.onAllDosesOptionChanged(YES)
        verify { viewStateObserver.onChanged(ViewState(allDosesSelection = YES, doseDateSelection = null)) }

        testSubject.onDoseDateOptionChanged(YES)
        verify { viewStateObserver.onChanged(ViewState(allDosesSelection = YES, doseDateSelection = YES)) }

        testSubject.onAllDosesOptionChanged(NO)
        verify { viewStateObserver.onChanged(ViewState(allDosesSelection = NO, doseDateSelection = null)) }
    }

    @Test
    fun `lastDoseDateLimit returns result of getLastDoseDateLimit`() {
        val expected = LocalDate.of(2021, 9, 5)

        every { mockGetLastDoseDateLimit() } returns expected

        assertEquals(expected, testSubject.lastDoseDateLimit())
    }
}
