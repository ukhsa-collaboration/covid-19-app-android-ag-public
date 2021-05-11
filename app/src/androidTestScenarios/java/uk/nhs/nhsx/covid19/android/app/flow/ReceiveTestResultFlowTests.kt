package uk.nhs.nhsx.covid19.android.app.flow

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.OrderTest
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
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderPollingConfig
import uk.nhs.nhsx.covid19.android.app.testordering.toRelevantVirologyTestResult
import uk.nhs.nhsx.covid19.android.app.util.IsolationChecker
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ReceiveTestResultFlowTests : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val testResultRobot = TestResultRobot(testAppContext.app)
    private val shareKeysInformationRobot = ShareKeysInformationRobot()
    private val orderTest = OrderTest(this)
    private val shareKeysResultRobot = ShareKeysResultRobot()
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

        receiveIndicativePositiveTestResult()
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

        val testResponse = receiveIndicativePositiveTestResult()
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

        receiveIndicativePositiveTestResult()
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

        val testResponse = receiveIndicativePositiveTestResult()
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

        receiveIndicativePositiveTestResult()
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
        isolationChecker.assertNeverIsolating()
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
    fun whenContactCase_withPrevIndicativePosTestFromBeforeCurrentIsolation_onAcknowledgingIndicativePosTest_showPosWillBeInIsolationAndOrderTest_andContinueIsolation_orderTest() = notReported {
        setContactCaseIsolationWithExpiredPositiveTest(requiresConfirmatoryTest = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePositiveTestResult()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        isolationChecker.assertActiveIndexAndContact()
        checkRelevantTestResultUpdated(testResponse)
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

        val testResponse = receiveIndicativePositiveTestResult()
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
        checkRelevantTestResultPreserved(previousTest, confirmedDateShouldBeNull = false)
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
        checkRelevantTestResultPreserved(previousTest, confirmedDateShouldBeNull = false)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedPosTest_whenAcknowledgingIndicativePosTest_showPosWillBeInIsolationAndOrderTest_andStartIsolation_orderTest() = notReported {
        setExpiredPositiveTestIsolation(requiresConfirmatoryTest = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        val testResponse = receiveIndicativePositiveTestResult()
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

        val testResponse = receiveIndicativePositiveTestResult()
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

        val testResponse = receiveIndicativePositiveTestResult()
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
        requiresConfirmatoryTest: Boolean
    ): AcknowledgedTestResult {
        val previousTest = previousTest(
            testResult = RelevantVirologyTestResult.POSITIVE,
            requiresConfirmatoryTest,
            testEndDate = previousIsolationStart
        )

        testAppContext.setState(
            isolationHelper.positiveTest(
                testResult = previousTest
            ).asIsolation(hasAcknowledgedEndOfIsolation = true)
        )

        return previousTest
    }

    private fun setSelfAssessmentIsolationWithPositiveTest(
        requiresConfirmatoryTest: Boolean
    ): AcknowledgedTestResult {
        val previousTest = previousTest(
            testResult = RelevantVirologyTestResult.POSITIVE,
            requiresConfirmatoryTest,
            testEndDate = LocalDateTime.ofInstant(testEndDate, testAppContext.clock.zone).toLocalDate()
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
                        requiresConfirmatoryTest,
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
            testEndDate = LocalDateTime.ofInstant(testEndDate, testAppContext.clock.zone).toLocalDate()
        )

    private fun previousTest(
        testResult: RelevantVirologyTestResult,
        requiresConfirmatoryTest: Boolean,
        testEndDate: LocalDate
    ): AcknowledgedTestResult =
        AcknowledgedTestResult(
            testEndDate,
            testResult,
            testKitType = LAB_RESULT,
            requiresConfirmatoryTest = requiresConfirmatoryTest,
            acknowledgedDate = testEndDate
        )

    private fun receiveConfirmedTestResult(
        testResult: VirologyTestResult,
        diagnosisKeySubmissionSupported: Boolean,
        testEndDate: Instant? = null
    ): TestResponse {
        testAppContext.virologyTestingApi.testEndDate = testEndDate
        return receiveTestResult(
            testResult,
            LAB_RESULT,
            diagnosisKeySubmissionSupported = diagnosisKeySubmissionSupported,
            requiresConfirmatoryTest = false
        )
    }

    private fun receiveIndicativePositiveTestResult(): TestResponse =
        receiveTestResult(
            POSITIVE,
            RAPID_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = true
        )

    private fun receiveTestResult(
        testResult: VirologyTestResult,
        testKit: VirologyTestKitType,
        diagnosisKeySubmissionSupported: Boolean,
        requiresConfirmatoryTest: Boolean
    ): TestResponse {
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
            requiresConfirmatoryTest = requiresConfirmatoryTest
        )
        testAppContext.virologyTestingApi.testResponseForPollingToken = mutableMapOf(
            pollingConfig.testResultPollingToken to testResponse
        )

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        return testResponse
    }

    private fun shareKeys() {
        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickContinueButton()

        waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }

        shareKeysResultRobot.clickActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
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
        assertEquals(testResponse.testResult.toRelevantVirologyTestResult(), relevantTestResult.testResult)
        assertEquals(testResponse.testKitType, relevantTestResult.testKitType)
        assertEquals(testResponse.requiresConfirmatoryTest, relevantTestResult.requiresConfirmatoryTest)
        assertNull(relevantTestResult.confirmedDate)
    }

    private fun checkRelevantTestResultPreserved(
        acknowledgedTestResult: AcknowledgedTestResult,
        confirmedDateShouldBeNull: Boolean = true
    ) {
        val relevantTestResult = testAppContext.getCurrentState().indexInfo?.testResult
        assertNotNull(relevantTestResult)
        assertEquals(acknowledgedTestResult.testEndDate, relevantTestResult.testEndDate)
        assertEquals(acknowledgedTestResult.testResult, relevantTestResult.testResult)
        assertEquals(acknowledgedTestResult.testKitType, relevantTestResult.testKitType)
        assertEquals(acknowledgedTestResult.requiresConfirmatoryTest, relevantTestResult.requiresConfirmatoryTest)
        assertEquals(confirmedDateShouldBeNull, relevantTestResult.confirmedDate == null)
    }

    companion object {
        private val isolationStart = LocalDate.now().minus(3, ChronoUnit.DAYS)
        private val previousIsolationStart = isolationStart.minus(12, ChronoUnit.DAYS)
        private val testEndDate = isolationStart.atStartOfDay(ZoneOffset.UTC).toInstant()
            .plus(1, ChronoUnit.DAYS)
    }
}
