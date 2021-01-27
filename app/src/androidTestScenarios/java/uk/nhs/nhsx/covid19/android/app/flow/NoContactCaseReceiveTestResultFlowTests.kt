package uk.nhs.nhsx.covid19.android.app.flow

import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import com.jeroenmols.featureflag.framework.TestSetting.USE_WEB_VIEW_FOR_INTERNAL_BROWSER
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.NEGATIVE_PCR_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.NEGATIVE_PCR_TOKEN_NO_KEY_SUBMISSION
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
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext.Companion.ENGLISH_LOCAL_AUTHORITY
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult

class NoContactCaseReceiveTestResultFlowTests : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val linkTestResultRobot = LinkTestResultRobot()
    private val testResultRobot = TestResultRobot()
    private val shareKeysInformationRobot = ShareKeysInformationRobot()

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
    fun whenIsolating_withoutPreviousTest_whenAcknowledgingNegativeTest_showNegativeWontBeInIsolationScreen_andEndIsolation() = notReported {
        setIndexCaseIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveTestResult(NEGATIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysNegativeWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()
        assertTrue { testAppContext.getCurrentState() is Default }
    }

    @Test
    fun whenIsolating_withoutPreviousTest_whenAcknowledgingVoidTest_showVoidWillBeInIsolation_andContinueIsolation() = notReported {
        setIndexCaseIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveTestResult(VOID, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysVoidWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()
        assertTrue { testAppContext.getCurrentState() is Isolation }
    }

    @Test
    fun whenIsolating_withoutPreviousTest_whenAcknowledgingPositiveTest_showPositiveContinueIsolation_andContinueIsolation_shareKeys() = notReported {
        setIndexCaseIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveTestResult(POSITIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation() }

        testResultRobot.clickIsolationActionButton()
        shareKeys()

        assertTrue { testAppContext.getCurrentState() is Isolation }
    }

    @Test
    fun whenIsolating_withoutPreviousTest_whenAcknowledgingPositiveTest_showPositiveContinueIsolation_andContinueIsolation_withoutKeysSharing() = notReported {
        setIndexCaseIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveTestResult(POSITIVE, diagnosisKeySubmissionSupported = false)
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation() }

        testResultRobot.clickIsolationActionButton()
        waitFor { statusRobot.checkActivityIsDisplayed() }

        assertTrue { testAppContext.getCurrentState() is Isolation }
    }

    @Test
    fun whenIsolating_withPreviousPositiveTest_whenAcknowledgingNegativeTest_showPositiveThenNegativeWillBeInIsolationScreen_andContinueIsolation() = notReported {
        setIndexCaseIsolation()
        setPreviousTest(POSITIVE)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveTestResult(NEGATIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysPositiveThenNegativeWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()
        assertTrue { testAppContext.getCurrentState() is Isolation }
    }

    @Test
    fun whenIsolating_withPreviousPositiveTest_whenAcknowledgingVoidTest_showVoidWillBeInIsolationScreen_andContinueIsolation() = notReported {
        setIndexCaseIsolation()
        setPreviousTest(POSITIVE)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveTestResult(VOID, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysVoidWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()
        assertTrue { testAppContext.getCurrentState() is Isolation }
    }

    @Test
    fun whenIsolating_withPreviousPositiveTest_whenAcknowledgingPositiveTest_showPositiveContinueIsolationScreen_andContinueIsolation() = notReported {
        setIndexCaseIsolation()
        setPreviousTest(POSITIVE)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveTestResult(POSITIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation() }

        testResultRobot.clickIsolationActionButton()
        assertTrue { testAppContext.getCurrentState() is Isolation }
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withoutPreviousTest_whenAcknowledgingNegativeTest_showNegativeNotInIsolationScreen_andNoIsolation() = notReported {
        setDefaultWithPreviousIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveTestResult(NEGATIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysNegativeNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()
        assertTrue { testAppContext.getCurrentState() is Default }
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withoutPreviousTest_whenAcknowledgingVoidTest_showVoidNotInIsolationScreen_andNoIsolation() = notReported {
        setDefaultWithPreviousIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveTestResult(VOID, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysVoidNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()
        assertTrue { testAppContext.getCurrentState() is Default }
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withoutPreviousTest_whenAcknowledgingPositiveTest_showPositiveWontBeInIsolationScreen_andNoIsolation_shareKeys() = notReported {
        setDefaultWithPreviousIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveTestResult(POSITIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()
        shareKeys()

        assertTrue { testAppContext.getCurrentState() is Default }
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withoutPreviousTest_whenAcknowledgingPositiveTest_showPositiveWontBeInIsolationScreen_andNoIsolation_withoutKeysSharing() = notReported {
        setDefaultWithPreviousIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveTestResult(POSITIVE, diagnosisKeySubmissionSupported = false)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()
        waitFor { statusRobot.checkActivityIsDisplayed() }

        assertTrue { testAppContext.getCurrentState() is Default }
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousPositiveTest_whenAcknowledgingNegativeTest_showNegativeNotInIsolationScreen_andNoIsolation() = notReported {
        setDefaultWithPreviousIsolation()
        setPreviousTest(POSITIVE)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveTestResult(NEGATIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysNegativeNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        assertTrue { testAppContext.getCurrentState() is Default }
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousPositiveTest_whenAcknowledgingVoidTest_showVoidNotInIsolationScreen_andNoIsolation() = notReported {
        setDefaultWithPreviousIsolation()
        setPreviousTest(POSITIVE)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveTestResult(VOID, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysVoidNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        assertTrue { testAppContext.getCurrentState() is Default }
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousPositiveTest_whenAcknowledgingPositiveTest_showPositiveWontBeInIsolationScreen_andNoIsolation_shareKeys() = notReported {
        setDefaultWithPreviousIsolation()
        setPreviousTest(POSITIVE)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveTestResult(POSITIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()
        shareKeys()

        assertTrue { testAppContext.getCurrentState() is Default }
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousPositiveTest_whenAcknowledgingPositiveTest_showPositiveWontBeInIsolationScreen_andNoIsolation_withoutKeysSharing() = notReported {
        setDefaultWithPreviousIsolation()
        setPreviousTest(POSITIVE)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveTestResult(POSITIVE, diagnosisKeySubmissionSupported = false)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()
        waitFor { statusRobot.checkActivityIsDisplayed() }

        assertTrue { testAppContext.getCurrentState() is Default }
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousNegativeTest_whenAcknowledgingNegativeTest_showNegativeNotInIsolationScreen_andNoIsolation() = notReported {
        setDefaultWithPreviousIsolation()
        setPreviousTest(NEGATIVE)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveTestResult(NEGATIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysNegativeNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        assertTrue { testAppContext.getCurrentState() is Default }
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousNegativeTest_whenAcknowledgingVoidTest_showVoidNotInIsolationScreen_andNoIsolation() = notReported {
        setDefaultWithPreviousIsolation()
        setPreviousTest(NEGATIVE)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveTestResult(VOID, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysVoidNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        assertTrue { testAppContext.getCurrentState() is Default }
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousNegativeTest_whenAcknowledgingPositiveTest_showPositiveWillBeInIsolationScreen_andEnterIsolation_shareKeys() = notReported {
        setDefaultWithPreviousIsolation()
        setPreviousTest(NEGATIVE)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveTestResult(POSITIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()
        shareKeys()

        assertTrue { testAppContext.getCurrentState() is Isolation }
    }

    @Test
    fun whenDefaultWithPreviousIsolation_withPreviousNegativeTest_whenAcknowledgingPositiveTest_showPositiveWillBeInIsolationScreen_andEnterIsolation_withoutKeysSharing() = notReported {
        setDefaultWithPreviousIsolation()
        setPreviousTest(NEGATIVE)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveTestResult(POSITIVE, diagnosisKeySubmissionSupported = false)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()
        waitFor { statusRobot.checkActivityIsDisplayed() }

        assertTrue { testAppContext.getCurrentState() is Isolation }
    }

    @Test
    fun whenDefaultWithoutPreviousIsolation_withoutPreviousTest_whenAcknowledgingPositiveTest_showPositiveWillBeInIsolationScreen_andEnterIsolation_shareKeys() = notReported {
        setDefaultWithoutPreviousIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveTestResult(POSITIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()
        shareKeys()

        assertTrue { testAppContext.getCurrentState() is Isolation }
    }

    @Test
    fun whenDefaultWithoutPreviousIsolation_withoutPreviousTest_whenAcknowledgingPositiveTest_showPositiveWillBeInIsolationScreen_andEnterIsolation_withoutKeysSharing() = notReported {
        setDefaultWithoutPreviousIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveTestResult(POSITIVE, diagnosisKeySubmissionSupported = false)
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()
        waitFor { statusRobot.checkActivityIsDisplayed() }

        assertTrue { testAppContext.getCurrentState() is Isolation }
    }

    @Test
    fun whenDefaultWithoutPreviousIsolation_withoutPreviousTest_whenAcknowledgingNegativeTest_showNegativeNotInIsolationScreen_andNoIsolation() = notReported {
        setDefaultWithoutPreviousIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        receiveTestResult(NEGATIVE, diagnosisKeySubmissionSupported = true)
        waitFor { testResultRobot.checkActivityDisplaysNegativeNotInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        assertTrue { testAppContext.getCurrentState() is Default }
    }

    private fun setDefaultWithoutPreviousIsolation() {
        testAppContext.setState(Default())
    }

    private fun setDefaultWithPreviousIsolation() {
        testAppContext.setState(
            state = Default(
                previousIsolation = Isolation(
                    isolationStart = Instant.now(),
                    isolationConfiguration = DurationDays(),
                    indexCase = IndexCase(
                        symptomsOnsetDate = LocalDate.now().minusDays(20),
                        expiryDate = LocalDate.now().minusDays(7),
                        selfAssessment = false
                    )
                )
            )
        )
    }

    private fun setIndexCaseIsolation() {
        testAppContext.setState(
            state = Isolation(
                isolationStart = Instant.now(),
                isolationConfiguration = DurationDays(),
                indexCase = IndexCase(
                    symptomsOnsetDate = LocalDate.now().minusDays(3),
                    expiryDate = LocalDate.now().plusDays(7),
                    selfAssessment = false
                )
            )
        )
    }

    private fun setPreviousTest(testResult: VirologyTestResult) {
        testAppContext.getRelevantTestResultProvider().onTestResultAcknowledged(
            ReceivedTestResult(
                "oldToken",
                Instant.now().minus(3, ChronoUnit.DAYS),
                testResult,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true
            )
        )
    }

    private fun receiveTestResult(testResult: VirologyTestResult, diagnosisKeySubmissionSupported: Boolean) {
        statusRobot.clickLinkTestResult()

        linkTestResultRobot.checkActivityIsDisplayed()

        val token = when (testResult) {
            POSITIVE -> if (diagnosisKeySubmissionSupported) POSITIVE_PCR_TOKEN else POSITIVE_PCR_TOKEN_NO_KEY_SUBMISSION
            NEGATIVE -> if (diagnosisKeySubmissionSupported) NEGATIVE_PCR_TOKEN else NEGATIVE_PCR_TOKEN_NO_KEY_SUBMISSION
            VOID -> if (diagnosisKeySubmissionSupported) VOID_PCR_TOKEN else VOID_PCR_TOKEN_NO_KEY_SUBMISSION
        }
        linkTestResultRobot.enterCtaToken(token)

        linkTestResultRobot.clickContinue()
    }

    private fun shareKeys() {
        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickIUnderstandButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }
}
