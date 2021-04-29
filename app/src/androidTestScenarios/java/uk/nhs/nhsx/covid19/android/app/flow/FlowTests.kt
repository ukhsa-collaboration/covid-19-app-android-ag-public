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
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.AWAIT_AT_MOST_SECONDS
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext.Companion.ENGLISH_LOCAL_AUTHORITY
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.retry.RetryFlakyTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.EncounterDetectionRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.IsolationExpirationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation.Overwrite
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

    private val isolationExpirationRobot = IsolationExpirationRobot()

    @Test
    fun startDefault_endNegativeTestResult() = notReported {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertEquals(Default(), testAppContext.getCurrentState())

        selfDiagnosis.selfDiagnosePositiveAndOrderTest(receiveResultImmediately = true)

        waitFor { statusRobot.checkIsolationViewIsDisplayed() }

        testAppContext.virologyTestingApi.setDefaultTestResponse(NEGATIVE)

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        waitFor { testResultRobot.checkActivityDisplaysNegativeWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        await.atMost(AWAIT_AT_MOST_SECONDS, SECONDS) until {
            testAppContext.getCurrentState() is Default
        }

        await.atMost(10, SECONDS) until {
            testAppContext.getCurrentState() is Default
        }
    }

    @Test
    fun startIndexCase_endPositiveTestResult() = notReported {
        testAppContext.setState(
            state = Isolation(
                isolationStart = Instant.now(),
                isolationConfiguration = DurationDays(),
                indexCase = IndexCase(
                    symptomsOnsetDate = LocalDate.now().minusDays(3),
                    expiryDate = LocalDate.now().plus(7, DAYS),
                    selfAssessment = false
                )
            )
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertTrue { (testAppContext.getCurrentState() as Isolation).isIndexCaseOnly() }

        statusRobot.clickOrderTest()

        orderTest()

        testAppContext.virologyTestingApi.setDefaultTestResponse(POSITIVE)

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation() }

        testResultRobot.clickIsolationActionButton()

        assertTrue { (testAppContext.getCurrentState() as Isolation).isIndexCaseOnly() }
    }

    @Test
    fun startIndexCase_endContactCase() = notReported {
        val dateNow = LocalDate.now()

        testAppContext.setState(
            state = Isolation(
                isolationStart = Instant.now(),
                isolationConfiguration = DurationDays(),
                indexCase = IndexCase(
                    symptomsOnsetDate = dateNow.minusDays(3),
                    expiryDate = dateNow.plus(7, DAYS),
                    selfAssessment = false
                )
            )
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertTrue { (testAppContext.getCurrentState() as Isolation).isIndexCaseOnly() }

        testAppContext.getExposureCircuitBreakerInfoProvider().add(exposureCircuitBreakerInfo)
        runBackgroundTasks()

        await.atMost(AWAIT_AT_MOST_SECONDS, SECONDS) until {
            (testAppContext.getCurrentState() as Isolation).isBothCases()
        }

        waitFor { encounterDetectionRobot.checkActivityIsDisplayed() }

        val contactCaseDays =
            testAppContext.getIsolationConfigurationProvider().durationDays.contactCase
        val expectedExpiryDate = dateNow.plus(contactCaseDays.toLong(), DAYS)
        val actualExpiryDate = (testAppContext.getCurrentState() as Isolation).expiryDate

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
    fun startIndexCaseWithPositiveIndicative_endContactCase() = notReported {
        val dateNow = LocalDate.now()

        testAppContext.setState(
            state = Isolation(
                isolationStart = Instant.now(),
                isolationConfiguration = DurationDays(),
                indexCase = IndexCase(
                    symptomsOnsetDate = dateNow.minusDays(3),
                    expiryDate = dateNow.plus(7, DAYS),
                    selfAssessment = false
                )
            )
        )
        testAppContext.getRelevantTestResultProvider().onTestResultAcknowledged(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "token",
                testEndDate = Instant.now(),
                testResult = POSITIVE,
                testKitType = RAPID_RESULT,
                diagnosisKeySubmissionSupported = false,
                requiresConfirmatoryTest = true
            ),
            testResultStorageOperation = Overwrite
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertTrue { (testAppContext.getCurrentState() as Isolation).isIndexCaseOnly() }

        testAppContext.getExposureCircuitBreakerInfoProvider().add(exposureCircuitBreakerInfo)
        runBackgroundTasks()

        await.atMost(AWAIT_AT_MOST_SECONDS, SECONDS) until {
            (testAppContext.getCurrentState() as Isolation).isBothCases()
        }

        waitFor { encounterDetectionRobot.checkActivityIsDisplayed() }

        val contactCaseDays =
            testAppContext.getIsolationConfigurationProvider().durationDays.contactCase
        val expectedExpiryDate = dateNow.plus(contactCaseDays.toLong(), DAYS)
        val actualExpiryDate = (testAppContext.getCurrentState() as Isolation).expiryDate

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
    fun startIndexCaseWithPositiveConfirmed_receiveExposure_remainIndexCase() = notReported {
        val dateNow = LocalDate.now()

        testAppContext.setState(
            state = Isolation(
                isolationStart = Instant.now(),
                isolationConfiguration = DurationDays(),
                indexCase = IndexCase(
                    symptomsOnsetDate = dateNow.minusDays(3),
                    expiryDate = dateNow.plus(7, DAYS),
                    selfAssessment = false
                )
            )
        )
        testAppContext.getRelevantTestResultProvider().onTestResultAcknowledged(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "token",
                testEndDate = Instant.now(),
                testResult = POSITIVE,
                testKitType = RAPID_RESULT,
                diagnosisKeySubmissionSupported = false,
                requiresConfirmatoryTest = false
            ),
            testResultStorageOperation = Overwrite
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertTrue { (testAppContext.getCurrentState() as Isolation).isIndexCaseOnly() }

        testAppContext.getExposureCircuitBreakerInfoProvider().add(exposureCircuitBreakerInfo)
        runBackgroundTasks()

        assertTrue((testAppContext.getCurrentState() as Isolation).isIndexCaseOnly())

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun startContactCase_endNegativeTestResult() = notReported {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        testAppContext.getExposureCircuitBreakerInfoProvider().add(exposureCircuitBreakerInfo)
        runBackgroundTasks()

        await.atMost(AWAIT_AT_MOST_SECONDS, SECONDS) until {
            testAppContext.getCurrentState() is Isolation &&
                (testAppContext.getCurrentState() as Isolation).isContactCaseOnly()
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

        assertTrue { (testAppContext.getCurrentState() as Isolation).isBothCases() }

        waitFor { testResultRobot.checkActivityDisplaysNegativeWillBeInIsolation() }
    }

    @Test
    fun startContactCase_endPositiveConfirmedTestResult() = notReported {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        testAppContext.getExposureCircuitBreakerInfoProvider().add(exposureCircuitBreakerInfo)
        runBackgroundTasks()

        await.atMost(AWAIT_AT_MOST_SECONDS, SECONDS) until {
            testAppContext.getCurrentState() is Isolation &&
                (testAppContext.getCurrentState() as Isolation).isContactCaseOnly()
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

        assertTrue { (testAppContext.getCurrentState() as Isolation).isIndexCaseOnly() }
    }

    @Test
    fun startContactCase_endPositiveIndicativeResult() = notReported {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        testAppContext.getExposureCircuitBreakerInfoProvider().add(exposureCircuitBreakerInfo)
        runBackgroundTasks()

        await.atMost(AWAIT_AT_MOST_SECONDS, SECONDS) until {
            testAppContext.getCurrentState() is Isolation &&
                (testAppContext.getCurrentState() as Isolation).isContactCaseOnly()
        }

        waitFor { encounterDetectionRobot.clickIUnderstandButton() }

        selfDiagnosis.selfDiagnosePositiveAndOrderTest(receiveResultImmediately = true)

        waitFor { statusRobot.checkIsolationViewIsDisplayed() }

        testAppContext.virologyTestingApi.setDefaultTestResponse(POSITIVE, requiresConfirmatoryTest = true)

        runBackgroundTasks()

        await.atMost(AWAIT_AT_MOST_SECONDS, SECONDS) until {
            testAppContext.getUnacknowledgedTestResultsProvider().testResults.any { it.testResult == POSITIVE }
        }

        assertTrue { (testAppContext.getCurrentState() as Isolation).isBothCases() }

        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }
    }

    @RetryFlakyTest
    @Test
    fun startIndexCase_endDefaultStateDueToExpiration() = notReported {
        val expiryDate = LocalDate.now().plus(1, DAYS)
        testAppContext.setState(
            state = Isolation(
                isolationStart = Instant.now(),
                isolationConfiguration = DurationDays(),
                indexCase = IndexCase(
                    symptomsOnsetDate = LocalDate.now().minusDays(3),
                    expiryDate = expiryDate,
                    selfAssessment = false
                )
            )
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertTrue { (testAppContext.getCurrentState() as Isolation).isIndexCaseOnly() }

        runBlocking {
            testAppContext.getDisplayStateExpirationNotification().doWork()
        }

        isolationExpirationRobot.checkActivityIsDisplayed()

        waitFor { isolationExpirationRobot.checkIsolationWillFinish(expiryDate) }

        val from = Instant.now().plus(1, DAYS)

        testAppContext.clock.currentInstant = from

        await.atMost(10, SECONDS) until { testAppContext.getCurrentState() is Default }
    }

    @Test
    fun startIndexCase_linkNegativeTestResult() = notReported {
        testAppContext.setState(
            state = Isolation(
                isolationStart = Instant.now(),
                isolationConfiguration = DurationDays(),
                indexCase = IndexCase(
                    symptomsOnsetDate = LocalDate.now().minusDays(3),
                    expiryDate = LocalDate.now().plus(7, DAYS),
                    selfAssessment = false
                )
            )
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        waitFor { statusRobot.checkIsolationViewIsDisplayed() }

        assertTrue { (testAppContext.getCurrentState() as Isolation).isIndexCaseOnly() }

        statusRobot.clickLinkTestResult()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken(NEGATIVE_PCR_TOKEN)

        linkTestResultRobot.clickContinue()

        waitFor { testResultRobot.checkActivityDisplaysNegativeWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.checkIsolationViewIsNotDisplayed()

        assertTrue { testAppContext.getCurrentState() is Default }
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
