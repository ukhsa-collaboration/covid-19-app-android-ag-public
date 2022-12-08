package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.CannotRememberDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.ExplicitDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.NotStated
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSymptomsOnsetViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSymptomsOnsetViewModel.NavigationTarget.CheckAnswers
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSymptomsOnsetViewModel.NavigationTarget.ReportedTest
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSymptomsOnsetViewModel.NavigationTarget.Symptoms
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSymptomsOnsetViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit.DAYS
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SelfReportSymptomsOnsetViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-07-27T01:00:00.00Z"), ZoneOffset.UTC)
    private val viewStateObserver = mockk<Observer<ViewState>>(relaxed = true)
    private val datePickerContainerClickedObserver = mockk<Observer<Long>>(relaxed = true)
    private val navigationStateObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)

    private val indexCaseSinceSelfDiagnosisOnsetEngland = 6
    private val indexCaseSinceSelfDiagnosisOnsetWales = 5
    private val testEndDate = LocalDate.now(fixedClock)
    private val testEndDateAsInstant = testEndDate.atStartOfDay().toInstant(ZoneOffset.UTC)
    private val symptomsOnsetWindowDays = LocalDate.parse("2020-07-22")..LocalDate.parse("2020-07-27")
    private val symptomsOnsetWindowDaysWales = LocalDate.parse("2020-07-23")..LocalDate.parse("2020-07-27")
    private val defaultInitialState = ViewState(
        selectedOnsetDate = NotStated,
        hasError = false,
        symptomsOnsetWindowDays = symptomsOnsetWindowDays
    )

    private val selfReportTestQuestions = SelfReportTestQuestions(
        POSITIVE, temporaryExposureKeys = listOf(NHSTemporaryExposureKey(key = "key", rollingStartNumber = 2)),
        RAPID_SELF_REPORTED, true, ChosenDate(true, testEndDate), true, null, null)

    private val selfReportTestQuestionsWithLFDNotFromNHS = SelfReportTestQuestions(
        POSITIVE, temporaryExposureKeys = listOf(NHSTemporaryExposureKey(key = "key", rollingStartNumber = 2)),
        RAPID_SELF_REPORTED, false, ChosenDate(true, testEndDate), true, null, null)

    private val selfReportTestQuestionsWithPreviouslySavedCannotRememberSymptoms = SelfReportTestQuestions(
        POSITIVE, temporaryExposureKeys = listOf(NHSTemporaryExposureKey(key = "key", rollingStartNumber = 2)),
        RAPID_SELF_REPORTED, false, ChosenDate(true, testEndDate), true,
        ChosenDate(false, testEndDate), null)

    private val selfReportTestQuestionsWithPreviouslySavedExplicitSymptoms = SelfReportTestQuestions(
        POSITIVE, temporaryExposureKeys = listOf(NHSTemporaryExposureKey(key = "key", rollingStartNumber = 2)),
        RAPID_SELF_REPORTED, false, ChosenDate(true, testEndDate), true,
        ChosenDate(true, testEndDate.minusDays(1)), null)

    @Before
    fun setUp() {
        every { isolationStateMachine.readLogicalState().isolationConfiguration.indexCaseSinceSelfDiagnosisOnset } returns indexCaseSinceSelfDiagnosisOnsetEngland
    }

    @Test
    fun `create and calculate symptomsOnsetWindowDays england`() = runBlocking {
        val testSubject = createDefaultTestSubject()

        testSubject.viewState().observeForever(viewStateObserver)

        val expectedViewState = ViewState(
            selectedOnsetDate = NotStated,
            hasError = false,
            symptomsOnsetWindowDays = symptomsOnsetWindowDays
        )

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `create and calculate symptomsOnsetWindowDays wales`() = runBlocking {
        every { isolationStateMachine.readLogicalState().isolationConfiguration.indexCaseSinceSelfDiagnosisOnset } returns indexCaseSinceSelfDiagnosisOnsetWales

        val testSubject = createDefaultTestSubject()

        testSubject.viewState().observeForever(viewStateObserver)

        val expectedViewState = ViewState(
            selectedOnsetDate = NotStated,
            hasError = false,
            symptomsOnsetWindowDays = symptomsOnsetWindowDaysWales
        )

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `select date`() = runBlocking {
        val testSubject = createDefaultTestSubject()

        testSubject.viewState().observeForever(viewStateObserver)

        val selectedDate = Instant.now(fixedClock).minus(1, DAYS)

        testSubject.onDateSelected(selectedDate.toEpochMilli())

        val expectedState = defaultInitialState.copy(selectedOnsetDate = ExplicitDate(selectedDate.toLocalDate(ZoneOffset.UTC)))

        verifyOrder {
            viewStateObserver.onChanged(defaultInitialState)
            viewStateObserver.onChanged(expectedState)
        }
    }

    @Test
    fun `select cannot remember date`() = runBlocking {
        val testSubject = createDefaultTestSubject()

        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.cannotRememberDateChecked()

        val expectedState = defaultInitialState.copy(selectedOnsetDate = CannotRememberDate)

        verifyOrder {
            viewStateObserver.onChanged(defaultInitialState)
            viewStateObserver.onChanged(expectedState)
        }
    }

    @Test
    fun `unselect cannot remember date`() = runBlocking {
        val testSubject = createDefaultTestSubject()

        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.cannotRememberDateUnchecked()

        val expectedState = defaultInitialState.copy(selectedOnsetDate = NotStated)

        verify(exactly = 2) { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `user clicks continue without stating a date, error is shown`() = runBlocking {
        val testSubject = createDefaultTestSubject()

        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.onButtonContinueClicked()

        val expectedState = defaultInitialState.copy(hasError = true)

        verifyOrder {
            viewStateObserver.onChanged(defaultInitialState)
            viewStateObserver.onChanged(expectedState)
        }
    }

    @Test
    fun `user clicks continue setting an explicit date and has LFD from NHS, should navigate to reported test`() = runBlocking {
        val testSubject = createDefaultTestSubject()

        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigate().observeForever(navigationStateObserver)

        val selectedDate = Instant.now(fixedClock).minus(1, DAYS)

        testSubject.onDateSelected(selectedDate.toEpochMilli())

        val expectedState = defaultInitialState.copy(selectedOnsetDate = ExplicitDate(selectedDate.toLocalDate(ZoneOffset.UTC)))

        testSubject.onButtonContinueClicked()

        verifyOrder {
            viewStateObserver.onChanged(defaultInitialState)
            viewStateObserver.onChanged(expectedState)
            navigationStateObserver.onChanged(ReportedTest(selfReportTestQuestions.copy(symptomsOnsetDate =
                ChosenDate(true, selectedDate.toLocalDate(ZoneOffset.UTC))))
            )
        }
    }

    @Test
    fun `user clicks continue setting an explicit date and has LFD not from NHS, should navigate to check answers`() = runBlocking {
        val testSubject = createTestSubjectWithLFDNotFromNHS()

        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigate().observeForever(navigationStateObserver)

        val selectedDate = Instant.now(fixedClock).minus(1, DAYS)

        testSubject.onDateSelected(selectedDate.toEpochMilli())

        val expectedState = defaultInitialState.copy(selectedOnsetDate = ExplicitDate(selectedDate.toLocalDate(ZoneOffset.UTC)))

        testSubject.onButtonContinueClicked()

        verifyOrder {
            viewStateObserver.onChanged(defaultInitialState)
            viewStateObserver.onChanged(expectedState)
            navigationStateObserver.onChanged(CheckAnswers(selfReportTestQuestionsWithLFDNotFromNHS.copy(symptomsOnsetDate =
            ChosenDate(true, selectedDate.toLocalDate(ZoneOffset.UTC))))
            )
        }
    }

    @Test
    fun `user clicks continue selecting cannot remember and has LFD from NHS, should navigate to reported test`() = runBlocking {
        val testSubject = createDefaultTestSubject()

        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigate().observeForever(navigationStateObserver)

        val expectedCannotRememberDate = testEndDateAsInstant

        testSubject.cannotRememberDateChecked()

        val expectedState = defaultInitialState.copy(selectedOnsetDate = CannotRememberDate)

        testSubject.onButtonContinueClicked()

        verifyOrder {
            viewStateObserver.onChanged(defaultInitialState)
            viewStateObserver.onChanged(expectedState)
            navigationStateObserver.onChanged(ReportedTest(selfReportTestQuestions.copy(symptomsOnsetDate =
            ChosenDate(false, expectedCannotRememberDate.toLocalDate(ZoneOffset.UTC))))
            )
        }
    }

    @Test
    fun `user clicks continue selecting cannot remember and has LFD not from NHS, should navigate to check answers`() = runBlocking {
        val testSubject = createTestSubjectWithLFDNotFromNHS()

        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigate().observeForever(navigationStateObserver)

        val expectedCannotRememberDate = testEndDateAsInstant

        testSubject.cannotRememberDateChecked()

        val expectedState = defaultInitialState.copy(selectedOnsetDate = CannotRememberDate)

        testSubject.onButtonContinueClicked()

        verifyOrder {
            viewStateObserver.onChanged(defaultInitialState)
            viewStateObserver.onChanged(expectedState)
            navigationStateObserver.onChanged(CheckAnswers(selfReportTestQuestionsWithLFDNotFromNHS.copy(symptomsOnsetDate =
            ChosenDate(false, expectedCannotRememberDate.toLocalDate(ZoneOffset.UTC))))
            )
        }
    }

    @Test
    fun `on click of date picker container emit test end time to parameterize data picker`() = runBlocking {
        val testSubject = createDefaultTestSubject()
        testSubject.datePickerContainerClicked().observeForever(datePickerContainerClickedObserver)

        testSubject.onDatePickerContainerClicked()

        verify { datePickerContainerClickedObserver.onChanged(testEndDateAsInstant.toEpochMilli()) }
    }

    @Test
    fun `Symptoms date after test end date is invalid`() {
        val testSubject = createDefaultTestSubject()
        assertFalse(
            testSubject.isSymptomsOnsetDateValid(
                testEndDateAsInstant.plus(1, DAYS).toEpochMilli(),
                symptomsOnsetWindowDays
            )
        )
    }

    @Test
    fun `test end date is valid`() {
        val testSubject = createDefaultTestSubject()
        assertTrue(
            testSubject.isSymptomsOnsetDateValid(
                testEndDateAsInstant.toEpochMilli(),
                symptomsOnsetWindowDays
            )
        )
    }

    @Test
    fun `symptoms date 5 days in past from test end date is valid`() {
        val testSubject = createDefaultTestSubject()
        assertTrue(
            testSubject.isSymptomsOnsetDateValid(
                testEndDateAsInstant.minus(5, DAYS).toEpochMilli(),
                symptomsOnsetWindowDays
            )
        )
    }

    @Test
    fun `symptoms date 6 days in past from test end date is invalid`() {
        val testSubject = createDefaultTestSubject()
        assertFalse(
            testSubject.isSymptomsOnsetDateValid(
                testEndDateAsInstant.minus(6, DAYS).toEpochMilli(),
                symptomsOnsetWindowDays
            )
        )
    }

    @Test
    fun `previously saved cannot remember is set on init`() {
        val testSubject = createTestSubjectWithPreviouslySavedCannotRememberSymptoms()

        testSubject.viewState().observeForever(viewStateObserver)

        val expectedState = defaultInitialState.copy(selectedOnsetDate = CannotRememberDate)

        verifyOrder {
            viewStateObserver.onChanged(expectedState)
        }
    }

    @Test
    fun `previously saved explicit symptoms date is set on init`() {
        val testSubject = createTestSubjectWithPreviouslySavedExplicitSymptoms()

        testSubject.viewState().observeForever(viewStateObserver)

        val expectedState = defaultInitialState.copy(selectedOnsetDate =
            ExplicitDate(selfReportTestQuestionsWithPreviouslySavedExplicitSymptoms.symptomsOnsetDate!!.date))

        verifyOrder {
            viewStateObserver.onChanged(expectedState)
        }
    }

    @Test
    fun `when back is pressed should send user to symptoms screen`() {
        val testSubject = createDefaultTestSubject()

        testSubject.navigate().observeForever(navigationStateObserver)

        testSubject.onBackPressed()

        val expectedState = Symptoms(selfReportTestQuestions)
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    private fun createDefaultTestSubject(): SelfReportSymptomsOnsetViewModel {
        return SelfReportSymptomsOnsetViewModel(
            fixedClock,
            isolationStateMachine,
            selfReportTestQuestions
        )
    }

    private fun createTestSubjectWithLFDNotFromNHS(): SelfReportSymptomsOnsetViewModel {
        return SelfReportSymptomsOnsetViewModel(
            fixedClock,
            isolationStateMachine,
            selfReportTestQuestionsWithLFDNotFromNHS
        )
    }

    private fun createTestSubjectWithPreviouslySavedCannotRememberSymptoms(): SelfReportSymptomsOnsetViewModel {
        return SelfReportSymptomsOnsetViewModel(
            fixedClock,
            isolationStateMachine,
            selfReportTestQuestionsWithPreviouslySavedCannotRememberSymptoms
        )
    }

    private fun createTestSubjectWithPreviouslySavedExplicitSymptoms(): SelfReportSymptomsOnsetViewModel {
        return SelfReportSymptomsOnsetViewModel(
            fixedClock,
            isolationStateMachine,
            selfReportTestQuestionsWithPreviouslySavedExplicitSymptoms
        )
    }
}
