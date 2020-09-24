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
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BrowserRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOrderingRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
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
    fun startIndexCase_receiveNegativeAndPositiveTestResultsSequentially() = notReported {
        testAppContext.setState(
            state = Isolation(
                isolationStart = Instant.now(),
                isolationConfiguration = DurationDays(),
                indexCase = IndexCase(
                    symptomsOnsetDate = LocalDate.now().minusDays(3),
                    expiryDate = LocalDate.now().plus(7, DAYS)
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

        testAppContext.getPeriodicTasks().scheduleVirologyTestResultFetching()

        waitFor { testResultRobot.checkActivityDisplaysNegativeAndFinishIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        assertTrue { testAppContext.getCurrentState() is Default }

        testAppContext.virologyTestingApi.testResultForPollingToken =
            mutableMapOf(secondToken to POSITIVE)

        testAppContext.getPeriodicTasks().scheduleVirologyTestResultFetching()

        waitFor { testResultRobot.checkActivityDisplaysPositiveAndFinishIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        assertTrue { testAppContext.getCurrentState() is Default }
    }

    @Test
    fun startIndexCase_receiveNegativeAndNegativeTestResultsSequentially() = notReported {
        testAppContext.setState(
            state = Isolation(
                isolationStart = Instant.now(),
                isolationConfiguration = DurationDays(),
                indexCase = IndexCase(
                    symptomsOnsetDate = LocalDate.now().minusDays(3),
                    expiryDate = LocalDate.now().plus(7, DAYS)
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

        testAppContext.getPeriodicTasks().scheduleVirologyTestResultFetching()

        waitFor { testResultRobot.checkActivityDisplaysNegativeAndFinishIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        assertTrue { testAppContext.getCurrentState() is Default }

        testAppContext.virologyTestingApi.testResultForPollingToken =
            mutableMapOf(secondToken to NEGATIVE)

        testAppContext.getPeriodicTasks().scheduleVirologyTestResultFetching()

        waitFor { testResultRobot.checkActivityDisplaysNegativeAndAlreadyFinishedIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        assertTrue { testAppContext.getCurrentState() is Default }
    }

    @Test
    fun startIndexCase_receiveMultipleTestResultsAtTheSameTime() = notReported {
        testAppContext.setState(
            state = Isolation(
                isolationStart = Instant.now(),
                isolationConfiguration = DurationDays(),
                indexCase = IndexCase(
                    symptomsOnsetDate = LocalDate.now().minusDays(3),
                    expiryDate = LocalDate.now().plus(7, DAYS)
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

    private fun orderTest() {
        statusRobot.clickOrderTest()

        testOrderingRobot.checkActivityIsDisplayed()

        testOrderingRobot.clickOrderTestButton()

        waitFor { browserRobot.checkActivityIsDisplayed() }

        browserRobot.clickCloseButton()
    }
}
