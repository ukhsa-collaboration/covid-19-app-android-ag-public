package uk.nhs.nhsx.covid19.android.app.flow

import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import com.jeroenmols.featureflag.framework.TestSetting.USE_WEB_VIEW_FOR_INTERNAL_BROWSER
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.retry.RetryFlakyTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BrowserRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOrderingRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import java.time.temporal.ChronoUnit.HOURS
import kotlin.test.assertTrue

class MultipleTestOrderingFlowTests : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val testOrderingRobot = TestOrderingRobot()
    private val testResultRobot = TestResultRobot()
    private val shareKeysInformationRobot = ShareKeysInformationRobot()
    private val browserRobot = BrowserRobot()

    @Before
    fun setUp() {
        FeatureFlagTestHelper.enableFeatureFlag(USE_WEB_VIEW_FOR_INTERNAL_BROWSER)
    }

    @After
    fun tearDown() {
        FeatureFlagTestHelper.disableFeatureFlag(USE_WEB_VIEW_FOR_INTERNAL_BROWSER)
    }

    @Test
    fun startIndexCase_receiveNegativeAndPositiveTestResultsSequentially_shouldIsolate() = notReported {
        testAppContext.setState(
            state = Isolation(
                isolationStart = Instant.now(),
                isolationConfiguration = DurationDays(),
                indexCase = IndexCase(
                    symptomsOnsetDate = LocalDate.now().minusDays(3),
                    expiryDate = LocalDate.now().plus(7, DAYS),
                    selfAssessment = true
                )
            )
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertTrue { (testAppContext.getCurrentState() as Isolation).isIndexCaseOnly() }

        val firstToken = "firstToken"
        val secondToken = "secondToken"

        testAppContext.virologyTestingApi.pollingToken = firstToken

        orderTest()

        testAppContext.virologyTestingApi.pollingToken = secondToken

        orderTest()

        testAppContext.virologyTestingApi.testResultForPollingToken =
            mutableMapOf(firstToken to NEGATIVE)

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        waitFor { testResultRobot.checkActivityDisplaysNegativeAndFinishIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        assertTrue { testAppContext.getCurrentState() is Default }

        testAppContext.virologyTestingApi.testResultForPollingToken =
            mutableMapOf(secondToken to POSITIVE)

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        waitFor { testResultRobot.checkActivityDisplaysPositiveAndSelfIsolate() }

        testResultRobot.clickIsolationActionButton()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickIUnderstandButton()

        assertTrue { testAppContext.getCurrentState() is Isolation }
    }

    @Test
    fun startIndexCase_receiveNegativeAndNegativeTestResultsSequentially_shouldEndIsolationOnFirstNegativeTestResult() = notReported {
        testAppContext.setState(
            state = Isolation(
                isolationStart = Instant.now(),
                isolationConfiguration = DurationDays(),
                indexCase = IndexCase(
                    symptomsOnsetDate = LocalDate.now().minusDays(3),
                    expiryDate = LocalDate.now().plus(7, DAYS),
                    selfAssessment = true
                )
            )
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertTrue { (testAppContext.getCurrentState() as Isolation).isIndexCaseOnly() }

        val firstToken = "firstToken"
        val secondToken = "secondToken"

        testAppContext.virologyTestingApi.pollingToken = firstToken

        orderTest()

        testAppContext.virologyTestingApi.pollingToken = secondToken

        orderTest()

        testAppContext.virologyTestingApi.testResultForPollingToken =
            mutableMapOf(firstToken to NEGATIVE)

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        waitFor { testResultRobot.checkActivityDisplaysNegativeAndFinishIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        assertTrue { testAppContext.getCurrentState() is Default }

        testAppContext.virologyTestingApi.testResultForPollingToken =
            mutableMapOf(secondToken to NEGATIVE)

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        waitFor { testResultRobot.checkActivityDisplaysNegativeAndAlreadyFinishedIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        assertTrue { testAppContext.getCurrentState() is Default }
    }

    @RetryFlakyTest
    @Test
    fun startIndexCase_receiveMultipleTestResultsAtTheSameTime_firstPositive_thenNegative_shouldIsolate() = notReported {
        testAppContext.setState(
            state = Isolation(
                isolationStart = Instant.now(),
                isolationConfiguration = DurationDays(),
                indexCase = IndexCase(
                    symptomsOnsetDate = LocalDate.now().minusDays(3),
                    expiryDate = LocalDate.now().plus(7, DAYS),
                    selfAssessment = true
                )
            )
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertTrue { (testAppContext.getCurrentState() as Isolation).isIndexCaseOnly() }

        val firstToken = "firstToken"
        val secondToken = "secondToken"

        testAppContext.virologyTestingApi.pollingToken = firstToken
        testAppContext.virologyTestingApi.diagnosisKeySubmissionToken = firstToken

        orderTest()

        testAppContext.virologyTestingApi.pollingToken = secondToken
        testAppContext.virologyTestingApi.diagnosisKeySubmissionToken = secondToken

        orderTest()

        testAppContext.virologyTestingApi.testResultForPollingToken =
            mutableMapOf(
                firstToken to NEGATIVE,
                secondToken to POSITIVE
            )

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        waitFor { testResultRobot.checkActivityDisplaysPositiveAndContinueSelfIsolation() }

        testResultRobot.clickIsolationActionButton()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickIUnderstandButton()

        waitFor { testResultRobot.checkActivityDisplaysPositiveThenNegativeAndStayInIsolation() }

        testResultRobot.clickIsolationActionButton()

        assertTrue { testAppContext.temporaryExposureKeyHistoryWasCalled() }

        assertTrue { testAppContext.getCurrentState() is Isolation }
    }

    @Test
    fun startIndexCaseWithPositiveTestResult_receiveNegativeTestResult_shouldStayInIsolation() = notReported {
        val now = Instant.now()
        testAppContext.setState(
            state = Isolation(
                isolationStart = now.minus(1, DAYS),
                isolationConfiguration = DurationDays(),
                indexCase = IndexCase(
                    symptomsOnsetDate = LocalDate.now().minusDays(3),
                    expiryDate = LocalDate.now().plus(7, DAYS),
                    selfAssessment = true
                )
            )
        )

        testAppContext.getTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "token",
                testEndDate = now.minus(1, HOURS),
                testResult = POSITIVE,
                acknowledgedDate = now.minus(1, HOURS)
            )
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertTrue { (testAppContext.getCurrentState() as Isolation).isIndexCaseOnly() }

        testAppContext.virologyTestingApi.pollingToken = "newToken"

        orderTest()

        testAppContext.virologyTestingApi.testResultForPollingToken =
            mutableMapOf("newToken" to NEGATIVE)

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        waitFor { testResultRobot.checkActivityDisplaysPositiveThenNegativeAndStayInIsolation() }

        testResultRobot.clickIsolationActionButton()

        assertTrue { testAppContext.getCurrentState() is Isolation }
    }

    @Test
    fun startIndexCase_receivePositiveTestResult_thenVoidTestResult_thenNegativeTestResult_shouldStayInIsolation() = notReported {
        testAppContext.setState(
            state = Isolation(
                isolationStart = Instant.now(),
                isolationConfiguration = DurationDays(),
                indexCase = IndexCase(
                    symptomsOnsetDate = LocalDate.now().minusDays(3),
                    expiryDate = LocalDate.now().plus(7, DAYS),
                    selfAssessment = true
                )
            )
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertTrue { (testAppContext.getCurrentState() as Isolation).isIndexCaseOnly() }

        val positiveTestResultToken = "positiveTestResultToken"
        val voidTestResultToken = "voidTestResultToken"
        val negativeTestResultToken = "negativeTestResultToken"

        testAppContext.virologyTestingApi.pollingToken = positiveTestResultToken

        orderTest()

        testAppContext.virologyTestingApi.pollingToken = voidTestResultToken

        orderTest()

        testAppContext.virologyTestingApi.testResultForPollingToken =
            mutableMapOf(positiveTestResultToken to POSITIVE)

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        waitFor { testResultRobot.checkActivityDisplaysPositiveAndContinueSelfIsolation() }

        testResultRobot.clickIsolationActionButton()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickIUnderstandButton()

        assertTrue { testAppContext.getCurrentState() is Isolation }

        testAppContext.virologyTestingApi.testResultForPollingToken =
            mutableMapOf(voidTestResultToken to VOID)

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        waitFor { testResultRobot.checkActivityDisplaysVoidAndContinueSelfIsolation() }

        testResultRobot.clickIsolationActionButton()

        testOrderingRobot.checkActivityIsDisplayed()

        testAppContext.virologyTestingApi.pollingToken = negativeTestResultToken

        testOrderingRobot.clickOrderTestButton()

        waitFor { browserRobot.checkActivityIsDisplayed() }

        browserRobot.clickCloseButton()

        assertTrue { testAppContext.getCurrentState() is Isolation }

        testAppContext.virologyTestingApi.testResultForPollingToken =
            mutableMapOf(negativeTestResultToken to NEGATIVE)

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        waitFor { testResultRobot.checkActivityDisplaysPositiveThenNegativeAndStayInIsolation() }

        testResultRobot.clickIsolationActionButton()

        assertTrue { testAppContext.getCurrentState() is Isolation }
    }

    private fun orderTest() {
        statusRobot.clickOrderTest()

        testOrderingRobot.checkActivityIsDisplayed()

        testOrderingRobot.clickOrderTestButton()

        waitFor { browserRobot.checkActivityIsDisplayed() }

        browserRobot.clickCloseButton()
    }
}
