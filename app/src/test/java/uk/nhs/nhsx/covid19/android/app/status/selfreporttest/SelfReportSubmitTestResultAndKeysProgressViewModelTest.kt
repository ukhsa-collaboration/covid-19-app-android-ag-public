package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SuccessfullySharedExposureKeys
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.common.Lce
import uk.nhs.nhsx.covid19.android.app.common.Lce.Error
import uk.nhs.nhsx.covid19.android.app.common.Lce.Loading
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.exposure.MAX_TRANSMISSION_RISK_LEVEL
import uk.nhs.nhsx.covid19.android.app.exposure.SubmitTemporaryExposureKeys
import uk.nhs.nhsx.covid19.android.app.exposure.TransmissionRiskLevelApplier
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfo
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfoProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnTestResultAcknowledge
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSubmitTestResultAndKeysProgressViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSubmitTestResultAndKeysProgressViewModel.NavigationTarget.Finish
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSubmitTestResultAndKeysProgressViewModel.NavigationTarget.ThankYou
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgeTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.SymptomsDate
import java.io.IOException
import java.lang.IllegalStateException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit.DAYS

class SelfReportSubmitTestResultAndKeysProgressViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val observer = mockk<Observer<Lce<Unit>>>(relaxUnitFun = true)
    private val navigationStateObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)
    private val submitTemporaryExposureKeys = mockk<SubmitTemporaryExposureKeys>()
    private val transmissionRiskLevelApplier = mockk<TransmissionRiskLevelApplier>(relaxed = true)
    private val keySharingInfoProvider = mockk<KeySharingInfoProvider>(relaxUnitFun = true)
    private val fixedClock = Clock.fixed(Instant.parse("2022-10-28T01:00:00.00Z"), ZoneOffset.UTC)
    private val isolationStateMachine = mockk<IsolationStateMachine>()
    private val acknowledgeTestResult = mockk<AcknowledgeTestResult>(relaxUnitFun = true)
    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxUnitFun = true)
    private val selfReportTestQuestions = SelfReportTestQuestions(
        POSITIVE, temporaryExposureKeys = listOf(NHSTemporaryExposureKey(key = "key", rollingStartNumber = 2)),
        RAPID_SELF_REPORTED, true, ChosenDate(false, LocalDate.now(fixedClock)), true,
        ChosenDate(false, LocalDate.now(fixedClock)), true)

    private val testSubject = SelfReportSubmitTestResultAndKeysProgressViewModel(
        submitTemporaryExposureKeys = submitTemporaryExposureKeys,
        transmissionRiskLevelApplier = transmissionRiskLevelApplier,
        keySharingInfoProvider = keySharingInfoProvider,
        clock = fixedClock,
        isolationStateMachine = isolationStateMachine,
        acknowledgeTestResult = acknowledgeTestResult,
        analyticsEventProcessor = analyticsEventProcessor,
        questions = selfReportTestQuestions
    )

    private val key1 = NHSTemporaryExposureKey(key = "key", rollingStartNumber = 2, transmissionRiskLevel = MAX_TRANSMISSION_RISK_LEVEL)
    private val keySharingInfo = KeySharingInfo(diagnosisKeySubmissionToken = "", acknowledgedDate = Instant.now(fixedClock))
    private val keySharingInfoWithOldAcknowledgeDate = KeySharingInfo(diagnosisKeySubmissionToken = "",
        acknowledgedDate = Instant.now(fixedClock).minus(2, DAYS))
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
    fun setUp() {
        testSubject.submitKeysResult().observeForever(observer)
        testSubject.navigate().observeForever(navigationStateObserver)
    }

    @Test
    fun `attempt to share keys, but testKitType is null, should show error`() = runBlocking {
        val testSubjectWithoutTestKitType = createCustomTestSubject(selfReportTestQuestions.copy(testKitType = null))
        testSubjectWithoutTestKitType.submitKeysResult().observeForever(observer)

        testSubjectWithoutTestKitType.submitTestResultAndKeys()
        coVerifyOrder {
            observer.onChanged(Loading)
            observer.onChanged(Error(IllegalStateException("test type, kit or date not set")))
        }
    }

    @Test
    fun `attempt to share keys, but no exposure keys present, should set declined key sharing and navigate to thank you`() = runBlocking {
        val testSubjectWithoutKeys = createCustomTestSubject(selfReportTestQuestions.copy(temporaryExposureKeys = null))
        testSubjectWithoutKeys.submitKeysResult().observeForever(observer)
        testSubjectWithoutKeys.navigate().observeForever(navigationStateObserver)

        setUpDefaultReceiveAndAcknowledgeTestResultIfNeeded()

        testSubjectWithoutKeys.submitTestResultAndKeys()
        coVerify {
            observer.onChanged(Loading)
            keySharingInfoProvider.setHasDeclinedSharingKeys()
            navigationStateObserver.onChanged(ThankYou(hasReported = true, hasSharedSuccessfully = false))
        }
    }

    @Test
    fun `attempt to share keys, but no exposure keys present and keySharingInfo is old, should reset keySharingInfo and navigate to thank you`() = runBlocking {
        val testSubjectWithoutKeys = createCustomTestSubject(selfReportTestQuestions.copy(temporaryExposureKeys = null))
        testSubjectWithoutKeys.submitKeysResult().observeForever(observer)
        testSubjectWithoutKeys.navigate().observeForever(navigationStateObserver)

        setUpReceiveAndAcknowledgeTestResultIfNeededWithStaticKeySharingInfo(keySharingInfoWithOldAcknowledgeDate)

        testSubjectWithoutKeys.submitTestResultAndKeys()
        coVerify {
            observer.onChanged(Loading)
            keySharingInfoProvider.reset()
            navigationStateObserver.onChanged(ThankYou(hasReported = true, hasSharedSuccessfully = false))
        }
    }

    @Test
    fun `attempt to share keys, but keySharingInfo is unchanged, should show error`() = runBlocking {
        attemptToShareKeysButKeySharingInfoWasNotUpdated(keySharingInfoWithOldAcknowledgeDate)
    }

    @Test
    fun `attempt to share keys, but keySharingInfo is null, should show error`() = runBlocking {
        attemptToShareKeysButKeySharingInfoWasNotUpdated(null)
    }

    @Test
    fun `keys with transmissionRiskLevel of 0 is filtered out`() = runBlocking {
        val key1 = NHSTemporaryExposureKey("1", intervalStart("2014-12-22T00:00:00Z"), 144)
        val key2 = NHSTemporaryExposureKey("2", intervalStart("2014-12-23T00:00:00Z"), 144)
        val key3 = NHSTemporaryExposureKey("3", intervalStart("2014-12-24T00:00:00Z"), 144)
        val key4 = NHSTemporaryExposureKey("4", intervalStart("2014-12-25T00:00:00Z"), 144)
        val key5 = NHSTemporaryExposureKey("5", intervalStart("2014-12-26T00:00:00Z"), 144)

        val exposureKeys = listOf(key1, key2, key3, key4, key5)

        val testSubjectWithSpecificKeys = createCustomTestSubject(selfReportTestQuestions.copy(temporaryExposureKeys = exposureKeys))
        testSubjectWithSpecificKeys.submitKeysResult().observeForever(observer)
        testSubjectWithSpecificKeys.navigate().observeForever(navigationStateObserver)

        coEvery { transmissionRiskLevelApplier.applyTransmissionRiskLevels(any(), any()) } returns
                exposureKeys.map { if (it.key == "1") it.copy(transmissionRiskLevel = 0) else it.copy(transmissionRiskLevel = 1) }

        setUpDefaultReceiveAndAcknowledgeTestResultIfNeeded()

        testSubjectWithSpecificKeys.submitTestResultAndKeys()

        val expected =
            listOf(
                key2.copy(transmissionRiskLevel = 1),
                key3.copy(transmissionRiskLevel = 1),
                key4.copy(transmissionRiskLevel = 1),
                key5.copy(transmissionRiskLevel = 1)
        )

        coVerify { submitTemporaryExposureKeys(expected, isPrivateJourney = true, testKit = selfReportTestQuestions.testKitType.toString()) }
    }

    @Test
    fun `when transmissionRiskLevelApplier throws, error is shown`() = runBlocking {
        val expectedMessage = "Something went wrong"
        coEvery { transmissionRiskLevelApplier.applyTransmissionRiskLevels(any(), any()) } throws Exception(
            expectedMessage
        )

        setUpDefaultReceiveAndAcknowledgeTestResultIfNeeded()

        testSubject.submitTestResultAndKeys()

        coVerifyOrder {
            observer.onChanged(Loading)
            observer.onChanged(Error(expectedMessage))
        }
    }

    @Test
    fun `attempt to share keys, but exception is thrown by submitTemporaryExposureKeys`() = runBlocking {
        setUpDefaultReceiveAndAcknowledgeTestResultIfNeeded()
        coEvery { transmissionRiskLevelApplier.applyTransmissionRiskLevels(any(), any()) } returns listOf(key1)
        val throwable = IOException()
        coEvery { submitTemporaryExposureKeys(listOf(key1), isPrivateJourney = true, testKit = selfReportTestQuestions.testKitType.toString()) } returns
                Failure(throwable)

        testSubject.submitTestResultAndKeys()

        coVerifyOrder {
            observer.onChanged(Loading)
            observer.onChanged(Error(throwable))
        }
    }

    @Test
    fun `attempt to share keys is successful`() = runBlocking {
        setUpDefaultReceiveAndAcknowledgeTestResultIfNeeded()
        coEvery { transmissionRiskLevelApplier.applyTransmissionRiskLevels(any(), any()) } returns listOf(key1)

        coEvery { submitTemporaryExposureKeys(listOf(key1), isPrivateJourney = true, testKit = selfReportTestQuestions.testKitType.toString()) } returns
                Result.Success(Unit)

        testSubject.submitTestResultAndKeys()

        coVerifyOrder {
            observer.onChanged(Loading)
            observer.onChanged(Lce.Success(Unit))
        }
    }

    @Test
    fun `onSuccess called, should navigate to thank you screen`() {
        testSubject.onSuccess()
        coVerifyOrder {
            keySharingInfoProvider.reset()
            analyticsEventProcessor.track(SuccessfullySharedExposureKeys)
            navigationStateObserver.onChanged(ThankYou(hasReported = true, hasSharedSuccessfully = true))
        }
    }

    @Test
    fun `receiveAndAcknowledgeTestResultIfNeeded is run as expected`() = runBlocking {
        setUpDefaultReceiveAndAcknowledgeTestResultIfNeeded()

        testSubject.submitTestResultAndKeys()

        coVerifyOrder {
            acknowledgeTestResult(testResult)
        }
    }

    @Test
    fun `receiveAndAcknowledgeTestResultIfNeeded is run second time after first run successful, but API error should return directly without reattempts`() = runBlocking {
        coEvery { transmissionRiskLevelApplier.applyTransmissionRiskLevels(any(), any()) } returns listOf(key1)
        val throwable = IOException()
        coEvery { submitTemporaryExposureKeys(listOf(key1), isPrivateJourney = true, testKit = selfReportTestQuestions.testKitType.toString()) } returns
                Failure(throwable)
        setUpDefaultReceiveAndAcknowledgeTestResultIfNeeded()

        testSubject.submitTestResultAndKeys()

        coVerifyOrder {
            observer.onChanged(Loading)
            acknowledgeTestResult(testResult)
            observer.onChanged(Error(throwable))
        }

        testSubject.submitTestResultAndKeys()
        coVerify(exactly = 1) { acknowledgeTestResult(testResult) }
        coVerify(exactly = 0) { isolationStateMachine.processEvent(OnTestResultAcknowledge(testResult)) }
    }

    @Test
    fun `receiveAndAcknowledgeTestResultIfNeeded is run second time after first run unsuccessful, due to keySharingInfo not being updated, should only reattempt processEvent`() = runBlocking {

        setUpReceiveAndAcknowledgeTestResultIfNeededWithStaticKeySharingInfo(null)

        testSubject.submitTestResultAndKeys()

        coVerifyOrder {
            observer.onChanged(Loading)
            acknowledgeTestResult(testResult)
            observer.onChanged(Error(IllegalStateException("keysSharingInfo not set")))
        }

        testSubject.submitTestResultAndKeys()
        coVerify(exactly = 1) { acknowledgeTestResult(testResult) }
        coVerify(exactly = 1) { isolationStateMachine.processEvent(OnTestResultAcknowledge(testResult)) }
    }

    @Test
    fun `onBackPressed called, with testKitType null should finish`() {
        val testSubjectWithoutTestKitType = createCustomTestSubject(selfReportTestQuestions.copy(testKitType = null))
        testSubjectWithoutTestKitType.navigate().observeForever(navigationStateObserver)

        testSubjectWithoutTestKitType.onBackPressed()
        coVerify {
            navigationStateObserver.onChanged(Finish)
        }
    }

    @Test
    fun `onBackPressed called, with keySharingInfo unchanged should finish`() {
        coEvery { keySharingInfoProvider.keySharingInfo } returns null

        testSubject.onBackPressed()
        coVerify {
            navigationStateObserver.onChanged(Finish)
        }
    }

    @Test
    fun `onBackPressed called, after key submission failed, should navigate to Thank You`() {
        coEvery { transmissionRiskLevelApplier.applyTransmissionRiskLevels(any(), any()) } returns listOf(key1)
        val throwable = IOException()
        coEvery { submitTemporaryExposureKeys(listOf(key1), isPrivateJourney = true, testKit = selfReportTestQuestions.testKitType.toString()) } returns
                Failure(throwable)
        setUpDefaultReceiveAndAcknowledgeTestResultIfNeeded()

        testSubject.submitTestResultAndKeys()

        coVerifyOrder {
            observer.onChanged(Loading)
            acknowledgeTestResult(testResult)
            observer.onChanged(Error(throwable))
        }

        testSubject.onBackPressed()
        coVerifyOrder {
            keySharingInfoProvider.setHasDeclinedSharingKeys()
            navigationStateObserver.onChanged(ThankYou(hasReported = true, hasSharedSuccessfully = false))
        }
    }

    private fun setUpDefaultReceiveAndAcknowledgeTestResultIfNeeded() {
        coEvery { keySharingInfoProvider.keySharingInfo } returns null andThen keySharingInfo
    }

    private fun setUpReceiveAndAcknowledgeTestResultIfNeededWithStaticKeySharingInfo(keySharingInfo: KeySharingInfo?) {
        coEvery { keySharingInfoProvider.keySharingInfo } returns keySharingInfo
    }

    private fun attemptToShareKeysButKeySharingInfoWasNotUpdated(keySharingInfo: KeySharingInfo?) {
        setUpReceiveAndAcknowledgeTestResultIfNeededWithStaticKeySharingInfo(keySharingInfo)

        testSubject.submitTestResultAndKeys()
        coVerify {
            observer.onChanged(Loading)
            observer.onChanged(Error(IllegalStateException("keysSharingInfo not set")))
        }
    }

    private fun intervalStart(date: String): Int {
        val millisIn10Minutes = Duration.ofMinutes(10).toMillis()
        return (Instant.parse(date).toEpochMilli() / millisIn10Minutes).toInt()
    }

    private fun createCustomTestSubject(selfReportTestQuestions: SelfReportTestQuestions): SelfReportSubmitTestResultAndKeysProgressViewModel {
        return SelfReportSubmitTestResultAndKeysProgressViewModel(
            submitTemporaryExposureKeys = submitTemporaryExposureKeys,
            transmissionRiskLevelApplier = transmissionRiskLevelApplier,
            keySharingInfoProvider = keySharingInfoProvider,
            clock = fixedClock,
            isolationStateMachine = isolationStateMachine,
            acknowledgeTestResult = acknowledgeTestResult,
            analyticsEventProcessor = analyticsEventProcessor,
            questions = selfReportTestQuestions
        )
    }
}
