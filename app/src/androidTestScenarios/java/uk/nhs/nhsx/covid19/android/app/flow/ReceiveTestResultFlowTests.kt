package uk.nhs.nhsx.covid19.android.app.flow

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
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
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
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
    fun whenIndexCase_withoutPreviousTest_whenAcknowledgingConfirmedNegativeTest_showNegativeWontBeInIsolation_andEndIsolation() = notReported {
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
    fun whenIndexCase_withoutPrevTest_whenAcknowledgingConfirmedNegTestOlderThanSymptomsOnsetDate_showNegAfterPosOrSymptomaticWillBeInIsolation_andContinueIsolation() = notReported {
        val isolation = setSelfAssessmentIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(
            NEGATIVE,
            diagnosisKeySubmissionSupported = true,
            testEndDate = ((isolation.indexInfo as IndexCase).isolationTrigger as SelfAssessment).assumedOnsetDate
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
    fun whenIndexCase_withoutPreviousTest_whenAcknowledgingConfirmedVoidTest_showVoidWillBeInIsolation_andContinueIsolation() = notReported {
        setSelfAssessmentIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(VOID, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysVoidWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        isolationChecker.assertActiveIndexNoContact()
        checkNoRelevantTestResult()
    }

    @Test
    fun whenIndexCase_withoutPreviousTest_whenAcknowledgingConfirmedPositiveTest_showPositiveContinueIsolation_andContinueIsolation_shareKeys() = notReported {
        setSelfAssessmentIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation() }

        testResultRobot.clickIsolationActionButton()
        shareKeys()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenIndexCase_withoutPreviousTest_whenAcknowledgingConfirmedPositiveTest_showPositiveContinueIsolation_andContinueIsolation_withoutKeysSharing() = notReported {
        setSelfAssessmentIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = false)
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation() }

        testResultRobot.clickIsolationActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenIndexCase_withoutPreviousTest_whenAcknowledgingIndicativePositiveTest_showPositiveWillBeInIsolationAndOrderTest_andContinueIsolation_orderTest() = notReported {
        setSelfAssessmentIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePosTestResultWithKeySubmissionNotSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertActiveIndexNoContact()
        checkNoRelevantTestResult() // indicative tests are ignored when already in index case
    }

    @Test
    fun whenContactCase_withoutPreviousTest_whenAcknowledgingIndicativePositiveTest_showPositiveWillBeInIsolationAndOrderTest_andContinueIsolation_orderTest() = notReported {
        setContactCaseIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionNotSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertActiveIndexAndContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenIndexCase_withPreviousConfirmedPosTest_whenAcknowledgingConfirmedNegTest_showNegAfterPosOrSymptomaticWillBeInIsolation_andContinueIsolation() = notReported {
        val previousTest = setSelfAssessmentIsolationWithPositiveTest(requiresConfirmatoryTest = false)

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
    fun whenIndexCase_withPreviousConfirmedPositiveTest_whenAcknowledgingConfirmedVoidTest_showVoidWillBeInIsolation_andContinueIsolation() = notReported {
        val previousTest = setSelfAssessmentIsolationWithPositiveTest(requiresConfirmatoryTest = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(VOID, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysVoidWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultPreserved(previousTest)
    }

    @Test
    fun whenIndexCase_withPreviousConfirmedPositiveTest_whenAcknowledgingConfirmedPositiveTest_showPositiveContinueIsolation_andContinueIsolation() = notReported {
        val previousTest = setSelfAssessmentIsolationWithPositiveTest(requiresConfirmatoryTest = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation() }

        testResultRobot.clickIsolationActionButton()
        shareKeys()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultPreserved(previousTest)
    }

    @Test
    fun whenIndexCase_withPreviousConfirmedPositiveTest_whenAcknowledgingIndicativePositiveTest_showPositiveContinueIsolationNoChange_andContinueIsolation() = notReported {
        val previousTest = setSelfAssessmentIsolationWithPositiveTest(requiresConfirmatoryTest = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePosTestResultWithKeySubmissionNotSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolationNoChange() }

        testResultRobot.clickIsolationActionButton()
        waitFor { statusRobot.checkActivityIsDisplayed() }

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultPreserved(previousTest)
    }

    @Test
    fun whenContactCase_withPrevConfirmedPosTestFromBeforeCurrentIsolation_onAcknowledgingIndicativePosTest_showPosWillBeInIsolationAndOrderTest_andContinueIsolation_orderTest() = notReported {
        setContactCaseIsolationWithExpiredPositiveTest(requiresConfirmatoryTest = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionNotSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        isolationChecker.assertActiveIndexAndContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenIndexCase_withPreviousIndicativePosTest_whenAcknowledgingIndicativePosTest_showPosWillBeInIsolationAndOrderTest_andContinueIsolation_orderTest() = notReported {
        val previousTest = setSelfAssessmentIsolationWithPositiveTest(requiresConfirmatoryTest = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePosTestResultWithKeySubmissionNotSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultPreserved(previousTest)
    }

    @Test
    fun whenIndexCase_withPreviousIndicativePositiveTest_whenAcknowledgingConfirmedNegativeTest_showNegativeWontBeInIsolation_andNoIsolation() = notReported {
        setSelfAssessmentIsolationWithPositiveTest(requiresConfirmatoryTest = true)

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
    fun whenIndexCase_withPrevIndicativePosTest_onAcknowledgingConfirmedNegTestOlderThanIndicative_showNegAfterPosOrSymptomaticWillBeInIsolation_andContIsolation() = notReported {
        val previousTest = setSelfAssessmentIsolationWithPositiveTest(requiresConfirmatoryTest = true)

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
    fun whenIndexCase_withPreviousIndicativePositiveTest_whenAcknowledgingConfirmedNegativeTestOutsideConfirmatoryDayLimit_showNegativeWillBeInIsolation_andContIsolation() = notReported {
        val indicativePositiveTestResult = setSelfAssessmentIsolationWithPositiveTest(requiresConfirmatoryTest = true, confirmatoryDayLimit = 2)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(NEGATIVE, diagnosisKeySubmissionSupported = false, testEndDate = testEndDate.plus(3, ChronoUnit.DAYS))
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
    fun whenContactCase_withPrevIndicativePosTestFromBeforeCurrentIsolation_onAcknowledgingIndicativePosTest_showPosWillBeInIsolationAndOrderTest_andContinueIsolation_orderTest() = notReported {
        setContactCaseIsolationWithExpiredPositiveTest(requiresConfirmatoryTest = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionNotSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        isolationChecker.assertActiveIndexAndContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenNotIsolating_withPrevExpiredIndicativePosTest_onAcknowledgingConfirmedNegativeTest_showNegativeNotInIsolation_andNoIsolation() = notReported {
        val indicativePositiveTestResult = setExpiredPositiveTestIsolation(requiresConfirmatoryTest = true, confirmatoryDayLimit = 2)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(NEGATIVE, diagnosisKeySubmissionSupported = false)
        waitFor { testResultRobot.checkActivityDisplaysNegativeNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        isolationChecker.assertExpiredIndexNoContact()
        checkRelevantTestResultPreserved(
            acknowledgedTestResult = indicativePositiveTestResult.copy(confirmatoryTestCompletionStatus = COMPLETED),
            confirmatoryTestCompletionStatus = COMPLETED
        )
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedNegativeTest_showNegativeNotInIsolation_andNoIsolation() = notReported {
        setSelfAssessmentIsolation(expired = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveConfirmedTestResult(NEGATIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysNegativeNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertExpiredIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedNegativeTestOlderThanSymptomsOnsetDate_showNegativeNotInIsolation_andNoIsolation() = notReported {
        val state = setSelfAssessmentIsolation(expired = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(
            NEGATIVE,
            diagnosisKeySubmissionSupported = true,
            testEndDate = ((state.indexInfo as IndexCase).isolationTrigger as SelfAssessment).assumedOnsetDate
                .atStartOfDay().toInstant(ZoneOffset.UTC)
                .minus(1, ChronoUnit.DAYS)
        )
        waitFor { testResultRobot.checkActivityDisplaysNegativeNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertExpiredIndexNoContact()
        checkNoRelevantTestResult()
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedVoidTest_showVoidNotInIsolation_andNoIsolation() = notReported {
        setSelfAssessmentIsolation(expired = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(VOID, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysVoidNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()
        orderTest()

        isolationChecker.assertExpiredIndexNoContact()
        checkNoRelevantTestResult()
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWontBeInIsolation_andNoIsolation_shareKeys() = notReported {
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
    fun whenDefaultWithPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWontBeInIsolation_andNoIsolation_withoutKeysSharing() = notReported {
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
    fun whenDefaultWithPreviousIsolation_withoutPreviousTest_whenAcknowledgingIndicativePositiveTest_showPositiveWillBeInIsolationAndOrderTest_andStartIsolation_orderTest() = notReported {
        setSelfAssessmentIsolation(expired = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionNotSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedPositiveTest_whenAcknowledgingConfirmedNegativeTest_showNegativeNotInIsolation_andNoIsolation() = notReported {
        val previousTest = setExpiredPositiveTestIsolation(requiresConfirmatoryTest = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(NEGATIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysNegativeNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertExpiredIndexNoContact()
        checkRelevantTestResultPreserved(previousTest)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedPositiveTest_whenAcknowledgingConfirmedVoidTest_showVoidNotInIsolation_andNoIsolation() = notReported {
        val previousTest = setExpiredPositiveTestIsolation(requiresConfirmatoryTest = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(VOID, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysVoidNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()
        orderTest()

        isolationChecker.assertExpiredIndexNoContact()
        checkRelevantTestResultPreserved(previousTest)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedPositiveTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWontBeInIsolation_andNoIsolation_shareKeys() = notReported {
        val previousTest = setExpiredPositiveTestIsolation(requiresConfirmatoryTest = false)

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
    fun whenDefaultWithPreviousIsolation_withPreviousIndicativePositiveTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWontBeInIsolation_andNoIsolation_shareKeys() = notReported {
        val previousTest = setExpiredPositiveTestIsolation(requiresConfirmatoryTest = true)

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
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedPositiveTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWontBeInIsolation_andNoIsolation_withoutKeysSharing() = notReported {
        val previousTest = setExpiredPositiveTestIsolation(requiresConfirmatoryTest = false)

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
    fun whenDefaultWithPreviousIsolation_withPreviousIndicativePositiveTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWontBeInIsolation_andNoIsolation() = notReported {
        val previousTest = setExpiredPositiveTestIsolation(requiresConfirmatoryTest = true)

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
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedPosTest_whenAcknowledgingIndicativePosTest_showPosWillBeInIsolationAndOrderTest_andStartIsolation_orderTest() = notReported {
        setExpiredPositiveTestIsolation(requiresConfirmatoryTest = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionNotSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedNegativeTest_whenAcknowledgingConfirmedNegativeTest_showNegativeNotInIsolation_andNoIsolation() = notReported {
        val previousTest = setExpiredSelfAssessmentIsolationWithNegativeTest()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(NEGATIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysNegativeNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertExpiredIndexNoContact()
        checkRelevantTestResultPreserved(previousTest)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedNegativeTest_whenAcknowledgingConfirmedVoidTest_showVoidNotInIsolation_andNoIsolation() = notReported {
        val previousTest = setExpiredSelfAssessmentIsolationWithNegativeTest()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(VOID, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysVoidNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()
        orderTest()

        isolationChecker.assertExpiredIndexNoContact()
        checkRelevantTestResultPreserved(previousTest)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedNegativeTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWillBeInIsolation_andStartIsolation_shareKeys() = notReported {
        setExpiredSelfAssessmentIsolationWithNegativeTest()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()
        shareKeys()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedNegTest_whenAcknowledgingConfirmedPosTest_showPosWillBeInIsolation_andStartIsolation_withoutKeysSharing() = notReported {
        setExpiredSelfAssessmentIsolationWithNegativeTest()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = false)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedNegTest_whenAcknowledgingIndicativePosTest_showPosWillBeInIsolationAndOrderTest_andStartIsolation_orderTest() = notReported {
        setExpiredSelfAssessmentIsolationWithNegativeTest()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionNotSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithoutPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWillBeInIsolation_andStartIsolation_shareKeys() = notReported {
        testAppContext.setState(isolationHelper.neverInIsolation())

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()
        shareKeys()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithoutPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWillBeInIsolation_andStartIsolation_withoutKeysSharing() = notReported {
        testAppContext.setState(isolationHelper.neverInIsolation())

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = false)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()
        waitFor { statusRobot.checkActivityIsDisplayed() }

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithoutPreviousIsolation_withoutPreviousTest_whenAcknowledgingIndicativePositiveTest_showPositiveWillBeInIsolationAndOrderTest_andStartIsolation_orderTest() = notReported {
        testAppContext.setState(isolationHelper.neverInIsolation())

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionNotSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithoutPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedNegativeTest_showNegativeNotInIsolation_andNoIsolation() = notReported {
        testAppContext.setState(isolationHelper.neverInIsolation())

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveConfirmedTestResult(NEGATIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysNegativeNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        isolationChecker.assertNeverIsolating()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithoutPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedVoidTest_showVoidNotInIsolation_andNoIsolation() = notReported {
        testAppContext.setState(isolationHelper.neverInIsolation())

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(VOID, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysVoidNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()
        orderTest()

        isolationChecker.assertNeverIsolating()
        checkNoRelevantTestResult()
    }

    @Test
    fun whenIndexCase_withoutPreviousTest_whenAcknowledgingIndicativePositiveTest_showPositiveContinueIsolation_andContinueIsolation_shareKeysAndBookTest() = notReported {
        setSelfAssessmentIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePosTestResultWithKeySubmissionSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation() }

        shareKeysAndBookTest()

        isolationChecker.assertActiveIndexNoContact()
        checkNoRelevantTestResult()
    }

    @Test
    fun whenContactCase_withoutPreviousTest_whenAcknowledgingIndicativePositiveTest_showPositiveContinueIsolation_andContinueIsolation_shareKeys() = notReported {
        setContactCaseIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation() }
        shareKeysAndBookTest()

        isolationChecker.assertActiveIndexAndContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenIndexCase_withPreviousConfirmedPositiveTest_whenAcknowledgingIndicativePositiveTest_showPositiveContinueIsolation_andContinueIsolation_shareKeys() = notReported {
        val previousTest = setSelfAssessmentIsolationWithPositiveTest(requiresConfirmatoryTest = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePosTestResultWithKeySubmissionSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation() }
        testResultRobot.clickIsolationActionButton()

        shareKeys()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultPreserved(previousTest)
    }

    @Test
    fun whenIndexCase_withPreviousConfirmedPositiveTest_whenAcknowledgingIndicativePosTestOlderThanConfirmed_showPositiveContinueIsolation_andContinueIsolation_shareKeys() = notReported {
        val previousTest = setSelfAssessmentIsolationWithPositiveTest(requiresConfirmatoryTest = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionSupported(
            testEndDate = testEndDate.minus(1, ChronoUnit.DAYS)
        )
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation() }
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
    fun whenContactCase_withPrevConfirmedPosTestFromBeforeCurrentIsolation_onAcknowledgingIndicativePosTest_showPosContinueIsolation_andContinueIsolation_shareKeysAndBookTest() = notReported {
        setContactCaseIsolationWithExpiredPositiveTest(requiresConfirmatoryTest = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation() }

        shareKeysAndBookTest()

        isolationChecker.assertActiveIndexAndContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenContactCase_withPrevIndicativePosTestFromBeforeCurrentIsolation_onAcknowledgingIndicativePosTest_showPositiveContinueIsolation_shareKeysAndBookTest() = notReported {
        setContactCaseIsolationWithExpiredPositiveTest(requiresConfirmatoryTest = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation() }

        shareKeysAndBookTest()

        isolationChecker.assertActiveIndexAndContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withoutPreviousTest_whenAcknowledgingIndicativePositiveTest_showPositiveWillBeInIsolation_andStartIsolation_shareKeysAndBookTest() = notReported {
        setSelfAssessmentIsolation(expired = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }

        shareKeysAndBookTest()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedPosTest_whenAcknowledgingIndicativePosTest_showPositiveWillBeInIsolation_andStartIsolation_shareKeysAndBookTest() = notReported {
        setExpiredPositiveTestIsolation(requiresConfirmatoryTest = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }

        shareKeysAndBookTest()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedNegTest_whenAcknowledgingIndicativePosTest_showPosWillBeInIsolation_andStartIsolation_shareKeysAndBookTest() = notReported {
        setExpiredSelfAssessmentIsolationWithNegativeTest()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }

        shareKeysAndBookTest()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    @Test
    fun whenDefaultWithoutPreviousIsolation_withoutPreviousTest_whenAcknowledgingIndicativePositiveTest_showPositiveWillBeInIsolation_andStartIsolation_shareKeysAndBookTest() = notReported {
        testAppContext.setState(isolationHelper.neverInIsolation())

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePosTestResultWithKeySubmissionSupported()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }

        shareKeysAndBookTest()

        isolationChecker.assertActiveIndexNoContact()
        checkRelevantTestResultUpdated(testResponse)
    }

    private fun setSelfAssessmentIsolation(expired: Boolean = false): IsolationState {
        val isolationStart =
            if (expired) previousIsolationStart
            else isolationStart
        val state = isolationHelper.selfAssessment(selfAssessmentDate = isolationStart)
            .asIsolation(hasAcknowledgedEndOfIsolation = expired)
        testAppContext.setState(state)
        return state
    }

    private fun setExpiredPositiveTestIsolation(
        requiresConfirmatoryTest: Boolean,
        confirmatoryDayLimit: Int? = null
    ): AcknowledgedTestResult {
        val previousTest = previousTest(
            testResult = RelevantVirologyTestResult.POSITIVE,
            requiresConfirmatoryTest = requiresConfirmatoryTest,
            testEndDate = previousIsolationStart,
            confirmatoryDayLimit = confirmatoryDayLimit
        )

        testAppContext.setState(
            isolationHelper.positiveTest(
                testResult = previousTest
            ).asIsolation(hasAcknowledgedEndOfIsolation = true)
        )

        return previousTest
    }

    private fun setSelfAssessmentIsolationWithPositiveTest(
        requiresConfirmatoryTest: Boolean,
        confirmatoryDayLimit: Int? = null
    ): AcknowledgedTestResult {
        val previousTest = previousTest(
            testResult = RelevantVirologyTestResult.POSITIVE,
            requiresConfirmatoryTest = requiresConfirmatoryTest,
            testEndDate = testEndDate.toLocalDate(testAppContext.clock.zone),
            confirmatoryDayLimit = confirmatoryDayLimit
        )

        testAppContext.setState(
            isolationHelper.selfAssessment(
                testResult = previousTest
            ).asIsolation()
        )

        return previousTest
    }

    private fun setExpiredSelfAssessmentIsolationWithNegativeTest(): AcknowledgedTestResult {
        val previousTest = previousNegativeTest()

        testAppContext.setState(
            isolationHelper.selfAssessment(
                testResult = previousTest
            ).copy(
                expiryDate = previousTest.testEndDate
            ).asIsolation(hasAcknowledgedEndOfIsolation = true)
        )

        return previousTest
    }

    private fun setContactCaseIsolation() {
        testAppContext.setState(
            isolationHelper.contactCase(
                exposureDate = isolationStart,
                notificationDate = isolationStart
            ).asIsolation()
        )
    }

    private fun setContactCaseIsolationWithExpiredPositiveTest(requiresConfirmatoryTest: Boolean) {
        testAppContext.setState(
            IsolationState(
                isolationConfiguration = DurationDays(),
                contactCase = isolationHelper.contactCase(
                    exposureDate = isolationStart,
                    notificationDate = isolationStart
                ),
                indexInfo = isolationHelper.positiveTest(
                    previousTest(
                        testResult = RelevantVirologyTestResult.POSITIVE,
                        requiresConfirmatoryTest = requiresConfirmatoryTest,
                        testEndDate = isolationStart.minusDays(12)
                    )
                )
            )
        )
    }

    private fun previousNegativeTest(): AcknowledgedTestResult =
        previousTest(
            testResult = RelevantVirologyTestResult.NEGATIVE,
            requiresConfirmatoryTest = false,
            testEndDate = testEndDate.toLocalDate(testAppContext.clock.zone)
        )

    private fun previousTest(
        testResult: RelevantVirologyTestResult,
        requiresConfirmatoryTest: Boolean,
        testEndDate: LocalDate,
        confirmatoryDayLimit: Int? = null
    ): AcknowledgedTestResult =
        AcknowledgedTestResult(
            testEndDate,
            testResult,
            testKitType = LAB_RESULT,
            requiresConfirmatoryTest = requiresConfirmatoryTest,
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
            requiresConfirmatoryTest = false
        )
    }

    private fun receiveIndicativePosTestResultWithKeySubmissionNotSupported(
        testEndDate: Instant? = null
    ): TestResponse {
        return receiveTestResult(
            POSITIVE,
            RAPID_RESULT,
            testEndDate,
            diagnosisKeySubmissionSupported = false,
            requiresConfirmatoryTest = true
        )
    }

    private fun receiveIndicativePosTestResultWithKeySubmissionSupported(
        testEndDate: Instant? = null
    ): TestResponse {
        return receiveTestResult(
            POSITIVE,
            RAPID_RESULT,
            testEndDate,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = true
        )
    }

    private fun receiveTestResult(
        testResult: VirologyTestResult,
        testKit: VirologyTestKitType,
        testEndDate: Instant? = null,
        diagnosisKeySubmissionSupported: Boolean,
        requiresConfirmatoryTest: Boolean,
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
        val relevantTestResult = testAppContext.getCurrentState().indexInfo?.testResult
        assertNull(relevantTestResult)
    }

    private fun checkRelevantTestResultUpdated(
        testResponse: TestResponse
    ) {
        val relevantTestResult = testAppContext.getCurrentState().indexInfo?.testResult
        assertNotNull(relevantTestResult)
        checkRelevantTestResultUpdated(testResponse, relevantTestResult)
        assertNull(relevantTestResult.confirmedDate)
    }

    private fun checkRelevantTestResultUpdatedAndCompleted(
        testResponse: TestResponse,
        confirmedDate: LocalDate,
        confirmatoryTestCompletionStatus: ConfirmatoryTestCompletionStatus
    ) {
        val relevantTestResult = testAppContext.getCurrentState().indexInfo?.testResult
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
        val relevantTestResult = testAppContext.getCurrentState().indexInfo?.testResult
        val confirmedDateShouldBeNull = confirmatoryTestCompletionStatus == null
        assertNotNull(relevantTestResult)
        assertEquals(acknowledgedTestResult.testEndDate, relevantTestResult.testEndDate)
        assertEquals(acknowledgedTestResult.testResult, relevantTestResult.testResult)
        assertEquals(acknowledgedTestResult.testKitType, relevantTestResult.testKitType)
        assertEquals(acknowledgedTestResult.requiresConfirmatoryTest, relevantTestResult.requiresConfirmatoryTest)
        assertEquals(confirmedDateShouldBeNull, relevantTestResult.confirmedDate == null)
        assertEquals(acknowledgedTestResult.confirmatoryTestCompletionStatus, relevantTestResult.confirmatoryTestCompletionStatus)
    }

    companion object {
        private val isolationStart = LocalDate.now().minus(3, ChronoUnit.DAYS)
        private val previousIsolationStart = isolationStart.minus(12, ChronoUnit.DAYS)
        private val testEndDate = isolationStart.atStartOfDay(ZoneOffset.UTC).toInstant()
            .plus(1, ChronoUnit.DAYS)
    }
}
