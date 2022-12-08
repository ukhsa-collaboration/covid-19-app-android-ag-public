package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSymptomsViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSymptomsViewModel.NavigationTarget.CheckAnswers
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSymptomsViewModel.NavigationTarget.ReportedTest
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSymptomsViewModel.NavigationTarget.SymptomOnsetDate
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSymptomsViewModel.NavigationTarget.TestDate
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSymptomsViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_1 as YES
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_2 as NO
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class SelfReportSymptomsViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)
    private val navigationStateObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)
    private val fixedClock = Clock.fixed(Instant.parse("2022-10-28T01:00:00.00Z"), ZoneOffset.UTC)
    private val selfReportTestQuestions = SelfReportTestQuestions(
        POSITIVE, temporaryExposureKeys = listOf(NHSTemporaryExposureKey(key = "key", rollingStartNumber = 2)),
        RAPID_SELF_REPORTED, false, ChosenDate(false, LocalDate.now(fixedClock)),
        null, null, null)

    private val testSubject = SelfReportSymptomsViewModel(selfReportTestQuestions)

    @Before
    fun setup() {
        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigate().observeForever(navigationStateObserver)
    }

    @Test
    fun `when nothing selected, error state set to true`() {
        testSubject.onClickContinue()

        val expectedState = ViewState(symptomsSelection = null, hasError = true)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when yes selected, symptomsSelection set to YES`() {
        testSubject.onSymptomsOptionChecked(YES)

        val expectedState = ViewState(symptomsSelection = YES, hasError = false)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when no selected, symptomsSelection set to No`() {
        testSubject.onSymptomsOptionChecked(NO)

        val expectedState = ViewState(symptomsSelection = NO, hasError = false)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when back is pressed, should send user to test date screen`() {
        testSubject.onBackPressed()

        val expectedState = TestDate(selfReportTestQuestions)
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when yes selected, navigate to symptoms onset date screen`() {
        testSubject.onSymptomsOptionChecked(YES)
        testSubject.onClickContinue()

        val expectedState = SymptomOnsetDate(selfReportTestQuestions.copy(hadSymptoms = true))
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when no selected with LFD and NHS test, navigate to reported test screen`() {
        val selfReportTestQuestionsWithLFDAndNHSTest = selfReportTestQuestions.copy(isNHSTest = true)
        val testSubjectWithLFDAndNHSTest = SelfReportSymptomsViewModel(selfReportTestQuestionsWithLFDAndNHSTest)
        testSubjectWithLFDAndNHSTest.navigate().observeForever(navigationStateObserver)

        testSubjectWithLFDAndNHSTest.onSymptomsOptionChecked(NO)
        testSubjectWithLFDAndNHSTest.onClickContinue()

        val expectedState = ReportedTest(selfReportTestQuestionsWithLFDAndNHSTest.copy(hadSymptoms = false))
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when no selected with LFD and not a NHS test, navigate to check answers screen`() {
        val selfReportTestQuestionsWithLFDAndNotNHSTest = selfReportTestQuestions.copy(isNHSTest = false)
        val testSubjectWithLFDAndNotNHSTest = SelfReportSymptomsViewModel(selfReportTestQuestionsWithLFDAndNotNHSTest)
        testSubjectWithLFDAndNotNHSTest.navigate().observeForever(navigationStateObserver)

        testSubjectWithLFDAndNotNHSTest.onSymptomsOptionChecked(NO)
        testSubjectWithLFDAndNotNHSTest.onClickContinue()

        val expectedState = CheckAnswers(selfReportTestQuestionsWithLFDAndNotNHSTest.copy(hadSymptoms = false))
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when previous selection is yes, previous selection is saved`() {
        val testSubjectWithPreviousSymptomsYesSelection = SelfReportSymptomsViewModel(selfReportTestQuestions.copy(hadSymptoms = true))

        testSubjectWithPreviousSymptomsYesSelection.viewState().observeForever(viewStateObserver)

        val expectedState = ViewState(symptomsSelection = YES, hasError = false)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when previous selection is no, previous selection is saved`() {
        val testSubjectWithPreviousSymptomsNoSelection = SelfReportSymptomsViewModel(selfReportTestQuestions.copy(hadSymptoms = false))

        testSubjectWithPreviousSymptomsNoSelection.viewState().observeForever(viewStateObserver)

        val expectedState = ViewState(symptomsSelection = NO, hasError = false)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when has symptoms onset date from previous, and changes answer to no should null symptoms onset`() {
        val selfReportTestQuestionsWithSymptomsOnset = selfReportTestQuestions.copy(
            hadSymptoms = true, symptomsOnsetDate = ChosenDate(false, LocalDate.now(fixedClock)))

        val testSubjectWithSymptomsOnset = SelfReportSymptomsViewModel(selfReportTestQuestionsWithSymptomsOnset)

        testSubjectWithSymptomsOnset.viewState().observeForever(viewStateObserver)
        testSubjectWithSymptomsOnset.navigate().observeForever(navigationStateObserver)

        testSubjectWithSymptomsOnset.onSymptomsOptionChecked(NO)

        testSubjectWithSymptomsOnset.onClickContinue()

        val expectedInitialViewState = ViewState(symptomsSelection = YES, hasError = false)
        val expectedNavigationState = CheckAnswers(selfReportTestQuestionsWithSymptomsOnset.copy(hadSymptoms = false, symptomsOnsetDate = null))
        verifyOrder {
            viewStateObserver.onChanged(expectedInitialViewState)
            viewStateObserver.onChanged(expectedInitialViewState.copy(symptomsSelection = NO))
            navigationStateObserver.onChanged(expectedNavigationState)
        }
    }

    @Test
    fun `when has lfd from nhs with symptoms onset date from previous, and changes answer to no should null symptoms onset`() {
        val selfReportTestQuestionsWithLFDFromNHSAndSymptomsOnset = selfReportTestQuestions.copy(
            isNHSTest = true, hadSymptoms = true, symptomsOnsetDate = ChosenDate(false, LocalDate.now(fixedClock)))

        val testSubjectWithLFDFromNHSAndSymptomsOnset = SelfReportSymptomsViewModel(
            selfReportTestQuestionsWithLFDFromNHSAndSymptomsOnset)

        testSubjectWithLFDFromNHSAndSymptomsOnset.viewState().observeForever(viewStateObserver)
        testSubjectWithLFDFromNHSAndSymptomsOnset.navigate().observeForever(navigationStateObserver)

        testSubjectWithLFDFromNHSAndSymptomsOnset.onSymptomsOptionChecked(NO)

        testSubjectWithLFDFromNHSAndSymptomsOnset.onClickContinue()

        val expectedInitialViewState = ViewState(symptomsSelection = YES, hasError = false)
        val expectedNavigationState = ReportedTest(selfReportTestQuestionsWithLFDFromNHSAndSymptomsOnset.copy(hadSymptoms = false, symptomsOnsetDate = null))
        verifyOrder {
            viewStateObserver.onChanged(expectedInitialViewState)
            viewStateObserver.onChanged(expectedInitialViewState.copy(symptomsSelection = NO))
            navigationStateObserver.onChanged(expectedNavigationState)
        }
    }
}
