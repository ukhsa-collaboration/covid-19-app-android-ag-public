package uk.nhs.nhsx.covid19.android.app.flow

import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import com.jeroenmols.featureflag.framework.TestSetting.USE_WEB_VIEW_FOR_INTERNAL_BROWSER
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.DIAGNOSIS_KEY_SUBMISSION_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.NEGATIVE_PCR_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.NEGATIVE_PCR_TOKEN_NO_KEY_SUBMISSION
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.POSITIVE_LFD_TOKEN_INDICATIVE
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.POSITIVE_PCR_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.POSITIVE_PCR_TOKEN_NO_KEY_SUBMISSION
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.VOID_PCR_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.VOID_PCR_TOKEN_NO_KEY_SUBMISSION
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
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
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BrowserRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOrderingRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation.OVERWRITE
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReceiveTestResultFlowTests : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val linkTestResultRobot = LinkTestResultRobot()
    private val testResultRobot = TestResultRobot()
    private val shareKeysInformationRobot = ShareKeysInformationRobot()
    private val testOrderingRobot = TestOrderingRobot()
    private val browserRobot = BrowserRobot()

    @Before
    fun setUp() {
        FeatureFlagTestHelper.enableFeatureFlag(USE_WEB_VIEW_FOR_INTERNAL_BROWSER)
        testAppContext.setLocalAuthority(ENGLISH_LOCAL_AUTHORITY)
    }

    @After
    fun tearDown() {
        FeatureFlagTestHelper.disableFeatureFlag(USE_WEB_VIEW_FOR_INTERNAL_BROWSER)
    }

    @Test
    fun whenIsolatingWithIndexCase_withoutPreviousTest_whenAcknowledgingConfirmedNegativeTest_showNegativeWontBeInIsolation_andEndIsolation() = notReported {
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
    fun whenIsolatingWithIndexCase_withoutPreviousTest_whenAcknowledgingConfirmedVoidTest_showVoidWillBeInIsolation_andContinueIsolation() = notReported {
        setIndexCaseIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(VOID, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysVoidWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()
        orderConfirmatoryTest()

        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkNoRelevantTestResult()
    }

    @Test
    fun whenIsolatingWithIndexCase_withoutPreviousTest_whenAcknowledgingConfirmedPositiveTest_showPositiveContinueIsolation_andContinueIsolation_shareKeys() = notReported {
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
    fun whenIsolatingWithIndexCase_withoutPreviousTest_whenAcknowledgingConfirmedPositiveTest_showPositiveContinueIsolation_andContinueIsolation_withoutKeysSharing() = notReported {
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
    fun whenIsolatingWithIndexCase_withoutPreviousTest_whenAcknowledgingIndicativePositiveTest_showPositiveWillBeInIsolationAndOrderTest_andContinueIsolation_orderTest() = notReported {
        setIndexCaseIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePositiveTestResult()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderConfirmatoryTest()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkNoRelevantTestResult() // indicative tests are ignored when already in index case
    }

    @Test
    fun whenIsolatingWithContactCase_withoutPreviousTest_whenAcknowledgingIndicativePositiveTest_showPositiveWillBeInIsolationAndOrderTest_andContinueIsolation_orderTest() = notReported {
        setContactCaseIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePositiveTestResult()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderConfirmatoryTest()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResultUpdated(RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenIsolatingWithIndexCase_withPreviousConfirmedPositiveTestFromCurrentIsolation_whenAcknowledgingConfirmedNegativeTest_showPositiveThenNegativeWillBeInIsolation_andContinueIsolation() = notReported {
        setIndexCaseIsolation()
        val previousTestToken = setPreviousTest(POSITIVE, requiresConfirmatoryTest = false, fromCurrentIsolation = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(NEGATIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysPositiveThenNegativeWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResult(previousTestToken, RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenIsolatingWithIndexCase_withPreviousConfirmedPositiveTestFromCurrentIsolation_whenAcknowledgingConfirmedVoidTest_showVoidWillBeInIsolation_andContinueIsolation() = notReported {
        setIndexCaseIsolation()
        val previousToken = setPreviousTest(POSITIVE, requiresConfirmatoryTest = false, fromCurrentIsolation = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(VOID, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysVoidWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()
        orderConfirmatoryTest()

        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResult(previousToken, RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenIsolatingWithIndexCase_withPreviousConfirmedPositiveTestFromBeforeCurrentIsolation_whenAcknowledgingConfirmedVoidTest_showVoidWillBeInIsolation_andContinueIsolation() = notReported {
        setIndexCaseIsolation()
        val previousToken = setPreviousTest(POSITIVE, requiresConfirmatoryTest = false, fromCurrentIsolation = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(VOID, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysVoidWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()
        orderConfirmatoryTest()

        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResult(previousToken, RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenIsolatingWithIndexCase_withPreviousConfirmedPositiveTestFromCurrentIsolation_whenAcknowledgingConfirmedPositiveTest_showPositiveContinueIsolation_andContinueIsolation() = notReported {
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
    fun whenIsolatingWithIndexCase_withPreviousConfirmedPositiveTestFromBeforeCurrentIsolation_whenAcknowledgingConfirmedPositiveTest_showPositiveContinueIsolation_andContinueIsolation() = notReported {
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
    fun whenIsolatingWithIndexCase_withPreviousConfirmedPositiveTestFromCurrentIsolation_whenAcknowledgingIndicativePositiveTest_showPositiveContinueIsolationNoChange_andContinueIsolation() = notReported {
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
    fun whenIsolatingWithIndexCase_withPreviousConfirmedPositiveTestFromBeforeCurrentIsolation_whenAcknowledgingIndicativePositiveTest_showPositiveWillBeInIsolationAndOrderTest_andContinueIsolation_orderTest() = notReported {
        setIndexCaseIsolation()
        val previousTestToken = setPreviousTest(POSITIVE, requiresConfirmatoryTest = false, fromCurrentIsolation = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePositiveTestResult()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderConfirmatoryTest()

        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResult(previousTestToken, RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenIsolatingWithContactCase_withPreviousConfirmedPositiveTestFromBeforeCurrentIsolation_whenAcknowledgingIndicativePositiveTest_showPositiveWillBeInIsolationAndOrderTest_andContinueIsolation_orderTest() = notReported {
        setContactCaseIsolation()
        setPreviousTest(POSITIVE, requiresConfirmatoryTest = false, fromCurrentIsolation = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePositiveTestResult()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderConfirmatoryTest()

        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResultUpdated(RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenIsolatingWithIndexCase_withPreviousIndicativePositiveTestFromCurrentIsolation_whenAcknowledgingIndicativePositiveTest_showPositiveWillBeInIsolationAndOrderTest_andContinueIsolation_orderTest() = notReported {
        setIndexCaseIsolation()
        val previousTestToken = setPreviousTest(POSITIVE, requiresConfirmatoryTest = true, fromCurrentIsolation = true)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePositiveTestResult()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderConfirmatoryTest()

        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResult(previousTestToken, RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenIsolatingWithIndexCase_withPreviousIndicativePositiveTestFromCurrentIsolation_whenAcknowledgingConfirmedNegativeTest_showNegativeWontBeInIsolation_andNoIsolation() = notReported {
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
    fun whenIsolatingWithIndexCase_withPreviousIndicativePositiveTestFromBeforeCurrentIsolation_whenAcknowledgingIndicativePositiveTest_showPositiveWillBeInIsolationAndOrderTest_andContinueIsolation_orderTest() = notReported {
        setIndexCaseIsolation()
        val previousTestToken = setPreviousTest(POSITIVE, requiresConfirmatoryTest = true, fromCurrentIsolation = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePositiveTestResult()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderConfirmatoryTest()

        assertTrue { testAppContext.getCurrentState() is Isolation }
        checkRelevantTestResult(previousTestToken, RelevantVirologyTestResult.POSITIVE)
    }

    @Test
    fun whenIsolatingWithContactCase_withPreviousIndicativePositiveTestFromBeforeCurrentIsolation_whenAcknowledgingIndicativePositiveTest_showPositiveWillBeInIsolationAndOrderTest_andContinueIsolation_orderTest() = notReported {
        setContactCaseIsolation()
        setPreviousTest(POSITIVE, requiresConfirmatoryTest = true, fromCurrentIsolation = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePositiveTestResult()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderConfirmatoryTest()

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
    fun whenDefaultWithPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedVoidTest_showVoidNotInIsolation_andNoIsolation() = notReported {
        setDefaultWithPreviousIndexCaseIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveConfirmedTestResult(VOID, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysVoidNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()
        orderConfirmatoryTest()

        assertTrue { testAppContext.getCurrentState() is Default }
        checkNoRelevantTestResult()
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withoutPreviousTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWontBeInIsolation_andNoIsolation_shareKeys() = notReported {
        setDefaultWithPreviousIndexCaseIsolation()

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
        setDefaultWithPreviousIndexCaseIsolation()

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
        orderConfirmatoryTest()

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
        orderConfirmatoryTest()

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
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedPositiveTest_whenAcknowledgingIndicativePositiveTest_showPositiveWillBeInIsolationAndOrderTest_andStartIsolation_orderTest() = notReported {
        setDefaultWithPreviousIndexCaseIsolation()
        setPreviousTest(POSITIVE, requiresConfirmatoryTest = false, fromCurrentIsolation = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePositiveTestResult()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderConfirmatoryTest()

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
        orderConfirmatoryTest()

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
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedNegativeTest_whenAcknowledgingConfirmedPositiveTest_showPositiveWillBeInIsolation_andStartIsolation_withoutKeysSharing() = notReported {
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
    fun whenDefaultWithPreviousIsolation_withPreviousConfirmedNegativeTest_whenAcknowledgingIndicativePositiveTest_showPositiveWillBeInIsolationAndOrderTest_andStartIsolation_orderTest() = notReported {
        setDefaultWithPreviousIndexCaseIsolation()
        setPreviousTest(NEGATIVE, requiresConfirmatoryTest = false, fromCurrentIsolation = false)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveIndicativePositiveTestResult()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

        testResultRobot.clickIsolationActionButton()
        orderConfirmatoryTest()

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
        orderConfirmatoryTest()

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
        orderConfirmatoryTest()

        assertTrue { testAppContext.getCurrentState() is Default }
        checkNoRelevantTestResult()
    }

    private fun setDefaultWithoutPreviousIsolation() {
        testAppContext.setState(Default())
    }

    private fun setDefaultWithPreviousIndexCaseIsolation() {
        testAppContext.setState(
            Default(
                previousIsolation = createIndexCaseIsolation(
                    isolationStart = previousIsolationStart
                )
            )
        )
    }

    private fun setIndexCaseIsolation() {
        testAppContext.setState(createIndexCaseIsolation(isolationStart = isolationStart))
    }

    private fun createIndexCaseIsolation(isolationStart: Instant): Isolation {
        val isolationStartDate =
            LocalDateTime.ofInstant(isolationStart, ZoneId.systemDefault()).toLocalDate()
        return Isolation(
            isolationStart = isolationStart,
            isolationConfiguration = DurationDays(),
            indexCase = IndexCase(
                symptomsOnsetDate = isolationStartDate.minusDays(3),
                expiryDate = isolationStartDate.plusDays(7),
                selfAssessment = false
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
            testResultStorageOperation = OVERWRITE
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
        diagnosisKeySubmissionSupported: Boolean
    ) {
        val token = getConfirmedTestResultToken(testResult, diagnosisKeySubmissionSupported)
        receiveTestResult(token)
    }

    private fun receiveIndicativePositiveTestResult() {
        receiveTestResult(POSITIVE_LFD_TOKEN_INDICATIVE)
    }

    private fun receiveTestResult(token: String) {
        statusRobot.clickLinkTestResult()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken(token)

        linkTestResultRobot.clickContinue()
    }

    private fun shareKeys() {
        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickIUnderstandButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    private fun orderConfirmatoryTest() {
        testOrderingRobot.checkActivityIsDisplayed()

        testOrderingRobot.clickOrderTestButton()

        waitFor { browserRobot.checkActivityIsDisplayed() }

        browserRobot.clickCloseButton()

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
