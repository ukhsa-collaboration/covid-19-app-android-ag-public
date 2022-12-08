package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestKitTypeViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestKitTypeViewModel.NavigationTarget.DeclinedKeySharing
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestKitTypeViewModel.NavigationTarget.ShareKeysInfo
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestKitTypeViewModel.NavigationTarget.TestDate
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestKitTypeViewModel.NavigationTarget.TestOrigin
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestKitTypeViewModel.ViewState
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryVerticalRadioGroup.BinaryVerticalRadioGroupOption.OPTION_1 as LFD
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryVerticalRadioGroup.BinaryVerticalRadioGroupOption.OPTION_2 as PCR

class TestKitTypeViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)
    private val navigationStateObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-07-27T01:00:00.00Z"), ZoneOffset.UTC)
    private val selfReportTestQuestions = SelfReportTestQuestions(
        POSITIVE, temporaryExposureKeys = listOf(NHSTemporaryExposureKey(key = "key", rollingStartNumber = 2)),
        null, null, null, null, null, null)

    private val selfReportTestQuestionsWithAllAnswers = SelfReportTestQuestions(
        POSITIVE, temporaryExposureKeys = listOf(NHSTemporaryExposureKey(key = "key", rollingStartNumber = 2)),
        RAPID_SELF_REPORTED, true, ChosenDate(false, LocalDate.now(fixedClock)), true,
        ChosenDate(false, LocalDate.now(fixedClock)), true)

    private val testSubject = TestKitTypeViewModel(selfReportTestQuestions)

    @Before
    fun setup() {
        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigate().observeForever(navigationStateObserver)
    }

    @Test
    fun `when nothing selected, error state set to true`() {
        testSubject.onClickContinue()

        val expectedState = ViewState(testKitTypeSelection = null, hasError = true)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when LFD selected, testKitTypeSelection selection set to LFD`() {
        testSubject.onTestKitTypeOptionChecked(LFD)

        val expectedState = ViewState(testKitTypeSelection = LFD, hasError = false)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when PCR selected, testKitTypeSelection selection set to PCR`() {
        testSubject.onTestKitTypeOptionChecked(PCR)

        val expectedState = ViewState(testKitTypeSelection = PCR, hasError = false)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when back is pressed, and keys are present, should send user to share keys info page`() {
        testSubject.onBackPressed()

        val expectedState = ShareKeysInfo(selfReportTestQuestions)
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when back is pressed, and keys are not present, should send user to declined key sharing page`() {
        val testSubject = TestKitTypeViewModel(selfReportTestQuestions.copy(temporaryExposureKeys = null))
        testSubject.navigate().observeForever(navigationStateObserver)
        testSubject.onBackPressed()

        val expectedState = DeclinedKeySharing(selfReportTestQuestions.copy(temporaryExposureKeys = null))
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when LFD selected, navigate to test origin screen`() {
        testSubject.onTestKitTypeOptionChecked(LFD)
        testSubject.onClickContinue()

        val expectedState = TestOrigin(selfReportTestQuestions.copy(testKitType = RAPID_SELF_REPORTED))
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when PCR selected, navigate to test date screen`() {
        testSubject.onTestKitTypeOptionChecked(PCR)
        testSubject.onClickContinue()

        val expectedState = TestDate(selfReportTestQuestions.copy(testKitType = LAB_RESULT))
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when LFD, isNHS and reported previously saved, answer changed to PCR should navigate to test date screen, and null related fields`() {
        val testSubjectWithLFDFromNHS = TestKitTypeViewModel(selfReportTestQuestionsWithAllAnswers)

        testSubjectWithLFDFromNHS.navigate().observeForever(navigationStateObserver)

        testSubjectWithLFDFromNHS.onTestKitTypeOptionChecked(PCR)
        testSubjectWithLFDFromNHS.onClickContinue()

        val expectedState = TestDate(selfReportTestQuestionsWithAllAnswers.copy(
            testKitType = LAB_RESULT, isNHSTest = null, hasReportedResult = null))
        verify { navigationStateObserver.onChanged(expectedState) }
    }
}
