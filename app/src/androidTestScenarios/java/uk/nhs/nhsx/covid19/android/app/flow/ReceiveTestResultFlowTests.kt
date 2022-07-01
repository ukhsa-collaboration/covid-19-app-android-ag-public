package uk.nhs.nhsx.covid19.android.app.flow

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.OrderTest
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ShareKeys
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ShareKeysAndBookTest
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.DIAGNOSIS_KEY_SUBMISSION_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.TestResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfiguration
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.addTestResult
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext.Companion.ENGLISH_LOCAL_AUTHORITY
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ConfirmatoryTestCompletionStatus
import uk.nhs.nhsx.covid19.android.app.testordering.ConfirmatoryTestCompletionStatus.COMPLETED
import uk.nhs.nhsx.covid19.android.app.testordering.ConfirmatoryTestCompletionStatus.COMPLETED_AND_CONFIRMED
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderPollingConfig
import uk.nhs.nhsx.covid19.android.app.testordering.toRelevantVirologyTestResult
import uk.nhs.nhsx.covid19.android.app.util.IsolationChecker
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ReceiveTestResultFlowTests : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val testResultRobot = TestResultRobot(testAppContext.app)
    private val shareKeysAndBookTest = ShareKeysAndBookTest(testAppContext.app)
    private val shareKeys = ShareKeys()
    private val orderTest = OrderTest(this)
    private val isolationHelper = IsolationHelper(testAppContext.clock)
    private val isolationChecker = IsolationChecker(testAppContext)

    @Before
    fun setUp() {
        testAppContext.setLocalAuthority(ENGLISH_LOCAL_AUTHORITY)
    }

    @Test
    fun whenIndexCase_withoutPreviousTest_whenAcknowledgingConfirmedNegativeTest_showNegativeWontBeInIsolation_andEndIsolation() {
        setSelfAssessmentIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveConfirmedTestResult(NEGATIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysNegativeWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertExpiredIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenIndexCase_withoutPrevTest_whenAcknowledgingConfirmedNegTestOlderThanSymptomsOnsetDate_showNegAfterPosOrSymptomaticWillBeInIsolation_andContinueIsolation() {
        val isolation = setSelfAssessmentIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(
            NEGATIVE,
            diagnosisKeySubmissionSupported = true,
            testEndDate = isolation.selfAssessment!!.assumedOnsetDate
                .atStartOfDay().toInstant(ZoneOffset.UTC)
                .minus(1, ChronoUnit.DAYS)
        )
        waitFor { testResultRobot.checkActivityDisplaysNegativeAfterPositiveOrSymptomaticWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertActiveIndexNoContact()
        checkNoRelevantTestResult()
    }

    @Test
    fun whenIndexCase_withoutPreviousTest_whenAcknowledgingConfirmedVoidTest_showVoidWillBeInIsolation_andContinueIsolation() {
        setSelfAssessmentIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(VOID, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysVoidWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()

        isolationChecker.assertActiveIndexNoContact()
        checkNoRelevantTestResult()
    }

    @Test
    fun whenIndexCase_withoutPreviousTest_whenAcknowledgingConfirmedPositiveTest_showPositiveContinueIsolation_andContinueIsolation_shareKeys() {
        setSelfAssessmentIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation(ENGLAND) }

        testResultRobot.clickIsolationActionButton()
        shareKeys()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenIndexCase_withoutPreviousTest_whenAcknowledgingConfirmedPositiveTest_showPositiveContinueIsolation_andContinueIsolation_withoutKeysSharing() {
        setSelfAssessmentIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = false)
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation(ENGLAND) }

        testResultRobot.clickIsolationActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenIndexCase_withoutPreviousTest_whenAcknowledgingIndicativePositiveTest_showPositiveWillBeInIsolationAndOrderTest_andContinueIsolation_orderTest() {
        setSelfAssessmentIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePosTestResultWithKeySubmissionNotSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation(ENGLAND) }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertActiveIndexNoContact()
        checkNoRelevantTestResult() // indicative tests are ignored when already in index case
    }

    @Test
    fun whenContactCase_withoutPreviousTest_whenAcknowledgingIndicativePositiveTest_showPositiveWillBeInIsolationAndOrderTest_andContinueIsolation_orderTest() {
        setContactCaseIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionNotSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation(ENGLAND) }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertActiveIndexAndContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenContactCase_withoutPreviousTest_whenAcknowledgingIndicativePosTest_shouldOfferFollowupTestFalse_showPosWillBeInIsolationAndOrderTest_andContinueIsolation_orderTest() {
        setContactCaseIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionNotSupported(shouldOfferFollowUpTest = false)
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation(ENGLAND) }

        testResultRobot.clickIsolationActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertActiveIndexAndContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenIndexCase_withPreviousConfirmedPosTest_whenAcknowledgingConfirmedNegTest_showNegAfterPosOrSymptomaticWillBeInIsolation_andContinueIsolation() {
        val previousTest = setSelfAssessmentIsolationWithPositiveTest(
            requiresConfirmatoryTest = false,
            shouldOfferFollowUpTest = false
        )

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(NEGATIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysNegativeAfterPositiveOrSymptomaticWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultPreserved(previousTest)
    }

    @Test
    fun whenIndexCase_withPreviousConfirmedPositiveTest_whenAcknowledgingConfirmedVoidTest_showVoidWillBeInIsolation_andContinueIsolation() {
        val previousTest = setSelfAssessmentIsolationWithPositiveTest(
            requiresConfirmatoryTest = false,
            shouldOfferFollowUpTest = false
        )

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(VOID, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysVoidWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultPreserved(previousTest)
    }

    @Test
    fun whenIndexCase_withPreviousConfirmedPositiveTest_whenAcknowledgingConfirmedPositiveTest_showPositiveContinueIsolation_andContinueIsolation() {
        val previousTest = setSelfAssessmentIsolationWithPositiveTest(
            requiresConfirmatoryTest = false,
            shouldOfferFollowUpTest = false
        )

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation(ENGLAND) }

        testResultRobot.clickIsolationActionButton()
        shareKeys()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultPreserved(previousTest)
    }

    @Test
    fun whenIndexCase_withPreviousConfirmedPositiveTest_whenAcknowledgingIndicativePositiveTest_showPositiveContinueIsolationNoChange_andContinueIsolation() {
        val previousTest = setSelfAssessmentIsolationWithPositiveTest(
            requiresConfirmatoryTest = false,
            shouldOfferFollowUpTest = false
        )

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePosTestResultWithKeySubmissionNotSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation(ENGLAND) }

        testResultRobot.clickIsolationActionButton()
        waitFor { statusRobot.checkActivityIsDisplayed() }

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultPreserved(previousTest)
    }

    @Test
    fun whenContactCase_withPrevConfirmedPosTestFromBeforeCurrentIsolation_onAcknowledgingIndicativePosTest_showPosWillBeInIsolationAndOrderTest_andContinueIsolation_orderTest() {
        setContactCaseIsolationWithExpiredPositiveTest(
            requiresConfirmatoryTest = false,
            shouldOfferFollowUpTest = false
        )

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionNotSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation(ENGLAND) }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        isolationChecker.assertActiveIndexAndContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenIndexCase_withPreviousIndicativePosTest_whenAcknowledgingIndicativePosTest_showPosWillBeInIsolationAndOrderTest_andContinueIsolation_orderTest() {
        val previousTest =
            setSelfAssessmentIsolationWithPositiveTest(requiresConfirmatoryTest = true, shouldOfferFollowUpTest = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePosTestResultWithKeySubmissionNotSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation(ENGLAND) }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultPreserved(previousTest)
    }

    @Test
    fun whenIndexCase_withPreviousIndicativePositiveTest_whenAcknowledgingConfirmedNegativeTest_showNegativeWontBeInIsolation_andNoIsolation() {
        setSelfAssessmentIsolationWithPositiveTest(requiresConfirmatoryTest = true, shouldOfferFollowUpTest = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveConfirmedTestResult(NEGATIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysNegativeWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertExpiredIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenIndexCase_withPrevIndicativePosTest_onAcknowledgingConfirmedNegTestOlderThanIndicative_showNegAfterPosOrSymptomaticWillBeInIsolation_andContIsolation() {
        val previousTest =
            setSelfAssessmentIsolationWithPositiveTest(requiresConfirmatoryTest = true, shouldOfferFollowUpTest = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(
            NEGATIVE,
            diagnosisKeySubmissionSupported = true,
            testEndDate = testEndDate.minus(1, ChronoUnit.DAYS)
        )
        waitFor { testResultRobot.checkActivityDisplaysNegativeAfterPositiveOrSymptomaticWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultPreserved(previousTest)
    }

    @Test
    fun whenExpiredIndexCase_withPrevConfirmedNegTest_onAcknowledgingIndicativePosTestOlderThanNegative_showPositiveWillBeInIsolation_andStartIsolation() {
        val onsetDate = testEndDate.minus(2, ChronoUnit.DAYS)
        val previousTest = setExpiredSelfAssessmentIsolationWithNegativeTest(
            onsetDate = onsetDate.toLocalDate(testAppContext.clock.zone)
        )

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionNotSupported(
            testEndDate = onsetDate.minus(3, ChronoUnit.DAYS),
            confirmatoryDayLimit = 2
        )

        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(ENGLAND) }

        testResultRobot.clickIsolationActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertActiveIndexNoContact()

        checkRelevantTestResultUpdatedAndCompleted(testResponse, previousTest.testEndDate, COMPLETED)
    }

    @Test
    fun whenExpiredIndexCase_withPrevConfirmedNegTest_onAcknowledgingIndicativePosTestOlderThanNegative_showPositiveWontBeInIsolation_andNoIsolation() {
        val onsetInstant = testEndDate.minus(DurationDays().england.indexCaseSinceSelfDiagnosisOnset.toLong(), ChronoUnit.DAYS)
        val onsetDate = onsetInstant.toLocalDate(testAppContext.clock.zone)
        val previousTest = setExpiredSelfAssessmentIsolationWithNegativeTest(
            selfAssessmentDate = onsetDate,
            onsetDate = onsetDate
        )

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionNotSupported(
            testEndDate = onsetInstant.minus(3, ChronoUnit.DAYS),
            confirmatoryDayLimit = 2
        )

        waitFor { testResultRobot.checkActivityDisplaysPositiveWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertExpiredIndexNoContact()

        checkRelevantTestResultUpdatedAndCompleted(testResponse, previousTest.testEndDate, COMPLETED)
    }

    @Test
    fun whenExpiredIndexCase_withPrevConfirmedNegTest_onAcknowledgingIndicativePosTestOlderThanNegative_showPositiveWontBeInIsolation_andNoIsolation_shareKeys() {
        val onsetInstant = testEndDate.minus(DurationDays().england.indexCaseSinceSelfDiagnosisOnset.toLong(), ChronoUnit.DAYS)
        val onsetDate = onsetInstant.toLocalDate(testAppContext.clock.zone)
        val previousTest = setExpiredSelfAssessmentIsolationWithNegativeTest(
            selfAssessmentDate = onsetDate,
            onsetDate = onsetDate
        )

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionSupported(
            testEndDate = onsetInstant.minus(3, ChronoUnit.DAYS),
            confirmatoryDayLimit = 2
        )

        waitFor { testResultRobot.checkActivityDisplaysPositiveWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()
        shareKeys()

        isolationChecker.assertExpiredIndexNoContact()

        checkRelevantTestResultUpdatedAndCompleted(testResponse, previousTest.testEndDate, COMPLETED)
    }

    @Test
    fun whenExpiredIndexCase_withPrevConfirmedNegTest_onAcknowledgingIndicativePosTestOlderThanNegative_showPositiveWillBeInIsolation_andStartIsolation_shareKeys() {
        val onsetDate = testEndDate.minus(2, ChronoUnit.DAYS)
        val previousTest =
            setExpiredSelfAssessmentIsolationWithNegativeTest(onsetDate.toLocalDate(testAppContext.clock.zone))

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionSupported(
            testEndDate = onsetDate.minus(3, ChronoUnit.DAYS),
            confirmatoryDayLimit = 2
        )

        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(ENGLAND) }

        testResultRobot.clickIsolationActionButton()
        shareKeys()

        isolationChecker.assertActiveIndexNoContact()

        checkRelevantTestResultUpdatedAndCompleted(testResponse, previousTest.testEndDate, COMPLETED)
    }

    @Test
    fun whenContactCase_withPrevConfirmedNegTest_onAcknowledgingIndicativePosTestOlderThanNegative_showPositiveContinueIsolation_andContinueIsolation() {
        val previousTest = setContactCaseIsolationWithNegativeTest()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionNotSupported(
            testEndDate = testEndDate.minus(3, ChronoUnit.DAYS),
            confirmatoryDayLimit = 2
        )

        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation(ENGLAND) }

        testResultRobot.clickIsolationActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertActiveIndexAndContact()

        checkRelevantTestResultUpdatedAndCompleted(testResponse, previousTest.testEndDate, COMPLETED)
    }

    @Test
    fun whenDefault_withPrevConfirmedNegTest_onAcknowledgingIndicativePosTestOlderThanNegative_showPositiveWillBeInIsolation_andStartIsolation() {
        val previousTest = setNoIsolationWithNegativeTest()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionNotSupported(
            testEndDate = testEndDate.minus(3, ChronoUnit.DAYS),
            confirmatoryDayLimit = 2
        )

        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(ENGLAND) }

        testResultRobot.clickIsolationActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertActiveIndexNoContact()

        checkRelevantTestResultUpdatedAndCompleted(testResponse, previousTest.testEndDate, COMPLETED)
    }

    @Test
    fun whenDefault_withPrevConfirmedNegTest_onAcknowledgingIndicativePosTestOlderThanNegative_showPositiveWontBeInIsolation_andNoIsolation() {
        val previousTest = setNoIsolationWithNegativeTest()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionNotSupported(
            testEndDate = testEndDate.minus(DurationDays().england.indexCaseSinceTestResultEndDate.toLong(), ChronoUnit.DAYS),
            confirmatoryDayLimit = 2
        )

        waitFor { testResultRobot.checkActivityDisplaysPositiveWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertExpiredIndexNoContact()

        checkRelevantTestResultUpdatedAndCompleted(testResponse, previousTest.testEndDate, COMPLETED)
    }

    @Test
    fun whenIndexCase_withPreviousIndicativePositiveTest_whenAcknowledgingConfirmedNegativeTestOutsideConfirmatoryDayLimit_showNegativeWillBeInIsolation_andContIsolation() {
        val indicativePositiveTestResult = setSelfAssessmentIsolationWithPositiveTest(
            requiresConfirmatoryTest = true,
            shouldOfferFollowUpTest = true,
            confirmatoryDayLimit = 2
        )

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(
            NEGATIVE,
            diagnosisKeySubmissionSupported = false,
            testEndDate = testEndDate.plus(3, ChronoUnit.DAYS)
        )
        waitFor { testResultRobot.checkActivityDisplaysNegativeWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultPreserved(
            indicativePositiveTestResult.copy(confirmatoryTestCompletionStatus = COMPLETED),
            confirmatoryTestCompletionStatus = COMPLETED
        )
    }

    @Test
    fun whenContactCase_withPrevIndicativePosTestFromBeforeCurrentIsolation_onAcknowledgingIndicativePosTest_showPosWillBeInIsolationAndOrderTest_andContinueIsolation_orderTest() {
        setContactCaseIsolationWithExpiredPositiveTest(requiresConfirmatoryTest = true, shouldOfferFollowUpTest = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionNotSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation(ENGLAND) }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        isolationChecker.assertActiveIndexAndContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenNotIsolating_withPrevExpiredIndicativePosTest_onAcknowledgingConfirmedNegativeTest_showNegativeNotInIsolation_andNoIsolation() {
        val indicativePositiveTestResult = setExpiredPositiveTestIsolation(
            requiresConfirmatoryTest = true,
            shouldOfferFollowUpTest = true,
            confirmatoryDayLimit = 2
        )

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(NEGATIVE, diagnosisKeySubmissionSupported = false)
        waitFor { testResultRobot.checkActivityDisplaysNegativeAlreadyNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        isolationChecker.assertExpiredIndexNoContact()
        checkRelevantTestResultPreserved(
            acknowledgedTestResult = indicativePositiveTestResult.copy(confirmatoryTestCompletionStatus = COMPLETED),
            confirmatoryTestCompletionStatus = COMPLETED
        )
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedNegativeTest_showNegativeNotInIsolation_andNoIsolation() {
        setSelfAssessmentIsolation(expired = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveConfirmedTestResult(NEGATIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysNegativeAlreadyNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertExpiredIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedNegativeTestOlderThanSymptomsOnsetDate_showNegativeNotInIsolation_andNoIsolation() {
        val state = setSelfAssessmentIsolation(expired = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(
            NEGATIVE,
            diagnosisKeySubmissionSupported = true,
            testEndDate = state.selfAssessment!!.assumedOnsetDate
                .atStartOfDay().toInstant(ZoneOffset.UTC)
                .minus(1, ChronoUnit.DAYS)
        )
        waitFor { testResultRobot.checkActivityDisplaysNegativeAlreadyNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertExpiredIndexNoContact()
        checkNoRelevantTestResult()
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedVoidTest_showVoidNotInIsolation_andNoIsolation() {
        setSelfAssessmentIsolation(expired = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(VOID, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysVoidNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        isolationChecker.assertExpiredIndexNoContact()
        checkNoRelevantTestResult()
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWontBeInIsolation_andNoIsolation_shareKeys() {
        setSelfAssessmentIsolation(expired = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()
        shareKeys()

        isolationChecker.assertExpiredIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWontBeInIsolation_andNoIsolation_withoutKeysSharing() {
        setSelfAssessmentIsolation(expired = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = false)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertExpiredIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withoutPreviousTest_whenAcknowledgingIndicativePositiveTest_showPositiveWillBeInIsolationAndOrderTest_andStartIsolation_orderTest() {
        setSelfAssessmentIsolation(expired = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionNotSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest(ENGLAND) }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedPositiveTest_whenAcknowledgingConfirmedNegativeTest_showNegativeNotInIsolation_andNoIsolation() {
        val previousTest =
            setExpiredPositiveTestIsolation(requiresConfirmatoryTest = false, shouldOfferFollowUpTest = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(NEGATIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysNegativeAlreadyNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertExpiredIndexNoContact()
        checkRelevantTestResultPreserved(previousTest)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedPositiveTest_whenAcknowledgingConfirmedVoidTest_showVoidNotInIsolation_andNoIsolation() {
        val previousTest =
            setExpiredPositiveTestIsolation(requiresConfirmatoryTest = false, shouldOfferFollowUpTest = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(VOID, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysVoidNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        isolationChecker.assertExpiredIndexNoContact()
        checkRelevantTestResultPreserved(previousTest)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedPositiveTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWontBeInIsolation_andNoIsolation_shareKeys() {
        val previousTest =
            setExpiredPositiveTestIsolation(requiresConfirmatoryTest = false, shouldOfferFollowUpTest = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()
        shareKeys()

        isolationChecker.assertExpiredIndexNoContact()
        checkRelevantTestResultPreserved(previousTest)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousIndicativePositiveTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWontBeInIsolation_andNoIsolation_shareKeys() {
        val previousTest =
            setExpiredPositiveTestIsolation(requiresConfirmatoryTest = true, shouldOfferFollowUpTest = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()
        shareKeys()

        isolationChecker.assertExpiredIndexNoContact()
        checkRelevantTestResultPreserved(
            acknowledgedTestResult = previousTest.copy(confirmatoryTestCompletionStatus = COMPLETED_AND_CONFIRMED),
            confirmatoryTestCompletionStatus = COMPLETED_AND_CONFIRMED
        )
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedPositiveTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWontBeInIsolation_andNoIsolation_withoutKeysSharing() {
        val previousTest =
            setExpiredPositiveTestIsolation(requiresConfirmatoryTest = false, shouldOfferFollowUpTest = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = false)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertExpiredIndexNoContact()
        checkRelevantTestResultPreserved(previousTest)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousIndicativePositiveTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWontBeInIsolation_andNoIsolation() {
        val previousTest =
            setExpiredPositiveTestIsolation(requiresConfirmatoryTest = true, shouldOfferFollowUpTest = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = false)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertExpiredIndexNoContact()
        checkRelevantTestResultPreserved(
            acknowledgedTestResult = previousTest.copy(confirmatoryTestCompletionStatus = COMPLETED_AND_CONFIRMED),
            confirmatoryTestCompletionStatus = COMPLETED_AND_CONFIRMED
        )
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedPosTest_whenAcknowledgingIndicativePosTest_showPosWillBeInIsolationAndOrderTest_andStartIsolation_orderTest() {
        setExpiredPositiveTestIsolation(requiresConfirmatoryTest = false, shouldOfferFollowUpTest = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionNotSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest(ENGLAND) }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedNegativeTest_whenAcknowledgingConfirmedNegativeTest_showNegativeNotInIsolation_andNoIsolation() {
        val previousTest = setExpiredSelfAssessmentIsolationWithNegativeTest()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(NEGATIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysNegativeAlreadyNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertExpiredIndexNoContact()
        checkRelevantTestResultPreserved(previousTest)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedNegativeTest_whenAcknowledgingConfirmedVoidTest_showVoidNotInIsolation_andNoIsolation() {
        val previousTest = setExpiredSelfAssessmentIsolationWithNegativeTest()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(VOID, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysVoidNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        isolationChecker.assertExpiredIndexNoContact()
        checkRelevantTestResultPreserved(previousTest)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedNegativeTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWillBeInIsolation_andStartIsolation_shareKeys() {
        setExpiredSelfAssessmentIsolationWithNegativeTest()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(ENGLAND) }

        testResultRobot.clickIsolationActionButton()
        shareKeys()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedNegTest_whenAcknowledgingConfirmedPosTest_showPosWillBeInIsolation_andStartIsolation_withoutKeysSharing() {
        setExpiredSelfAssessmentIsolationWithNegativeTest()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = false)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(ENGLAND) }

        testResultRobot.clickIsolationActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedNegTest_whenAcknowledgingIndicativePosTest_showPosWillBeInIsolationAndOrderTest_andStartIsolation_orderTest() {
        setExpiredSelfAssessmentIsolationWithNegativeTest()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionNotSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest(ENGLAND) }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithoutPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWillBeInIsolation_andStartIsolation_shareKeys() {
        testAppContext.setState(isolationHelper.neverInIsolation())

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(ENGLAND) }

        testResultRobot.clickIsolationActionButton()
        shareKeys()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithoutPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWillBeInIsolation_andStartIsolation_withoutKeysSharing() {
        testAppContext.setState(isolationHelper.neverInIsolation())

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = false)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(ENGLAND) }

        testResultRobot.clickIsolationActionButton()
        waitFor { statusRobot.checkActivityIsDisplayed() }

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithoutPreviousIsolation_withoutPreviousTest_whenAcknowledgingIndicativePositiveTest_showPositiveWillBeInIsolationAndOrderTest_andStartIsolation_orderTest() {
        testAppContext.setState(isolationHelper.neverInIsolation())

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionNotSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest(ENGLAND) }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithoutPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedNegativeTest_showNegativeNotInIsolation_andNoIsolation() {
        testAppContext.setState(isolationHelper.neverInIsolation())

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveConfirmedTestResult(NEGATIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysNegativeAlreadyNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertNeverIsolating()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithoutPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedVoidTest_showVoidNotInIsolation_andNoIsolation() {
        testAppContext.setState(isolationHelper.neverInIsolation())

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(VOID, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysVoidNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        isolationChecker.assertNeverIsolating()
        checkNoRelevantTestResult()
    }

    @Test
    fun whenIndexCase_withoutPreviousTest_whenAcknowledgingIndicativePositiveTest_showPositiveContinueIsolation_andContinueIsolation_shareKeysAndBookTest() {
        setSelfAssessmentIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePosTestResultWithKeySubmissionSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation(ENGLAND) }

        shareKeysAndBookTest()

        isolationChecker.assertActiveIndexNoContact()
        checkNoRelevantTestResult()
    }

    @Test
    fun whenContactCase_withoutPreviousTest_whenAcknowledgingIndicativePositiveTest_showPositiveContinueIsolation_andContinueIsolation_shareKeys() {
        setContactCaseIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation(ENGLAND) }
        shareKeysAndBookTest()

        isolationChecker.assertActiveIndexAndContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenIndexCase_withPreviousConfirmedPositiveTest_whenAcknowledgingIndicativePositiveTest_showPositiveContinueIsolation_andContinueIsolation_shareKeys() {
        val previousTest = setSelfAssessmentIsolationWithPositiveTest(
            requiresConfirmatoryTest = false,
            shouldOfferFollowUpTest = false
        )

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePosTestResultWithKeySubmissionSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation(ENGLAND) }
        testResultRobot.clickIsolationActionButton()

        shareKeys()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultPreserved(previousTest)
    }

    @Test
    fun whenIndexCase_withPreviousConfirmedPositiveTest_whenAcknowledgingIndicativePosTestOlderThanConfirmed_showPositiveContinueIsolation_andContinueIsolation_shareKeys() {
        val previousTest = setSelfAssessmentIsolationWithPositiveTest(
            requiresConfirmatoryTest = false,
            shouldOfferFollowUpTest = false
        )

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionSupported(
            testEndDate = testEndDate.minus(1, ChronoUnit.DAYS)
        )
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation(ENGLAND) }
        testResultRobot.clickIsolationActionButton()

        shareKeys()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdatedAndCompleted(
            testResponse,
            confirmedDate = previousTest.testEndDate,
            confirmatoryTestCompletionStatus = COMPLETED_AND_CONFIRMED
        )
    }

    @Test
    fun whenContactCase_withPrevConfirmedPosTestFromBeforeCurrentIsolation_onAcknowledgingIndicativePosTest_showPosContinueIsolation_andContinueIsolation_shareKeysAndBookTest() {
        setContactCaseIsolationWithExpiredPositiveTest(
            requiresConfirmatoryTest = false,
            shouldOfferFollowUpTest = false
        )

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation(ENGLAND) }

        shareKeysAndBookTest()

        isolationChecker.assertActiveIndexAndContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenContactCase_withPrevIndicativePosTestFromBeforeCurrentIsolation_onAcknowledgingIndicativePosTest_showPositiveContinueIsolation_shareKeysAndBookTest() {
        setContactCaseIsolationWithExpiredPositiveTest(requiresConfirmatoryTest = true, shouldOfferFollowUpTest = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation(ENGLAND) }

        shareKeysAndBookTest()

        isolationChecker.assertActiveIndexAndContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withoutPreviousTest_whenAcknowledgingIndicativePositiveTest_showPositiveWillBeInIsolation_andStartIsolation_shareKeysAndBookTest() {
        setSelfAssessmentIsolation(expired = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(ENGLAND) }

        shareKeysAndBookTest()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedPosTest_whenAcknowledgingIndicativePosTest_showPositiveWillBeInIsolation_andStartIsolation_shareKeysAndBookTest() {
        setExpiredPositiveTestIsolation(requiresConfirmatoryTest = false, shouldOfferFollowUpTest = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(ENGLAND) }

        shareKeysAndBookTest()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedNegTest_whenAcknowledgingIndicativePosTest_showPosWillBeInIsolation_andStartIsolation_shareKeysAndBookTest() {
        setExpiredSelfAssessmentIsolationWithNegativeTest()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(ENGLAND) }

        shareKeysAndBookTest()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithoutPreviousIsolation_withoutPreviousTest_whenAcknowledgingIndicativePositiveTest_showPositiveWillBeInIsolation_andStartIsolation_shareKeysAndBookTest() {
        testAppContext.setState(isolationHelper.neverInIsolation())

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(ENGLAND) }

        shareKeysAndBookTest()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    private fun setSelfAssessmentIsolation(expired: Boolean = false): IsolationState {
        val isolationStart =
            if (expired) previousIsolationStart
            else isolationStart
        val state = SelfAssessment(selfAssessmentDate = isolationStart)
            .asIsolation(hasAcknowledgedEndOfIsolation = expired)
        testAppContext.setState(state)
        return state
    }

    private fun setExpiredPositiveTestIsolation(
        requiresConfirmatoryTest: Boolean,
        shouldOfferFollowUpTest: Boolean,
        confirmatoryDayLimit: Int? = null
    ): AcknowledgedTestResult {
        val previousTest = previousTest(
            testResult = RelevantVirologyTestResult.POSITIVE,
            requiresConfirmatoryTest = requiresConfirmatoryTest,
            shouldOfferFollowUpTest = shouldOfferFollowUpTest,
            testEndDate = previousIsolationStart,
            confirmatoryDayLimit = confirmatoryDayLimit
        )

        testAppContext.setState(
            previousTest.asIsolation(hasAcknowledgedEndOfIsolation = true)
        )

        return previousTest
    }

    private fun setSelfAssessmentIsolationWithPositiveTest(
        requiresConfirmatoryTest: Boolean,
        shouldOfferFollowUpTest: Boolean,
        confirmatoryDayLimit: Int? = null
    ): AcknowledgedTestResult {
        val previousTest = previousTest(
            testResult = RelevantVirologyTestResult.POSITIVE,
            requiresConfirmatoryTest = requiresConfirmatoryTest,
            shouldOfferFollowUpTest = shouldOfferFollowUpTest,
            testEndDate = testEndDate.toLocalDate(testAppContext.clock.zone),
            confirmatoryDayLimit = confirmatoryDayLimit
        )

        testAppContext.setState(
            isolationHelper.selfAssessment().asIsolation()
                .addTestResult(previousTest)
        )

        return previousTest
    }

    private fun setExpiredSelfAssessmentIsolationWithNegativeTest(
        selfAssessmentDate: LocalDate = LocalDate.now(testAppContext.clock).minusDays(2),
        onsetDate: LocalDate? = null
    ): AcknowledgedTestResult {
        val previousTest = previousNegativeTest()

        testAppContext.setState(
            SelfAssessment(
                selfAssessmentDate = selfAssessmentDate,
                onsetDate = onsetDate
            )
                .asIsolation(hasAcknowledgedEndOfIsolation = true)
                .addTestResult(testResult = previousTest)
        )

        return previousTest
    }

    private fun setNoIsolationWithNegativeTest(): AcknowledgedTestResult {
        val previousTest = previousNegativeTest()

        testAppContext.setState(
            isolationHelper.neverInIsolation().copy(
                testResult = previousTest
            ).copy(hasAcknowledgedEndOfIsolation = true)
        )

        return previousTest
    }

    private fun setContactCaseIsolation() {
        testAppContext.setState(
            isolationHelper.contact(
                exposureDate = isolationStart,
                notificationDate = isolationStart
            ).asIsolation()
        )
    }

    private fun setContactCaseIsolationWithExpiredPositiveTest(
        requiresConfirmatoryTest: Boolean,
        shouldOfferFollowUpTest: Boolean
    ) {
        testAppContext.setState(
            IsolationState(
                isolationConfiguration = IsolationConfiguration(),
                contact = isolationHelper.contact(
                    exposureDate = isolationStart,
                    notificationDate = isolationStart
                ),
                testResult = previousTest(
                    testResult = RelevantVirologyTestResult.POSITIVE,
                    requiresConfirmatoryTest = requiresConfirmatoryTest,
                    shouldOfferFollowUpTest = shouldOfferFollowUpTest,
                    testEndDate = isolationStart.minusDays(12)
                )
            )
        )
    }

    private fun setContactCaseIsolationWithNegativeTest(): AcknowledgedTestResult {
        val previousNegativeTest = previousNegativeTest()
        testAppContext.setState(
            IsolationState(
                isolationConfiguration = IsolationConfiguration(),
                contact = isolationHelper.contact(
                    exposureDate = isolationStart,
                    notificationDate = isolationStart
                ),
                testResult = previousNegativeTest
            )
        )
        return previousNegativeTest
    }

    private fun previousNegativeTest(): AcknowledgedTestResult =
        previousTest(
            testResult = RelevantVirologyTestResult.NEGATIVE,
            requiresConfirmatoryTest = false,
            shouldOfferFollowUpTest = false,
            testEndDate = testEndDate.toLocalDate(testAppContext.clock.zone)
        )

    private fun previousTest(
        testResult: RelevantVirologyTestResult,
        requiresConfirmatoryTest: Boolean,
        shouldOfferFollowUpTest: Boolean,
        testEndDate: LocalDate,
        confirmatoryDayLimit: Int? = null
    ): AcknowledgedTestResult =
        AcknowledgedTestResult(
            testEndDate,
            testResult,
            testKitType = LAB_RESULT,
            requiresConfirmatoryTest = requiresConfirmatoryTest,
            shouldOfferFollowUpTest = shouldOfferFollowUpTest,
            acknowledgedDate = testEndDate,
            confirmatoryDayLimit = confirmatoryDayLimit
        )

    private fun receiveConfirmedTestResult(
        testResult: VirologyTestResult,
        diagnosisKeySubmissionSupported: Boolean,
        testEndDate: Instant? = null
    ): TestResponse {
        return receiveTestResult(
            testResult,
            LAB_RESULT,
            testEndDate,
            diagnosisKeySubmissionSupported = diagnosisKeySubmissionSupported,
            requiresConfirmatoryTest = false,
            shouldOfferFollowUpTest = false,
        )
    }

    private fun receiveIndicativePosTestResultWithKeySubmissionNotSupported(
        testEndDate: Instant? = null,
        confirmatoryDayLimit: Int? = null,
        shouldOfferFollowUpTest: Boolean = true
    ): TestResponse {
        return receiveTestResult(
            POSITIVE,
            RAPID_RESULT,
            testEndDate,
            diagnosisKeySubmissionSupported = false,
            requiresConfirmatoryTest = true,
            shouldOfferFollowUpTest = shouldOfferFollowUpTest,
            confirmatoryDayLimit = confirmatoryDayLimit
        )
    }

    private fun receiveIndicativePosTestResultWithKeySubmissionSupported(
        testEndDate: Instant? = null,
        confirmatoryDayLimit: Int? = null,
    ): TestResponse {
        return receiveTestResult(
            POSITIVE,
            RAPID_RESULT,
            testEndDate,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = true,
            shouldOfferFollowUpTest = true,
            confirmatoryDayLimit = confirmatoryDayLimit
        )
    }

    private fun receiveTestResult(
        testResult: VirologyTestResult,
        testKit: VirologyTestKitType,
        testEndDate: Instant? = null,
        diagnosisKeySubmissionSupported: Boolean,
        requiresConfirmatoryTest: Boolean,
        shouldOfferFollowUpTest: Boolean,
        confirmatoryDayLimit: Int? = null
    ): TestResponse {
        testAppContext.virologyTestingApi.testEndDate = testEndDate

        val pollingConfig = TestOrderPollingConfig(
            Instant.now(),
            "pollingToken",
            DIAGNOSIS_KEY_SUBMISSION_TOKEN
        )

        testAppContext.getTestOrderingTokensProvider().add(pollingConfig)

        val testResponse = TestResponse(
            testResult,
            testKit,
            diagnosisKeySubmissionSupported = diagnosisKeySubmissionSupported,
            requiresConfirmatoryTest = requiresConfirmatoryTest,
            shouldOfferFollowUpTest = shouldOfferFollowUpTest,
            confirmatoryDayLimit = confirmatoryDayLimit
        )
        testAppContext.virologyTestingApi.testResponseForPollingToken = mutableMapOf(
            pollingConfig.testResultPollingToken to testResponse
        )

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        return testResponse
    }

    private fun checkNoRelevantTestResult() {
        val relevantTestResult = testAppContext.getCurrentState().testResult
        assertNull(relevantTestResult)
    }

    private fun checkRelevantTestResultUpdated(
        testResponse: TestResponse
    ) {
        val relevantTestResult = testAppContext.getCurrentState().testResult
        assertNotNull(relevantTestResult)
        checkRelevantTestResultUpdated(testResponse, relevantTestResult)
        assertNull(relevantTestResult.confirmedDate)
    }

    private fun checkRelevantTestResultUpdatedAndCompleted(
        testResponse: TestResponse,
        confirmedDate: LocalDate,
        confirmatoryTestCompletionStatus: ConfirmatoryTestCompletionStatus
    ) {
        val relevantTestResult = testAppContext.getCurrentState().testResult
        assertNotNull(relevantTestResult)
        checkRelevantTestResultUpdated(testResponse, relevantTestResult)
        assertEquals(confirmedDate, relevantTestResult.confirmedDate)
        assertEquals(confirmatoryTestCompletionStatus, relevantTestResult.confirmatoryTestCompletionStatus)
    }

    private fun checkRelevantTestResultUpdated(
        testResponse: TestResponse,
        relevantTestResult: AcknowledgedTestResult
    ) {
        assertEquals(testResponse.testResult.toRelevantVirologyTestResult(), relevantTestResult.testResult)
        assertEquals(testResponse.testKitType, relevantTestResult.testKitType)
        assertEquals(testResponse.requiresConfirmatoryTest, relevantTestResult.requiresConfirmatoryTest)
        assertEquals(testResponse.confirmatoryDayLimit, relevantTestResult.confirmatoryDayLimit)
    }

    private fun checkRelevantTestResultPreserved(
        acknowledgedTestResult: AcknowledgedTestResult,
        confirmatoryTestCompletionStatus: ConfirmatoryTestCompletionStatus? = null
    ) {
        val relevantTestResult = testAppContext.getCurrentState().testResult
        val confirmedDateShouldBeNull = confirmatoryTestCompletionStatus == null
        assertNotNull(relevantTestResult)
        assertEquals(acknowledgedTestResult.testEndDate, relevantTestResult.testEndDate)
        assertEquals(acknowledgedTestResult.testResult, relevantTestResult.testResult)
        assertEquals(acknowledgedTestResult.testKitType, relevantTestResult.testKitType)
        assertEquals(acknowledgedTestResult.requiresConfirmatoryTest, relevantTestResult.requiresConfirmatoryTest)
        assertEquals(confirmedDateShouldBeNull, relevantTestResult.confirmedDate == null)
        assertEquals(
            acknowledgedTestResult.confirmatoryTestCompletionStatus,
            relevantTestResult.confirmatoryTestCompletionStatus
        )
    }

    companion object {
        private val isolationStart = LocalDate.now().minus(3, ChronoUnit.DAYS)
        private val previousIsolationStart = isolationStart.minus(12, ChronoUnit.DAYS)
        private val testEndDate = isolationStart.atStartOfDay(ZoneOffset.UTC).toInstant()
            .plus(1, ChronoUnit.DAYS)
    }
}
