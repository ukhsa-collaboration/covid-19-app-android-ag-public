package uk.nhs.nhsx.covid19.android.app.flow

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.OrderTest
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.DIAGNOSIS_KEY_SUBMISSION_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.NEGATIVE_PCR_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.NEGATIVE_PCR_TOKEN_NO_KEY_SUBMISSION
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.POSITIVE_PCR_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.POSITIVE_PCR_TOKEN_NO_KEY_SUBMISSION
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.VOID_PCR_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.VOID_PCR_TOKEN_NO_KEY_SUBMISSION
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
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext.Companion.ENGLISH_LOCAL_AUTHORITY
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderPollingConfig
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation.Overwrite
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReceiveTestResultFlowTests : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val testResultRobot = TestResultRobot(testAppContext.app)
    private val shareKeysInformationRobot = ShareKeysInformationRobot()
    private val orderTest = OrderTest(this)

    @Before
    fun setUp() {
        testAppContext.setLocalAuthority(ENGLISH_LOCAL_AUTHORITY)
    }

    @Test
    fun whenIndexCase_withoutPreviousTest_whenAcknowledgingConfirmedNegativeTest_showNegativeWontBeInIsolation_andEndIsolation() = notReported {
        setIndexCaseIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(NEGATIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysNegativeWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        assertTrue { testAppContext.getCurrentState() is Default }
        checkRelevantTestResultUpdated(RelevantVirologyTestResult.NEGATIVE)
    }

    @Test
    fun whenIndexCase_withoutPrevTest_whenAcknowledgingConfirmedNegTestOlderThanSymptomsOnsetDate_showNegAfterPosOrSymptomaticWillBeInIsolation_andContinueIsolation() = notReported {
        val isolation = setIndexCaseIsolation(selfAssessment = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(
            NEGATIVE,
            diagnosisKeySubmissionSupported = true,
            testEndDate = isolation.indexCase!!.symptomsOnsetDate.atStartOfDay().toInstant(ZoneOffset.UTC)
                .minus(1, ChronoUnit.DAYS)
        )
        waitFor { testResultRobot.checkActivityDisplaysNegativeAfterPositiveOrSymptomaticWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkNoRelevantTestResult()
    }

    @Test
    fun whenIndexCase_withoutPreviousTest_whenAcknowledgingConfirmedVoidTest_showVoidWillBeInIsolation_andContinueIsolation() = notReported {
        setIndexCaseIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(VOID, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysVoidWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkNoRelevantTestResult()
    }

    @Test
    fun whenIndexCase_withoutPreviousTest_whenAcknowledgingConfirmedPositiveTest_showPositiveContinueIsolation_andContinueIsolation_shareKeys() = notReported {
        setIndexCaseIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation() }

        testResultRobot.clickIsolationActionButton()
        shareKeys()

        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResultUpdated(RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenIndexCase_withoutPreviousTest_whenAcknowledgingConfirmedPositiveTest_showPositiveContinueIsolation_andContinueIsolation_withoutKeysSharing() = notReported {
        setIndexCaseIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = false)
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation() }

        testResultRobot.clickIsolationActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResultUpdated(RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenIndexCase_withoutPreviousTest_whenAcknowledgingIndicativePositiveTest_showPositiveWillBeInIsolationAndOrderTest_andContinueIsolation_orderTest() = notReported {
        setIndexCaseIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePositiveTestResult()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkNoRelevantTestResult() // indicative tests are ignored when already in index case
    }

    @Test
    fun whenContactCase_withoutPreviousTest_whenAcknowledgingIndicativePositiveTest_showPositiveWillBeInIsolationAndOrderTest_andContinueIsolation_orderTest() = notReported {
        setContactCaseIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePositiveTestResult()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResultUpdated(RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenIndexCase_withPreviousConfirmedPosTestFromCurrentIsolation_whenAcknowledgingConfirmedNegTest_showNegAfterPosOrSymptomaticWillBeInIsolation_andContinueIsolation() = notReported {
        setIndexCaseIsolation()
        val previousTestToken = setPreviousTest(POSITIVE, requiresConfirmatoryTest = false, fromCurrentIsolation = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(NEGATIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysNegativeAfterPositiveOrSymptomaticWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResult(previousTestToken, RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenIndexCase_withPreviousConfirmedPositiveTestFromCurrentIsolation_whenAcknowledgingConfirmedVoidTest_showVoidWillBeInIsolation_andContinueIsolation() = notReported {
        setIndexCaseIsolation()
        val previousToken = setPreviousTest(POSITIVE, requiresConfirmatoryTest = false, fromCurrentIsolation = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(VOID, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysVoidWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResult(previousToken, RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenIndexCase_withPreviousConfirmedPositiveTestFromBeforeCurrentIsolation_whenAcknowledgingConfirmedVoidTest_showVoidWillBeInIsolation_andContinueIsolation() = notReported {
        setIndexCaseIsolation()
        val previousToken = setPreviousTest(POSITIVE, requiresConfirmatoryTest = false, fromCurrentIsolation = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(VOID, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysVoidWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResult(previousToken, RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenIndexCase_withPreviousConfirmedPositiveTestFromCurrentIsolation_whenAcknowledgingConfirmedPositiveTest_showPositiveContinueIsolation_andContinueIsolation() = notReported {
        setIndexCaseIsolation()
        val previousTestToken = setPreviousTest(POSITIVE, requiresConfirmatoryTest = false, fromCurrentIsolation = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation() }

        testResultRobot.clickIsolationActionButton()
        shareKeys()

        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResult(previousTestToken, RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenIndexCase_withPreviousConfirmedPositiveTestFromBeforeCurrentIsolation_whenAcknowledgingConfirmedPositiveTest_showPositiveContinueIsolation_andContinueIsolation() = notReported {
        setIndexCaseIsolation()
        val previousToken = setPreviousTest(POSITIVE, requiresConfirmatoryTest = false, fromCurrentIsolation = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation() }

        testResultRobot.clickIsolationActionButton()
        shareKeys()

        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResult(previousToken, RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenIndexCase_withPreviousConfirmedPositiveTestFromCurrentIsolation_whenAcknowledgingIndicativePositiveTest_showPositiveContinueIsolationNoChange_andContinueIsolation() = notReported {
        setIndexCaseIsolation()
        val previousTestToken = setPreviousTest(POSITIVE, requiresConfirmatoryTest = false, fromCurrentIsolation = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePositiveTestResult()
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolationNoChange() }

        testResultRobot.clickIsolationActionButton()
        waitFor { statusRobot.checkActivityIsDisplayed() }

        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResult(previousTestToken, RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenIndexCase_withPrevConfirmedPosTestFromBeforeCurrentIsolation_whenAcknowledgingIndicativePosTest_showPosWillBeInIsolationAndOrderTest_andContinueIsolation_orderTest() = notReported {
        setIndexCaseIsolation()
        val previousTestToken = setPreviousTest(POSITIVE, requiresConfirmatoryTest = false, fromCurrentIsolation = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePositiveTestResult()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResult(previousTestToken, RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenContactCase_withPrevConfirmedPosTestFromBeforeCurrentIsolation_onAcknowledgingIndicativePosTest_showPosWillBeInIsolationAndOrderTest_andContinueIsolation_orderTest() = notReported {
        setContactCaseIsolation()
        setPreviousTest(POSITIVE, requiresConfirmatoryTest = false, fromCurrentIsolation = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePositiveTestResult()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResultUpdated(RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenIndexCase_withPreviousIndicativePosTestFromCurrentIsolation_whenAcknowledgingIndicativePosTest_showPosWillBeInIsolationAndOrderTest_andContinueIsolation_orderTest() = notReported {
        setIndexCaseIsolation()
        val previousTestToken = setPreviousTest(POSITIVE, requiresConfirmatoryTest = true, fromCurrentIsolation = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePositiveTestResult()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResult(previousTestToken, RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenIndexCase_withPreviousIndicativePositiveTestFromCurrentIsolation_whenAcknowledgingConfirmedNegativeTest_showNegativeWontBeInIsolation_andNoIsolation() = notReported {
        setIndexCaseIsolation()
        setPreviousTest(POSITIVE, requiresConfirmatoryTest = true, fromCurrentIsolation = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(NEGATIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysNegativeWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        assertTrue { testAppContext.getCurrentState() is Default }
        checkRelevantTestResultUpdated(RelevantVirologyTestResult.NEGATIVE)
    }

    @Test
    fun whenIndexCase_withPrevIndicativePosTestFromCurIsolation_onAcknowledgingConfirmedNegTestOlderThanIndicative_showNegAfterPosOrSymptomaticWillBeInIsolation_andContIsolation() = notReported {
        setIndexCaseIsolation()
        val previousTestToken = setPreviousTest(POSITIVE, requiresConfirmatoryTest = true, fromCurrentIsolation = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(
            NEGATIVE,
            diagnosisKeySubmissionSupported = true,
            testEndDate = testEndDateWithinCurrentIsolation.minus(1, ChronoUnit.DAYS)
        )
        waitFor { testResultRobot.checkActivityDisplaysNegativeAfterPositiveOrSymptomaticWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResult(previousTestToken, RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenIndexCase_withPrevIndicativePosTestFromBeforeCurrentIsolation_whenAcknowledgingIndicativePosTest_showPosWillBeInIsolationAndOrderTest_andContinueIsolation_orderTest() = notReported {
        setIndexCaseIsolation()
        val previousTestToken = setPreviousTest(POSITIVE, requiresConfirmatoryTest = true, fromCurrentIsolation = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePositiveTestResult()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResult(previousTestToken, RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenContactCase_withPrevIndicativePosTestFromBeforeCurrentIsolation_onAcknowledgingIndicativePosTest_showPosWillBeInIsolationAndOrderTest_andContinueIsolation_orderTest() = notReported {
        setContactCaseIsolation()
        setPreviousTest(POSITIVE, requiresConfirmatoryTest = true, fromCurrentIsolation = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePositiveTestResult()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResultUpdated(RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedNegativeTest_showNegativeNotInIsolation_andNoIsolation() = notReported {
        setDefaultWithPreviousIndexCaseIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(NEGATIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysNegativeNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        assertTrue { testAppContext.getCurrentState() is Default }
        checkRelevantTestResultUpdated(RelevantVirologyTestResult.NEGATIVE)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedNegativeTestOlderThanSymptomsOnsetDate_showNegativeNotInIsolation_andNoIsolation() = notReported {
        val state = setDefaultWithPreviousIndexCaseIsolation(selfAssessment = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(
            NEGATIVE,
            diagnosisKeySubmissionSupported = true,
            testEndDate = state.previousIsolation!!.indexCase!!.symptomsOnsetDate.atStartOfDay().toInstant(ZoneOffset.UTC)
                .minus(1, ChronoUnit.DAYS)
        )
        waitFor { testResultRobot.checkActivityDisplaysNegativeNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        assertTrue { testAppContext.getCurrentState() is Default }
        checkNoRelevantTestResult()
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedVoidTest_showVoidNotInIsolation_andNoIsolation() = notReported {
        setDefaultWithPreviousIndexCaseIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(VOID, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysVoidNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()
        orderTest()

        assertTrue { testAppContext.getCurrentState() is Default }
        checkNoRelevantTestResult()
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWontBeInIsolation_andNoIsolation_shareKeys() = notReported {
        setDefaultWithPreviousIndexCaseIsolation(selfAssessment = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()
        shareKeys()

        assertTrue { testAppContext.getCurrentState() is Default }
        checkRelevantTestResultUpdated(RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWontBeInIsolation_andNoIsolation_withoutKeysSharing() = notReported {
        setDefaultWithPreviousIndexCaseIsolation(selfAssessment = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = false)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        assertTrue { testAppContext.getCurrentState() is Default }
        checkRelevantTestResultUpdated(RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withoutPreviousTest_whenAcknowledgingIndicativePositiveTest_showPositiveWillBeInIsolationAndOrderTest_andStartIsolation_orderTest() = notReported {
        setDefaultWithPreviousIndexCaseIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePositiveTestResult()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResultUpdated(RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedPositiveTest_whenAcknowledgingConfirmedNegativeTest_showNegativeNotInIsolation_andNoIsolation() = notReported {
        setDefaultWithPreviousIndexCaseIsolation()
        val previousToken = setPreviousTest(POSITIVE, requiresConfirmatoryTest = false, fromCurrentIsolation = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(NEGATIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysNegativeNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        assertTrue { testAppContext.getCurrentState() is Default }
        checkRelevantTestResult(previousToken, RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedPositiveTest_whenAcknowledgingConfirmedVoidTest_showVoidNotInIsolation_andNoIsolation() = notReported {
        setDefaultWithPreviousIndexCaseIsolation()
        val previousToken = setPreviousTest(POSITIVE, requiresConfirmatoryTest = false, fromCurrentIsolation = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(VOID, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysVoidNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()
        orderTest()

        assertTrue { testAppContext.getCurrentState() is Default }
        checkRelevantTestResult(previousToken, RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedPositiveTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWontBeInIsolation_andNoIsolation_shareKeys() = notReported {
        setDefaultWithPreviousIndexCaseIsolation()
        val previousToken = setPreviousTest(POSITIVE, requiresConfirmatoryTest = false, fromCurrentIsolation = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()
        shareKeys()

        assertTrue { testAppContext.getCurrentState() is Default }
        checkRelevantTestResult(previousToken, RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousIndicativePositiveTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWontBeInIsolation_andNoIsolation_shareKeys() = notReported {
        setDefaultWithPreviousIndexCaseIsolation()
        val previousToken = setPreviousTest(POSITIVE, requiresConfirmatoryTest = true, fromCurrentIsolation = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()
        shareKeys()

        assertTrue { testAppContext.getCurrentState() is Default }
        checkRelevantTestResult(previousToken, RelevantVirologyTestResult.POSITIVE, confirmedDateShouldBeNull = false)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedPositiveTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWontBeInIsolation_andNoIsolation_withoutKeysSharing() = notReported {
        setDefaultWithPreviousIndexCaseIsolation()
        val previousToken = setPreviousTest(POSITIVE, requiresConfirmatoryTest = false, fromCurrentIsolation = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = false)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        assertTrue { testAppContext.getCurrentState() is Default }
        checkRelevantTestResult(previousToken, RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousIndicativePositiveTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWontBeInIsolation_andNoIsolation() = notReported {
        setDefaultWithPreviousIndexCaseIsolation()
        val previousToken = setPreviousTest(POSITIVE, requiresConfirmatoryTest = true, fromCurrentIsolation = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = false)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        assertTrue { testAppContext.getCurrentState() is Default }
        checkRelevantTestResult(previousToken, RelevantVirologyTestResult.POSITIVE, confirmedDateShouldBeNull = false)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedPosTest_whenAcknowledgingIndicativePosTest_showPosWillBeInIsolationAndOrderTest_andStartIsolation_orderTest() = notReported {
        setDefaultWithPreviousIndexCaseIsolation()
        setPreviousTest(POSITIVE, requiresConfirmatoryTest = false, fromCurrentIsolation = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePositiveTestResult()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResultUpdated(RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedNegativeTest_whenAcknowledgingConfirmedNegativeTest_showNegativeNotInIsolation_andNoIsolation() = notReported {
        setDefaultWithPreviousIndexCaseIsolation()
        val previousToken = setPreviousTest(NEGATIVE, requiresConfirmatoryTest = false, fromCurrentIsolation = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(NEGATIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysNegativeNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        assertTrue { testAppContext.getCurrentState() is Default }
        checkRelevantTestResult(previousToken, RelevantVirologyTestResult.NEGATIVE)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedNegativeTest_whenAcknowledgingConfirmedVoidTest_showVoidNotInIsolation_andNoIsolation() = notReported {
        setDefaultWithPreviousIndexCaseIsolation()
        val previousToken = setPreviousTest(NEGATIVE, requiresConfirmatoryTest = false, fromCurrentIsolation = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(VOID, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysVoidNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()
        orderTest()

        assertTrue { testAppContext.getCurrentState() is Default }
        checkRelevantTestResult(previousToken, RelevantVirologyTestResult.NEGATIVE)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedNegativeTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWillBeInIsolation_andStartIsolation_shareKeys() = notReported {
        setDefaultWithPreviousIndexCaseIsolation()
        setPreviousTest(NEGATIVE, requiresConfirmatoryTest = false, fromCurrentIsolation = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()
        shareKeys()

        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResultUpdated(RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedNegTest_whenAcknowledgingConfirmedPosTest_showPosWillBeInIsolation_andStartIsolation_withoutKeysSharing() = notReported {
        setDefaultWithPreviousIndexCaseIsolation()
        setPreviousTest(NEGATIVE, requiresConfirmatoryTest = false, fromCurrentIsolation = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = false)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResultUpdated(RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedNegTest_whenAcknowledgingIndicativePosTest_showPosWillBeInIsolationAndOrderTest_andStartIsolation_orderTest() = notReported {
        setDefaultWithPreviousIndexCaseIsolation()
        setPreviousTest(NEGATIVE, requiresConfirmatoryTest = false, fromCurrentIsolation = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePositiveTestResult()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResultUpdated(RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenDefaultWithoutPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWillBeInIsolation_andStartIsolation_shareKeys() = notReported {
        setDefaultWithoutPreviousIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()
        shareKeys()

        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResultUpdated(RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenDefaultWithoutPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWillBeInIsolation_andStartIsolation_withoutKeysSharing() = notReported {
        setDefaultWithoutPreviousIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(POSITIVE, diagnosisKeySubmissionSupported = false)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()
        waitFor { statusRobot.checkActivityIsDisplayed() }

        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResultUpdated(RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenDefaultWithoutPreviousIsolation_withoutPreviousTest_whenAcknowledgingIndicativePositiveTest_showPositiveWillBeInIsolationAndOrderTest_andStartIsolation_orderTest() = notReported {
        setDefaultWithoutPreviousIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePositiveTestResult()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderTest()

        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResultUpdated(RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenDefaultWithoutPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedNegativeTest_showNegativeNotInIsolation_andNoIsolation() = notReported {
        setDefaultWithoutPreviousIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(NEGATIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysNegativeNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        assertTrue { testAppContext.getCurrentState() is Default }
        checkRelevantTestResultUpdated(RelevantVirologyTestResult.NEGATIVE)
    }

    @Test
    fun whenDefaultWithoutPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedVoidTest_showVoidNotInIsolation_andNoIsolation() = notReported {
        setDefaultWithoutPreviousIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(VOID, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysVoidNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()
        orderTest()

        assertTrue { testAppContext.getCurrentState() is Default }
        checkNoRelevantTestResult()
    }

    private fun setDefaultWithoutPreviousIsolation() {
        testAppContext.setState(Default())
    }

    private fun setDefaultWithPreviousIndexCaseIsolation(selfAssessment: Boolean = false): Default {
        val state = Default(
            previousIsolation = createIndexCaseIsolation(
                previousIsolationStart,
                selfAssessment
            )
        )
        testAppContext.setState(state)
        return state
    }

    private fun setIndexCaseIsolation(selfAssessment: Boolean = false): Isolation {
        val state = createIndexCaseIsolation(isolationStart, selfAssessment)
        testAppContext.setState(state)
        return state
    }

    private fun createIndexCaseIsolation(isolationStart: Instant, selfAssessment: Boolean = false): Isolation {
        val isolationStartDate =
            LocalDateTime.ofInstant(isolationStart, ZoneId.systemDefault()).toLocalDate()
        return Isolation(
            isolationStart = isolationStart,
            isolationConfiguration = DurationDays(),
            indexCase = IndexCase(
                symptomsOnsetDate = isolationStartDate.minusDays(3),
                expiryDate = isolationStartDate.plusDays(7),
                selfAssessment = selfAssessment
            )
        )
    }

    private fun setContactCaseIsolation() {
        val isolationStartDate =
            LocalDateTime.ofInstant(isolationStart, ZoneId.systemDefault()).toLocalDate()
        testAppContext.setState(
            state = Isolation(
                isolationStart = isolationStart,
                isolationConfiguration = DurationDays(),
                contactCase = ContactCase(
                    startDate = isolationStart,
                    notificationDate = isolationStart,
                    expiryDate = isolationStartDate.plusDays(11)
                )
            )
        )
    }

    private fun setPreviousTest(
        testResult: VirologyTestResult,
        requiresConfirmatoryTest: Boolean,
        fromCurrentIsolation: Boolean
    ): String {
        val token = "oldToken"
        val testEndDate =
            if (fromCurrentIsolation) testEndDateWithinCurrentIsolation
            else testEndDateBeforeCurrentIsolation
        testAppContext.getRelevantTestResultProvider().onTestResultAcknowledged(
            ReceivedTestResult(
                token,
                testEndDate,
                testResult,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = requiresConfirmatoryTest
            ),
            testResultStorageOperation = Overwrite
        )
        return token
    }

    private fun getConfirmedTestResultToken(
        testResult: VirologyTestResult,
        diagnosisKeySubmissionSupported: Boolean
    ): String =
        when (testResult) {
            POSITIVE -> if (diagnosisKeySubmissionSupported) POSITIVE_PCR_TOKEN else POSITIVE_PCR_TOKEN_NO_KEY_SUBMISSION
            NEGATIVE -> if (diagnosisKeySubmissionSupported) NEGATIVE_PCR_TOKEN else NEGATIVE_PCR_TOKEN_NO_KEY_SUBMISSION
            VOID -> if (diagnosisKeySubmissionSupported) VOID_PCR_TOKEN else VOID_PCR_TOKEN_NO_KEY_SUBMISSION
        }

    private fun receiveConfirmedTestResult(
        testResult: VirologyTestResult,
        diagnosisKeySubmissionSupported: Boolean,
        testEndDate: Instant? = null
    ) {
        testAppContext.virologyTestingApi.testEndDate = testEndDate
        receiveTestResult(
            testResult,
            LAB_RESULT,
            diagnosisKeySubmissionSupported = diagnosisKeySubmissionSupported,
            requiresConfirmatoryTest = false
        )
    }

    private fun receiveIndicativePositiveTestResult() {
        receiveTestResult(
            POSITIVE,
            RAPID_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = true
        )
    }

    private fun receiveTestResult(
        testResult: VirologyTestResult,
        testKit: VirologyTestKitType,
        diagnosisKeySubmissionSupported: Boolean,
        requiresConfirmatoryTest: Boolean
    ) {
        val pollingConfig = TestOrderPollingConfig(
            Instant.now(),
            "pollingToken",
            DIAGNOSIS_KEY_SUBMISSION_TOKEN
        )

        testAppContext.getTestOrderingTokensProvider().add(pollingConfig)

        testAppContext.virologyTestingApi.testResponseForPollingToken = mutableMapOf(
            pollingConfig.testResultPollingToken to TestResponse(
                testResult,
                testKit,
                diagnosisKeySubmissionSupported = diagnosisKeySubmissionSupported,
                requiresConfirmatoryTest = requiresConfirmatoryTest
            )
        )

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }
    }

    private fun shareKeys() {
        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickIUnderstandButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    private fun checkNoRelevantTestResult() {
        assertNull(testAppContext.getRelevantTestResultProvider().testResult)
    }

    private fun checkRelevantTestResultUpdated(
        testResult: RelevantVirologyTestResult
    ) {
        checkRelevantTestResult(DIAGNOSIS_KEY_SUBMISSION_TOKEN, testResult)
    }

    private fun checkRelevantTestResult(
        diagnosisKeySubmissionToken: String,
        testResult: RelevantVirologyTestResult,
        confirmedDateShouldBeNull: Boolean = true
    ) {
        val relevantTestResult = testAppContext.getRelevantTestResultProvider().testResult
        assertNotNull(relevantTestResult)
        assertEquals(diagnosisKeySubmissionToken, relevantTestResult.diagnosisKeySubmissionToken)
        assertEquals(testResult, relevantTestResult.testResult)
        assertEquals(confirmedDateShouldBeNull, relevantTestResult.confirmedDate == null)
    }

    companion object {
        private val isolationStart = Instant.now().minus(3, ChronoUnit.DAYS)
        private val previousIsolationStart = isolationStart.minus(10, ChronoUnit.DAYS)
        private val testEndDateWithinCurrentIsolation = isolationStart.plus(1, ChronoUnit.DAYS)
        private val testEndDateBeforeCurrentIsolation = isolationStart.minus(1, ChronoUnit.DAYS)
    }
}
