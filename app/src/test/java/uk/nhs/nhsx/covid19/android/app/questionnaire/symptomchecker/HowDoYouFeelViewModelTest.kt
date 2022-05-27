package uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.HowDoYouFeelViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.HowDoYouFeelViewModel.NavigationTarget.Next
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.HowDoYouFeelViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_1 as YES
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_2 as NO

class HowDoYouFeelViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testSubject = HowDoYouFeelViewModel(questions = symptomsCheckerQuestions)

    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)
    private val navigationStateObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)

    @Before
    fun setup() {
        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigate().observeForever(navigationStateObserver)
    }

    @Test
    fun `when nothing selected, error state set to true`() {
        testSubject.onClickContinue()

        val expectedState = ViewState(howDoYouFeelSelection = null, hasError = true)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when YES selected, howDoYouFeel selection set to YES`() {
        testSubject.onHowDoYouFeelOptionChecked(YES)

        val expectedState = ViewState(howDoYouFeelSelection = YES, hasError = false)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when NO selected, howDoYouFeel selection set to NO`() {
        testSubject.onHowDoYouFeelOptionChecked(NO)

        val expectedState = ViewState(howDoYouFeelSelection = NO, hasError = false)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when YES selected, navigate to next screen`() {
        testSubject.onHowDoYouFeelOptionChecked(YES)
        testSubject.onClickContinue()

        val expectedState = Next(symptomsCheckerQuestions.copy(howDoYouFeelSymptom = HowDoYouFeelSymptom(true)))
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when NO selected, navigate to next screen`() {
        testSubject.onHowDoYouFeelOptionChecked(NO)
        testSubject.onClickContinue()

        val expectedState = Next(symptomsCheckerQuestions.copy(howDoYouFeelSymptom = HowDoYouFeelSymptom(false)))
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    companion object {
        val symptomsCheckerQuestions = SymptomsCheckerQuestions(null, null, null)
    }
}
