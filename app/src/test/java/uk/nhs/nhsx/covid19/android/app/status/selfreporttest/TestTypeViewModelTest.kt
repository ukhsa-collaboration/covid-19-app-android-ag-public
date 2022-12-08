package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestTypeViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestTypeViewModel.NavigationTarget.NegativeTest
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestTypeViewModel.NavigationTarget.PositiveTest
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestTypeViewModel.NavigationTarget.VoidTest
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestTypeViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.widgets.TripleVerticalRadioGroup.TripleVerticalRadioGroupOption.OPTION_1 as POSITIVE
import uk.nhs.nhsx.covid19.android.app.widgets.TripleVerticalRadioGroup.TripleVerticalRadioGroupOption.OPTION_2 as NEGATIVE
import uk.nhs.nhsx.covid19.android.app.widgets.TripleVerticalRadioGroup.TripleVerticalRadioGroupOption.OPTION_3 as VOID

class TestTypeViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testSubject = TestTypeViewModel()

    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)
    private val navigationStateObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)
    private val selfReportTestQuestions = SelfReportTestQuestions(null, null, null,
        null, null, null, null, null)

    @Before
    fun setup() {
        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigate().observeForever(navigationStateObserver)
    }

    @Test
    fun `when nothing selected, error state set to true`() {
        testSubject.onClickContinue()

        val expectedState = ViewState(testTypeSelection = null, hasError = true)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when POSITIVE selected, testTypeSelection selection set to POSITIVE`() {
        testSubject.onTestTypeOptionChecked(POSITIVE)

        val expectedState = ViewState(testTypeSelection = POSITIVE, hasError = false)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when NEGATIVE selected, testTypeSelection selection set to NEGATIVE`() {
        testSubject.onTestTypeOptionChecked(NEGATIVE)

        val expectedState = ViewState(testTypeSelection = NEGATIVE, hasError = false)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when VOID selected, testTypeSelection selection set to VOID`() {
        testSubject.onTestTypeOptionChecked(VOID)

        val expectedState = ViewState(testTypeSelection = VOID, hasError = false)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when POSITIVE selected, navigate to positive test flow`() {
        testSubject.onTestTypeOptionChecked(POSITIVE)
        testSubject.onClickContinue()

        val expectedState = PositiveTest(selfReportTestQuestions.copy(testType = VirologyTestResult.POSITIVE))
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when NEGATIVE selected, navigate to negative test flow`() {
        testSubject.onTestTypeOptionChecked(NEGATIVE)
        testSubject.onClickContinue()

        val expectedState = NegativeTest(isNegative = true)
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when VOID selected, navigate to void test flow`() {
        testSubject.onTestTypeOptionChecked(VOID)
        testSubject.onClickContinue()

        val expectedState = VoidTest(isNegative = false)
        verify { navigationStateObserver.onChanged(expectedState) }
    }
}
