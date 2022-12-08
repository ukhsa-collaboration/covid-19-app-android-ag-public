package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.AskedToShareExposureKeysInTheInitialFlow
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedSelfReportingTestFlow
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ConsentedToShareExposureKeysInTheInitialFlow
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidHaveSymptomsBeforeReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidRememberOnsetSymptomsDateBeforeReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.IsPositiveSelfLFDFree
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelfReportedPositiveSelfLFDOnGov
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.OUTSIDE_APP
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.TrackTestResultAnalyticsOnReceive
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportCheckAnswersViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportCheckAnswersViewModel.NavigationTarget.ReportedTest
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportCheckAnswersViewModel.NavigationTarget.SubmitAndContinue
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportCheckAnswersViewModel.NavigationTarget.Symptoms
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportCheckAnswersViewModel.NavigationTarget.SymptomsOnset
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportCheckAnswersViewModel.NavigationTarget.TestDate
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportCheckAnswersViewModel.NavigationTarget.TestKitType
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportCheckAnswersViewModel.NavigationTarget.TestOrigin
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportCheckAnswersViewModel.NavigationTarget.ThankYou
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportCheckAnswersViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.testordering.IsKeySubmissionSupported
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.SymptomsDate
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class SelfReportCheckAnswersViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)
    private val navigationStateObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-07-27T01:00:00.00Z"), ZoneOffset.UTC)
    private val isKeySubmissionSupported = mockk<IsKeySubmissionSupported>()
    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxUnitFun = true)
    private val trackTestResultAnalyticsOnReceive = mockk<TrackTestResultAnalyticsOnReceive>(relaxUnitFun = true)
    private val temporaryExposureKeys = listOf(NHSTemporaryExposureKey(key = "key", rollingStartNumber = 2))
    private val selfReportTestQuestions = SelfReportTestQuestions(
        POSITIVE, temporaryExposureKeys = temporaryExposureKeys, RAPID_SELF_REPORTED, true,
        ChosenDate(false, LocalDate.now(fixedClock)), true,
        ChosenDate(false, LocalDate.now(fixedClock)), true)

    private val testSubject = SelfReportCheckAnswersViewModel(isKeySubmissionSupported, fixedClock,
        analyticsEventProcessor, trackTestResultAnalyticsOnReceive, selfReportTestQuestions)

    private val testResult = ReceivedTestResult(
        diagnosisKeySubmissionToken = "00000000-0000-0000-0000-000000000000",
        testEndDate = selfReportTestQuestions.testEndDate!!.date.atStartOfDay(fixedClock.zone).toInstant(),
        testResult = selfReportTestQuestions.testType!!,
        testKitType = selfReportTestQuestions.testKitType,
        diagnosisKeySubmissionSupported = true,
        requiresConfirmatoryTest = false,
        shouldOfferFollowUpTest = false,
        symptomsOnsetDate = SymptomsDate(selfReportTestQuestions.symptomsOnsetDate?.date),
        isSelfReporting = true
    )

    @Before
    fun setup() {
        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigate().observeForever(navigationStateObserver)
    }

    @Test
    fun `when change test kit type clicked, navigate to test kit type activity`() {
        testSubject.changeTestKitTypeClicked()

        val expectedState = TestKitType(selfReportTestQuestions)
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when change test origin is clicked, navigate to test origin activity`() {
        testSubject.changeTestOriginClicked()

        val expectedState = TestOrigin(selfReportTestQuestions)
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when change test date is clicked, navigate to select test date activity`() {
        testSubject.changeTestDateClicked()

        val expectedState = TestDate(selfReportTestQuestions)
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when change symptoms is clicked, navigate to symptoms activity`() {
        testSubject.changeSymptomsClicked()

        val expectedState = Symptoms(selfReportTestQuestions)
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when change symptoms onset is clicked, navigate to symptoms onset activity`() {
        testSubject.changeSymptomsOnsetClicked()

        val expectedState = SymptomsOnset(selfReportTestQuestions)
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when change reported test is clicked, navigate to reported test activity`() {
        testSubject.changeReportedTestClicked()

        val expectedState = ReportedTest(selfReportTestQuestions)
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when back is clicked with reported test answered, navigate to reported test activity`() {
        testSubject.onBackPressed()

        val expectedState = ReportedTest(selfReportTestQuestions)
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when back is clicked without reported test answered and symptoms onset present, navigate to symptoms onset activity`() {
        val selfReportTestQuestionsWithoutReportedTestWithSymptoms = selfReportTestQuestions.copy(hasReportedResult = null)
        val testSubjectWithoutReportedTestWithSymptoms =
            SelfReportCheckAnswersViewModel(isKeySubmissionSupported, fixedClock, analyticsEventProcessor,
                trackTestResultAnalyticsOnReceive, selfReportTestQuestionsWithoutReportedTestWithSymptoms)
        testSubjectWithoutReportedTestWithSymptoms.navigate().observeForever(navigationStateObserver)

        testSubjectWithoutReportedTestWithSymptoms.onBackPressed()

        val expectedState = SymptomsOnset(selfReportTestQuestionsWithoutReportedTestWithSymptoms)
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when back is clicked without reported test and symptoms onset not present, navigate to symptoms activity`() {
        val selfReportTestQuestionsWithoutReportedTestAndSymptoms =
            selfReportTestQuestions.copy(hadSymptoms = false, symptomsOnsetDate = null, hasReportedResult = null)
        val testSubjectWithoutReportedTestAndSymptoms =
            SelfReportCheckAnswersViewModel(isKeySubmissionSupported, fixedClock, analyticsEventProcessor,
                trackTestResultAnalyticsOnReceive, selfReportTestQuestionsWithoutReportedTestAndSymptoms)
        testSubjectWithoutReportedTestAndSymptoms.navigate().observeForever(navigationStateObserver)

        testSubjectWithoutReportedTestAndSymptoms.onBackPressed()

        val expectedState = Symptoms(selfReportTestQuestionsWithoutReportedTestAndSymptoms)
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when back is clicked without reported test and symptoms unanswered, navigate to test date activity`() {
        val selfReportTestQuestionsWithoutReportedTestAndSymptomsUnanswered =
            selfReportTestQuestions.copy(hadSymptoms = null, symptomsOnsetDate = null, hasReportedResult = null)
        val testSubjectWithoutReportedTestAndSymptomsUnanswered =
            SelfReportCheckAnswersViewModel(isKeySubmissionSupported, fixedClock, analyticsEventProcessor,
                trackTestResultAnalyticsOnReceive, selfReportTestQuestionsWithoutReportedTestAndSymptomsUnanswered)
        testSubjectWithoutReportedTestAndSymptomsUnanswered.navigate().observeForever(navigationStateObserver)

        testSubjectWithoutReportedTestAndSymptomsUnanswered.onBackPressed()

        val expectedState = TestDate(selfReportTestQuestionsWithoutReportedTestAndSymptomsUnanswered)
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when submit and continue button is clicked and keySharing supported navigate to submit test result and keys progress activity`() {
        every { isKeySubmissionSupported(testResult, isSelfReportJourney = true) } returns true
        testSubject.onClickSubmitAndContinue()

        val expectedState = SubmitAndContinue(selfReportTestQuestions)

        verify(exactly = 1) { trackTestResultAnalyticsOnReceive(testResult, OUTSIDE_APP) }
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when submit and continue button is clicked and keySharing is not supported navigate to thank you page with reported true`() {
        every { isKeySubmissionSupported(testResult, isSelfReportJourney = true) } returns false
        testSubject.onClickSubmitAndContinue()

        val expectedState = ThankYou(hasReported = true, hasSharedSuccessfully = false)

        verify(exactly = 1) { trackTestResultAnalyticsOnReceive(testResult, OUTSIDE_APP) }
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when submit and continue button is clicked and keySharing is not supported navigate to thank you page with reported false`() {
        val selfReportTestWithReportedFalse = selfReportTestQuestions.copy(hasReportedResult = false)
        val testSubjectWithReportedFalse =
            SelfReportCheckAnswersViewModel(isKeySubmissionSupported, fixedClock, analyticsEventProcessor,
                trackTestResultAnalyticsOnReceive, selfReportTestWithReportedFalse)
        testSubjectWithReportedFalse.navigate().observeForever(navigationStateObserver)

        every { isKeySubmissionSupported(testResult, isSelfReportJourney = true) } returns false
        testSubjectWithReportedFalse.onClickSubmitAndContinue()

        val expectedState = ThankYou(hasReported = false, hasSharedSuccessfully = false)

        verify(exactly = 1) { trackTestResultAnalyticsOnReceive(testResult, OUTSIDE_APP) }
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when submit and continue button is clicked and keySharing is not supported navigate to thank you page with reported updated from null to true`() {
        val selfReportTestWithReportedInitiallyNull = selfReportTestQuestions.copy(hasReportedResult = null)
        val testSubjectWithReportedInitiallyNull =
            SelfReportCheckAnswersViewModel(isKeySubmissionSupported, fixedClock, analyticsEventProcessor,
                trackTestResultAnalyticsOnReceive, selfReportTestWithReportedInitiallyNull)
        testSubjectWithReportedInitiallyNull.navigate().observeForever(navigationStateObserver)

        every { isKeySubmissionSupported(testResult, isSelfReportJourney = true) } returns false
        testSubjectWithReportedInitiallyNull.onClickSubmitAndContinue()

        val expectedState = ThankYou(hasReported = true, hasSharedSuccessfully = false)

        verify(exactly = 1) { trackTestResultAnalyticsOnReceive(testResult, OUTSIDE_APP) }
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when submit and continue button is clicked, without test type navigate to submit test result and keys progress activity`() {
        val selfReportTestQuestionsWithoutTestType = selfReportTestQuestions.copy(testType = null)
        val testSubjectWithoutTestType =
            SelfReportCheckAnswersViewModel(isKeySubmissionSupported, fixedClock, analyticsEventProcessor,
                trackTestResultAnalyticsOnReceive, selfReportTestQuestionsWithoutTestType)
        testSubjectWithoutTestType.navigate().observeForever(navigationStateObserver)

        testSubjectWithoutTestType.onClickSubmitAndContinue()

        val expectedState = SubmitAndContinue(selfReportTestQuestionsWithoutTestType)
        verify { navigationStateObserver.onChanged(expectedState) }
        verify(exactly = 0) { trackTestResultAnalyticsOnReceive(testResult, OUTSIDE_APP) }
    }

    @Test
    fun `when submit and continue button is clicked, analytics everything is tracked`() {
        val selfReportTestQuestionsAllAnswers = SelfReportTestQuestions(
            testType = POSITIVE, temporaryExposureKeys = temporaryExposureKeys, testKitType = RAPID_SELF_REPORTED,
            isNHSTest = true, testEndDate = ChosenDate(true, LocalDate.now(fixedClock)), hadSymptoms = true,
            symptomsOnsetDate = ChosenDate(true, LocalDate.now(fixedClock)), hasReportedResult = true)
        val testSubjectWithAllAnswers = createTestSubject(selfReportTestQuestionsAllAnswers)

        every { isKeySubmissionSupported(createReceivedTest(selfReportTestQuestionsAllAnswers), isSelfReportJourney = true) } returns true
        testSubjectWithAllAnswers.onClickSubmitAndContinue()

        verify(exactly = 1) { analyticsEventProcessor.track(DidHaveSymptomsBeforeReceivedTestResult) }
        verify(exactly = 1) { analyticsEventProcessor.track(DidRememberOnsetSymptomsDateBeforeReceivedTestResult) }
        verify(exactly = 1) { analyticsEventProcessor.track(IsPositiveSelfLFDFree) }
        verify(exactly = 1) { analyticsEventProcessor.track(SelfReportedPositiveSelfLFDOnGov) }
        verify(exactly = 1) { analyticsEventProcessor.track(ConsentedToShareExposureKeysInTheInitialFlow) }
        verify(exactly = 1) { analyticsEventProcessor.track(AskedToShareExposureKeysInTheInitialFlow) }
        verify(exactly = 1) { analyticsEventProcessor.track(CompletedSelfReportingTestFlow) }
    }

    @Test
    fun `when submit and continue button is clicked, analytics not NHS test, did not remember symptoms onset`() {
        val selfReportTestQuestionsNotNHSTestDidNotRememberSymptoms = SelfReportTestQuestions(
            testType = POSITIVE, temporaryExposureKeys = temporaryExposureKeys, testKitType = RAPID_SELF_REPORTED,
            isNHSTest = false, testEndDate = ChosenDate(true, LocalDate.now(fixedClock)), hadSymptoms = true,
            symptomsOnsetDate = ChosenDate(false, LocalDate.now(fixedClock)), hasReportedResult = null)
        val testSubjectNotNHSTestDidNotRememberSymptoms =
            createTestSubject(selfReportTestQuestionsNotNHSTestDidNotRememberSymptoms)

        every { isKeySubmissionSupported(createReceivedTest(selfReportTestQuestionsNotNHSTestDidNotRememberSymptoms),
            isSelfReportJourney = true) } returns true
        testSubjectNotNHSTestDidNotRememberSymptoms.onClickSubmitAndContinue()

        verify(exactly = 1) { analyticsEventProcessor.track(DidHaveSymptomsBeforeReceivedTestResult) }
        verify(exactly = 0) { analyticsEventProcessor.track(DidRememberOnsetSymptomsDateBeforeReceivedTestResult) }
        verify(exactly = 0) { analyticsEventProcessor.track(IsPositiveSelfLFDFree) }
        verify(exactly = 0) { analyticsEventProcessor.track(SelfReportedPositiveSelfLFDOnGov) }
        verify(exactly = 1) { analyticsEventProcessor.track(ConsentedToShareExposureKeysInTheInitialFlow) }
        verify(exactly = 1) { analyticsEventProcessor.track(AskedToShareExposureKeysInTheInitialFlow) }
        verify(exactly = 1) { analyticsEventProcessor.track(CompletedSelfReportingTestFlow) }
    }

    @Test
    fun `when submit and continue button is clicked, analytics is NHS test, no symptoms, did not report`() {
        val selfReportTestQuestionsNHSTestNoSymptomsDidNotReport = SelfReportTestQuestions(
            testType = POSITIVE, temporaryExposureKeys = temporaryExposureKeys, testKitType = RAPID_SELF_REPORTED,
            isNHSTest = true, testEndDate = ChosenDate(true, LocalDate.now(fixedClock)), hadSymptoms = false,
            symptomsOnsetDate = null, hasReportedResult = false)
        val testSubjectNHSTestNoSymptomsDidNotReport =
            createTestSubject(selfReportTestQuestionsNHSTestNoSymptomsDidNotReport)

        every { isKeySubmissionSupported(createReceivedTest(selfReportTestQuestionsNHSTestNoSymptomsDidNotReport),
            isSelfReportJourney = true) } returns true
        testSubjectNHSTestNoSymptomsDidNotReport.onClickSubmitAndContinue()

        verify(exactly = 0) { analyticsEventProcessor.track(DidHaveSymptomsBeforeReceivedTestResult) }
        verify(exactly = 0) { analyticsEventProcessor.track(DidRememberOnsetSymptomsDateBeforeReceivedTestResult) }
        verify(exactly = 1) { analyticsEventProcessor.track(IsPositiveSelfLFDFree) }
        verify(exactly = 0) { analyticsEventProcessor.track(SelfReportedPositiveSelfLFDOnGov) }
        verify(exactly = 1) { analyticsEventProcessor.track(ConsentedToShareExposureKeysInTheInitialFlow) }
        verify(exactly = 1) { analyticsEventProcessor.track(AskedToShareExposureKeysInTheInitialFlow) }
        verify(exactly = 1) { analyticsEventProcessor.track(CompletedSelfReportingTestFlow) }
    }

    @Test
    fun `when submit and continue button is clicked, analytics only completedSelfReportingTestFlow and AskedToShareExposureKeysInTheInitialFlow is tracked`() {
        val selfReportTestQuestionsWithMinimumAnswers = SelfReportTestQuestions(
            testType = POSITIVE, temporaryExposureKeys = null, testKitType = LAB_RESULT,
            isNHSTest = null, testEndDate = ChosenDate(false, LocalDate.now(fixedClock)), hadSymptoms = null,
            symptomsOnsetDate = null, hasReportedResult = null)
        val testSubjectWithMinimumAnswers = createTestSubject(selfReportTestQuestionsWithMinimumAnswers)

        every { isKeySubmissionSupported(createReceivedTest(selfReportTestQuestionsWithMinimumAnswers), isSelfReportJourney = true) } returns true
        testSubjectWithMinimumAnswers.onClickSubmitAndContinue()

        verify(exactly = 0) { analyticsEventProcessor.track(DidHaveSymptomsBeforeReceivedTestResult) }
        verify(exactly = 0) { analyticsEventProcessor.track(DidRememberOnsetSymptomsDateBeforeReceivedTestResult) }
        verify(exactly = 0) { analyticsEventProcessor.track(IsPositiveSelfLFDFree) }
        verify(exactly = 0) { analyticsEventProcessor.track(SelfReportedPositiveSelfLFDOnGov) }
        verify(exactly = 0) { analyticsEventProcessor.track(ConsentedToShareExposureKeysInTheInitialFlow) }
        verify(exactly = 1) { analyticsEventProcessor.track(AskedToShareExposureKeysInTheInitialFlow) }
        verify(exactly = 1) { analyticsEventProcessor.track(CompletedSelfReportingTestFlow) }
    }

    @Test
    fun `when submit and continue button is clicked, analytics everything is tracked except ConsentedToShareExposureKeysInTheInitialFlow`() {
        val selfReportTestQuestionsAllAnswersWithoutKeySharing = SelfReportTestQuestions(
            testType = POSITIVE, temporaryExposureKeys = null, testKitType = RAPID_SELF_REPORTED,
            isNHSTest = true, testEndDate = ChosenDate(true, LocalDate.now(fixedClock)), hadSymptoms = true,
            symptomsOnsetDate = ChosenDate(true, LocalDate.now(fixedClock)), hasReportedResult = true)
        val testSubjectWithAllAnswersWithoutKeySharing = createTestSubject(selfReportTestQuestionsAllAnswersWithoutKeySharing)

        every { isKeySubmissionSupported(createReceivedTest(selfReportTestQuestionsAllAnswersWithoutKeySharing),
            isSelfReportJourney = true) } returns true
        testSubjectWithAllAnswersWithoutKeySharing.onClickSubmitAndContinue()

        verify(exactly = 1) { analyticsEventProcessor.track(DidHaveSymptomsBeforeReceivedTestResult) }
        verify(exactly = 1) { analyticsEventProcessor.track(DidRememberOnsetSymptomsDateBeforeReceivedTestResult) }
        verify(exactly = 1) { analyticsEventProcessor.track(IsPositiveSelfLFDFree) }
        verify(exactly = 1) { analyticsEventProcessor.track(SelfReportedPositiveSelfLFDOnGov) }
        verify(exactly = 0) { analyticsEventProcessor.track(ConsentedToShareExposureKeysInTheInitialFlow) }
        verify(exactly = 1) { analyticsEventProcessor.track(AskedToShareExposureKeysInTheInitialFlow) }
        verify(exactly = 1) { analyticsEventProcessor.track(CompletedSelfReportingTestFlow) }
    }

    private fun createTestSubject(selfReportTestQuestions: SelfReportTestQuestions): SelfReportCheckAnswersViewModel {
        return SelfReportCheckAnswersViewModel(isKeySubmissionSupported, fixedClock, analyticsEventProcessor,
            trackTestResultAnalyticsOnReceive, selfReportTestQuestions)
    }

    private fun createReceivedTest(selfReportTestQuestions: SelfReportTestQuestions): ReceivedTestResult {
        return ReceivedTestResult(
            diagnosisKeySubmissionToken = "00000000-0000-0000-0000-000000000000",
            testEndDate = selfReportTestQuestions.testEndDate!!.date.atStartOfDay(fixedClock.zone).toInstant(),
            testResult = selfReportTestQuestions.testType!!,
            testKitType = selfReportTestQuestions.testKitType,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false,
            shouldOfferFollowUpTest = false,
            symptomsOnsetDate = SymptomsDate(selfReportTestQuestions.symptomsOnsetDate?.date),
            isSelfReporting = true
        )
    }
}
