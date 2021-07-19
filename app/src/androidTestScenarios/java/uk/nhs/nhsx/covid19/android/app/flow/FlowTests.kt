package uk.nhs.nhsx.covid19.android.app.flow

import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import com.jeroenmols.featureflag.framework.TestSetting.USE_WEB_VIEW_FOR_INTERNAL_BROWSER
import kotlinx.coroutines.runBlocking
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R.plurals
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureCircuitBreakerInfo
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.OrderTest
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.SelfDiagnosis
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.NEGATIVE_PCR_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.AWAIT_AT_MOST_SECONDS
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext.Companion.ENGLISH_LOCAL_AUTHORITY
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.EncounterDetectionRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestingHubRobot
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlowTests : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val testResultRobot = TestResultRobot(testAppContext.app)
    private val linkTestResultRobot = LinkTestResultRobot()
    private val encounterDetectionRobot = EncounterDetectionRobot()
    private val shareKeysInformationRobot = ShareKeysInformationRobot()
    private val orderTest = OrderTest(this)
    private val selfDiagnosis = SelfDiagnosis(this)
    private val shareKeysResultRobot = ShareKeysResultRobot()
    private val testingHubRobot = TestingHubRobot()
    private val isolationHelper = IsolationHelper(testAppContext.clock)

    @Before
    fun setUp() {
        FeatureFlagTestHelper.clearFeatureFlags()
        FeatureFlagTestHelper.enableFeatureFlag(USE_WEB_VIEW_FOR_INTERNAL_BROWSER)

        testAppContext.setLocalAuthority(ENGLISH_LOCAL_AUTHORITY)
    }

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
        testAppContext.clock.reset()
    }

    @Test
    fun startDefault_selfDiagnose_receiveNegative_notInIsolation() {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertEquals(isolationHelper.neverInIsolation(), testAppContext.getCurrentState())

        selfDiagnosis.selfDiagnosePositiveAndOrderTest(receiveResultImmediately = true)

        waitFor { statusRobot.checkIsolationViewIsDisplayed() }

        testAppContext.virologyTestingApi.setDefaultTestResponse(NEGATIVE)

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        waitFor { testResultRobot.checkActivityDisplaysNegativeWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        await.atMost(AWAIT_AT_MOST_SECONDS, SECONDS) until {
            !testAppContext.getCurrentLogicalState().isActiveIsolation(testAppContext.clock)
        }
    }

    @Test
    fun startIndexCase_receivePositiveTestResult_inIndexIsolation() {
        testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertTrue(isActiveIndexNoContact())

        statusRobot.clickTestingHub()

        testingHubRobot.checkActivityIsDisplayed()
        testingHubRobot.clickBookTest()

        orderTest()

        testAppContext.virologyTestingApi.setDefaultTestResponse(POSITIVE)

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation() }

        testResultRobot.clickIsolationActionButton()

        assertTrue(isActiveIndexNoContact())
    }

    @Test
    fun startIndexCaseWithSelfAssessment_receiveExposureNotification_inIndexAndContactIsolation() {
        val dateNow = LocalDate.now()

        testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertTrue(isActiveIndexNoContact())

        testAppContext.getExposureCircuitBreakerInfoProvider().add(exposureCircuitBreakerInfo)
        runBackgroundTasks()

        await.atMost(AWAIT_AT_MOST_SECONDS, SECONDS) until {
            isActiveIndexAndContact()
        }

        waitFor { encounterDetectionRobot.checkActivityIsDisplayed() }

        val contactCaseDays =
            testAppContext.getIsolationConfigurationProvider().durationDays.contactCase
        val expectedExpiryDate = dateNow.plus(contactCaseDays.toLong(), DAYS)
        val actualExpiryDate = (testAppContext.getCurrentLogicalState() as PossiblyIsolating).expiryDate

        assertEquals(expectedExpiryDate, actualExpiryDate)

        encounterDetectionRobot.checkNumberOfDaysTextIs(
            testAppContext.app.resources.getQuantityString(
                plurals.state_isolation_days,
                contactCaseDays,
                contactCaseDays
            )
        )
    }

    @Test
    fun startIndexCaseWithPositiveIndicative_receiveExposureNotification_inIndexAndContactIsolation() {
        val dateNow = LocalDate.now()
        testAppContext.setState(
            isolationHelper.positiveTest(
                AcknowledgedTestResult(
                    testEndDate = LocalDate.now(),
                    testResult = RelevantVirologyTestResult.POSITIVE,
                    testKitType = RAPID_RESULT,
                    requiresConfirmatoryTest = true,
                    acknowledgedDate = LocalDate.now()
                )
            ).asIsolation()
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertTrue(isActiveIndexNoContact())

        testAppContext.getExposureCircuitBreakerInfoProvider().add(exposureCircuitBreakerInfo)
        runBackgroundTasks()

        await.atMost(AWAIT_AT_MOST_SECONDS, SECONDS) until {
            isActiveIndexAndContact()
        }

        waitFor { encounterDetectionRobot.checkActivityIsDisplayed() }

        val contactCaseDays =
            testAppContext.getIsolationConfigurationProvider().durationDays.contactCase
        val expectedExpiryDate = dateNow.plus(contactCaseDays.toLong(), DAYS)
        val actualExpiryDate = (testAppContext.getCurrentLogicalState() as PossiblyIsolating).expiryDate

        assertEquals(expectedExpiryDate, actualExpiryDate)

        encounterDetectionRobot.checkNumberOfDaysTextIs(
            testAppContext.app.resources.getQuantityString(
                plurals.state_isolation_days,
                contactCaseDays,
                contactCaseDays
            )
        )
    }

    @Test
    fun startIndexCaseWithPositiveConfirmed_receiveExposureNotification_remainIndexCase() {
        testAppContext.setState(
            isolationHelper.positiveTest(
                AcknowledgedTestResult(
                    testEndDate = LocalDate.now(),
                    testResult = RelevantVirologyTestResult.POSITIVE,
                    testKitType = RAPID_RESULT,
                    requiresConfirmatoryTest = false,
                    acknowledgedDate = LocalDate.now()
                )
            ).asIsolation()
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertTrue(isActiveIndexNoContact())

        testAppContext.getExposureCircuitBreakerInfoProvider().add(exposureCircuitBreakerInfo)
        runBackgroundTasks()

        assertTrue(isActiveIndexNoContact())

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun startContactCase_selfDiagnose_receiveNegativeTestResult_inContactIsolation() {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        testAppContext.getExposureCircuitBreakerInfoProvider().add(exposureCircuitBreakerInfo)
        runBackgroundTasks()

        await.atMost(AWAIT_AT_MOST_SECONDS, SECONDS) until {
            isActiveContactNoIndex()
        }

        waitFor { encounterDetectionRobot.checkActivityIsDisplayed() }

        encounterDetectionRobot.clickIUnderstandButton()

        selfDiagnosis.selfDiagnosePositiveAndOrderTest(receiveResultImmediately = true)

        testAppContext.virologyTestingApi.setDefaultTestResponse(NEGATIVE)

        waitFor { statusRobot.checkIsolationViewIsDisplayed() }

        runBackgroundTasks()

        await.atMost(AWAIT_AT_MOST_SECONDS, SECONDS) until {
            testAppContext.getUnacknowledgedTestResultsProvider().testResults.any { it.testResult == NEGATIVE }
        }

        assertTrue(isActiveIndexAndContact())

        waitFor { testResultRobot.checkActivityDisplaysNegativeWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        assertTrue(isActiveContactAndExpiredIndex())
    }

    @Test
    fun startContactCase_selfDiagnose_receivePositiveConfirmedTestResult_inIndexIsolation() {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        testAppContext.getExposureCircuitBreakerInfoProvider().add(exposureCircuitBreakerInfo)
        runBackgroundTasks()

        await.atMost(AWAIT_AT_MOST_SECONDS, SECONDS) until {
            isActiveContactNoIndex()
        }

        waitFor { encounterDetectionRobot.clickIUnderstandButton() }

        selfDiagnosis.selfDiagnosePositiveAndOrderTest(receiveResultImmediately = true)

        waitFor { statusRobot.checkIsolationViewIsDisplayed() }

        testAppContext.virologyTestingApi.setDefaultTestResponse(POSITIVE, requiresConfirmatoryTest = false)

        runBackgroundTasks()

        await.atMost(AWAIT_AT_MOST_SECONDS, SECONDS) until {
            testAppContext.getUnacknowledgedTestResultsProvider().testResults.any { it.testResult == POSITIVE }
        }

        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation() }

        testResultRobot.clickIsolationActionButton()

        waitFor { shareKeysInformationRobot.checkActivityIsDisplayed() }

        shareKeysInformationRobot.clickContinueButton()

        waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }

        shareKeysResultRobot.clickActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        assertTrue(isActiveIndexNoContact())
    }

    @Test
    fun startContactCase_selfDiagnose_receivePositiveIndicativeTestResultWithKeySharingNotSupported_inIndexAndContactIsolation() {
        startContactCase_selfDiagnose()

        testAppContext.virologyTestingApi.setDefaultTestResponse(
            POSITIVE,
            requiresConfirmatoryTest = true,
            diagnosisKeySubmissionSupported = false
        )

        runBackgroundTasks()

        await.atMost(AWAIT_AT_MOST_SECONDS, SECONDS) until {
            testAppContext.getUnacknowledgedTestResultsProvider().testResults.any { it.testResult == POSITIVE }
        }

        assertTrue(isActiveIndexAndContact())

        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }
    }

    @Test
    fun startContactCase_selfDiagnose_receivePositiveIndicativeTestResultWithKeySharingSupported_inIndexAndContactIsolation() {
        startContactCase_selfDiagnose()

        testAppContext.virologyTestingApi.setDefaultTestResponse(
            POSITIVE,
            requiresConfirmatoryTest = true,
            diagnosisKeySubmissionSupported = true
        )

        runBackgroundTasks()

        await.atMost(AWAIT_AT_MOST_SECONDS, SECONDS) until {
            testAppContext.getUnacknowledgedTestResultsProvider().testResults.any { it.testResult == POSITIVE }
        }

        assertTrue(isActiveIndexAndContact())

        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation() }
    }

    private fun startContactCase_selfDiagnose() {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        testAppContext.getExposureCircuitBreakerInfoProvider().add(exposureCircuitBreakerInfo)
        runBackgroundTasks()

        await.atMost(AWAIT_AT_MOST_SECONDS, SECONDS) until {
            isActiveContactNoIndex()
        }

        waitFor { encounterDetectionRobot.clickIUnderstandButton() }

        selfDiagnosis.selfDiagnosePositiveAndOrderTest(receiveResultImmediately = true)

        waitFor { statusRobot.checkIsolationViewIsDisplayed() }
    }

    @Test
    fun startIndexCase_linkNegativeTestResult() {
        testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        waitFor { statusRobot.checkIsolationViewIsDisplayed() }

        assertTrue(isActiveIndexNoContact())

        statusRobot.clickLinkTestResult()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken(NEGATIVE_PCR_TOKEN)

        linkTestResultRobot.clickContinue()

        waitFor { testResultRobot.checkActivityDisplaysNegativeWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.checkIsolationViewIsNotDisplayed()

        assertTrue(isExpiredIndexNoContact())
    }

    private fun isActiveIndexAndContact(): Boolean {
        val state = testAppContext.getCurrentLogicalState()
        return state.isActiveIndexCase(testAppContext.clock) &&
            state.isActiveContactCase(testAppContext.clock)
    }

    private fun isActiveIndexNoContact(): Boolean {
        val state = testAppContext.getCurrentLogicalState()
        return state.isActiveIndexCase(testAppContext.clock) &&
            !state.remembersContactCase()
    }

    private fun isExpiredIndexNoContact(): Boolean {
        val state = testAppContext.getCurrentLogicalState()
        return state.remembersIndexCase() &&
            !state.isActiveIndexCase(testAppContext.clock) &&
            !state.remembersContactCase()
    }

    private fun isActiveContactNoIndex(): Boolean {
        val state = testAppContext.getCurrentLogicalState()
        return state.isActiveContactCase(testAppContext.clock) &&
            !state.remembersIndexCase()
    }

    private fun isActiveContactAndExpiredIndex(): Boolean {
        val state = testAppContext.getCurrentLogicalState()
        return state.isActiveContactCase(testAppContext.clock) &&
            state.remembersIndexCase() &&
            !state.isActiveIndexCase(testAppContext.clock)
    }

    companion object {
        private val exposureCircuitBreakerInfo = ExposureCircuitBreakerInfo(
            maximumRiskScore = 10.0,
            startOfDayMillis = Instant.now().toEpochMilli(),
            matchedKeyCount = 1,
            riskCalculationVersion = 2,
            exposureNotificationDate = Instant.now().toEpochMilli()
        )
    }
}
