package uk.nhs.nhsx.covid19.android.app.flow

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.OrderTest
import uk.nhs.nhsx.covid19.android.app.remote.TestResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
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
import uk.nhs.nhsx.covid19.android.app.testhelpers.retry.RetryFlakyTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation.Overwrite
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import java.time.temporal.ChronoUnit.HOURS
import kotlin.test.assertTrue

class MultipleTestOrderingFlowTests : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val orderTest = OrderTest(this)
    private val testResultRobot = TestResultRobot(testAppContext.app)
    private val shareKeysInformationRobot = ShareKeysInformationRobot()
    private val shareKeysResultRobot = ShareKeysResultRobot()

    @Before
    fun setUp() {
        testAppContext.setLocalAuthority(ENGLISH_LOCAL_AUTHORITY)
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

        orderTestFromStatusActivity(firstToken)
        orderTestFromStatusActivity(secondToken)

        testAppContext.virologyTestingApi.testResponseForPollingToken =
            mutableMapOf(firstToken to TestResponse(NEGATIVE, LAB_RESULT))

        runBackgroundTasks()

        waitFor { testResultRobot.checkActivityDisplaysNegativeWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        assertTrue { testAppContext.getCurrentState() is Default }

        testAppContext.virologyTestingApi.testResponseForPollingToken =
            mutableMapOf(secondToken to TestResponse(POSITIVE, LAB_RESULT))

        runBackgroundTasks()

        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickContinueButton()

        waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }

        shareKeysResultRobot.clickActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }

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

        orderTestFromStatusActivity(firstToken)
        orderTestFromStatusActivity(secondToken)

        testAppContext.virologyTestingApi.testResponseForPollingToken =
            mutableMapOf(firstToken to TestResponse(NEGATIVE, LAB_RESULT))

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        waitFor { testResultRobot.checkActivityDisplaysNegativeWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        assertTrue { testAppContext.getCurrentState() is Default }

        testAppContext.virologyTestingApi.testResponseForPollingToken =
            mutableMapOf(secondToken to TestResponse(NEGATIVE, LAB_RESULT))

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        waitFor { testResultRobot.checkActivityDisplaysNegativeWontBeInIsolation() }

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

        testAppContext.virologyTestingApi.diagnosisKeySubmissionToken = firstToken
        orderTestFromStatusActivity(firstToken)

        testAppContext.virologyTestingApi.diagnosisKeySubmissionToken = secondToken
        orderTestFromStatusActivity(secondToken)

        testAppContext.virologyTestingApi.testResponseForPollingToken =
            mutableMapOf(
                firstToken to TestResponse(NEGATIVE, LAB_RESULT),
                secondToken to TestResponse(POSITIVE, LAB_RESULT)
            )

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation() }

        testResultRobot.clickIsolationActionButton()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickContinueButton()

        waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }

        shareKeysResultRobot.clickActionButton()

        waitFor { testResultRobot.checkActivityDisplaysNegativeAfterPositiveOrSymptomaticWillBeInIsolation() }

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

        testAppContext.getRelevantTestResultProvider().onTestResultAcknowledged(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "token",
                testEndDate = now.minus(1, HOURS),
                testResult = POSITIVE,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true
            ),
            testResultStorageOperation = Overwrite
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertTrue { (testAppContext.getCurrentState() as Isolation).isIndexCaseOnly() }

        orderTestFromStatusActivity("newToken")

        testAppContext.virologyTestingApi.testResponseForPollingToken =
            mutableMapOf("newToken" to TestResponse(NEGATIVE, LAB_RESULT))

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        waitFor { testResultRobot.checkActivityDisplaysNegativeAfterPositiveOrSymptomaticWillBeInIsolation() }

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

        orderTestFromStatusActivity(positiveTestResultToken)
        orderTestFromStatusActivity(voidTestResultToken)

        testAppContext.virologyTestingApi.testResponseForPollingToken =
            mutableMapOf(positiveTestResultToken to TestResponse(POSITIVE, LAB_RESULT))

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation() }

        testResultRobot.clickIsolationActionButton()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickContinueButton()

        waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }
        shareKeysResultRobot.clickActionButton()

        assertTrue { testAppContext.getCurrentState() is Isolation }

        testAppContext.virologyTestingApi.testResponseForPollingToken =
            mutableMapOf(voidTestResultToken to TestResponse(VOID, LAB_RESULT))

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        waitFor { testResultRobot.checkActivityDisplaysVoidWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()

        orderTest(negativeTestResultToken)

        assertTrue { testAppContext.getCurrentState() is Isolation }

        testAppContext.virologyTestingApi.testResponseForPollingToken =
            mutableMapOf(negativeTestResultToken to TestResponse(NEGATIVE, LAB_RESULT))

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        waitFor { testResultRobot.checkActivityDisplaysNegativeAfterPositiveOrSymptomaticWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()

        assertTrue { testAppContext.getCurrentState() is Isolation }
    }

    private fun orderTestFromStatusActivity(pollingToken: String) {
        statusRobot.clickOrderTest()
        orderTest(pollingToken)
    }
}
