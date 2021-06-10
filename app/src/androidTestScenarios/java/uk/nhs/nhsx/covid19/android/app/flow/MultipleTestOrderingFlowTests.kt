package uk.nhs.nhsx.covid19.android.app.flow

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.OrderTest
import uk.nhs.nhsx.covid19.android.app.remote.TestResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext.Companion.ENGLISH_LOCAL_AUTHORITY
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.retry.RetryFlakyTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestingHubRobot
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import uk.nhs.nhsx.covid19.android.app.util.IsolationChecker
import java.time.LocalDate
import kotlin.test.assertTrue

class MultipleTestOrderingFlowTests : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val orderTest = OrderTest(this)
    private val testResultRobot = TestResultRobot(testAppContext.app)
    private val shareKeysInformationRobot = ShareKeysInformationRobot()
    private val shareKeysResultRobot = ShareKeysResultRobot()
    private val testingHubRobot = TestingHubRobot()
    private val isolationHelper = IsolationHelper(testAppContext.clock)
    private val isolationChecker = IsolationChecker(testAppContext)

    @Before
    fun setUp() {
        testAppContext.setLocalAuthority(ENGLISH_LOCAL_AUTHORITY)
    }

    @Test
    fun startIndexCase_receiveNegativeAndPositiveTestResultsSequentially_shouldIsolate() = notReported {
        testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        isolationChecker.assertActiveIndexNoContact()

        val firstToken = "firstToken"
        val secondToken = "secondToken"

        orderTestFromStatusActivity(firstToken)
        orderTestFromStatusActivity(secondToken)

        testAppContext.virologyTestingApi.testResponseForPollingToken =
            mutableMapOf(firstToken to TestResponse(NEGATIVE, LAB_RESULT))

        runBackgroundTasks()

        waitFor { testResultRobot.checkActivityDisplaysNegativeWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        isolationChecker.assertExpiredIndexNoContact()

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

        isolationChecker.assertActiveIndexNoContact()
    }

    @Test
    fun startIndexCase_receiveNegativeAndNegativeTestResultsSequentially_shouldEndIsolationOnFirstNegativeTestResult() = notReported {
        testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        isolationChecker.assertActiveIndexNoContact()

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

        isolationChecker.assertExpiredIndexNoContact()

        testAppContext.virologyTestingApi.testResponseForPollingToken =
            mutableMapOf(secondToken to TestResponse(NEGATIVE, LAB_RESULT))

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        waitFor { testResultRobot.checkActivityDisplaysNegativeWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        isolationChecker.assertExpiredIndexNoContact()
    }

    @RetryFlakyTest
    @Test
    fun startIndexCase_receiveMultipleTestResultsAtTheSameTime_firstPositive_thenNegative_shouldIsolate() = notReported {
        testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        isolationChecker.assertActiveIndexNoContact()

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

        isolationChecker.assertActiveIndexNoContact()
    }

    @Test
    fun startIndexCaseWithPositiveTestResult_receiveNegativeTestResult_shouldStayInIsolation() = notReported {
        testAppContext.setState(
            isolationHelper.selfAssessment(
                testResult = AcknowledgedTestResult(
                    testEndDate = LocalDate.now(),
                    testResult = RelevantVirologyTestResult.POSITIVE,
                    testKitType = LAB_RESULT,
                    acknowledgedDate = LocalDate.now()
                )
            ).asIsolation()
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        isolationChecker.assertActiveIndexNoContact()

        orderTestFromStatusActivity("newToken")

        testAppContext.virologyTestingApi.testResponseForPollingToken =
            mutableMapOf("newToken" to TestResponse(NEGATIVE, LAB_RESULT))

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        waitFor { testResultRobot.checkActivityDisplaysNegativeAfterPositiveOrSymptomaticWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()

        isolationChecker.assertActiveIndexNoContact()
    }

    @Test
    fun startIndexCase_receivePositiveTestResult_thenVoidTestResult_thenNegativeTestResult_shouldStayInIsolation() = notReported {
        testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        isolationChecker.assertActiveIndexNoContact()

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

        isolationChecker.assertActiveIndexNoContact()

        testAppContext.virologyTestingApi.testResponseForPollingToken =
            mutableMapOf(voidTestResultToken to TestResponse(VOID, LAB_RESULT))

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        waitFor { testResultRobot.checkActivityDisplaysVoidWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()

        orderTest(negativeTestResultToken)

        isolationChecker.assertActiveIndexNoContact()

        testAppContext.virologyTestingApi.testResponseForPollingToken =
            mutableMapOf(negativeTestResultToken to TestResponse(NEGATIVE, LAB_RESULT))

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        waitFor { testResultRobot.checkActivityDisplaysNegativeAfterPositiveOrSymptomaticWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()

        isolationChecker.assertActiveIndexNoContact()
    }

    private fun orderTestFromStatusActivity(pollingToken: String) {
        statusRobot.clickTestingHub()

        testingHubRobot.checkActivityIsDisplayed()
        testingHubRobot.clickBookTest()

        orderTest(pollingToken)
    }
}
