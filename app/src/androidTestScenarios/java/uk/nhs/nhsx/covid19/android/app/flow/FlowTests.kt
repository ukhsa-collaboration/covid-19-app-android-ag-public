package uk.nhs.nhsx.covid19.android.app.flow

import androidx.test.espresso.NoMatchingViewException
import org.awaitility.kotlin.await
import org.awaitility.kotlin.ignoreException
import org.awaitility.kotlin.until
import org.awaitility.kotlin.untilAsserted
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.PositiveSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.QuestionnaireRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ReviewSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOrderingRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlowTests : EspressoTest() {

    private val statusRobot = StatusRobot()

    private val questionnaireRobot = QuestionnaireRobot()

    private val reviewSymptomsRobot = ReviewSymptomsRobot()

    private val positiveSymptomsRobot = PositiveSymptomsRobot()

    private val testOrderingRobot = TestOrderingRobot()

    private val testResultRobot = TestResultRobot()

    @Test
    fun startDefault_endNegativeTestResult() {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertEquals(Default(), testAppContext.getCurrentState())

        statusRobot.clickReportSymptoms()

        completeQuestionnaireWithSymptoms()

        assertTrue { (testAppContext.getCurrentState() as Isolation).isIndexCaseOnly() }

        completeTestOrdering()

        statusRobot.checkIsolationViewIsDisplayed()

        testAppContext.virologyTestingApi.setDefaultTestResult(NEGATIVE)

        testAppContext.getPeriodicTasks().schedule(keepPrevious = false)

        await.atMost(10, SECONDS) until { testAppContext.getCurrentState() is Default }

        await.atMost(10, SECONDS) ignoreException NoMatchingViewException::class untilAsserted { testResultRobot.checkActivityDisplaysNegativeAndFinishIsolation() }
    }

    @Test
    fun startIndexCase_endPositiveTestResult() {
        testAppContext.setState(
            state = Isolation(
                isolationStart = Instant.now(),
                expiryDate = LocalDate.now().plus(7, ChronoUnit.DAYS),
                indexCase = IndexCase(
                    symptomsOnsetDate = LocalDate.now().minusDays(3),
                    testResult = null
                )
            )
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertTrue { (testAppContext.getCurrentState() as Isolation).isIndexCaseOnly() }

        statusRobot.clickOrderTest()

        testOrderingRobot.checkActivityIsDisplayed()

        testOrderingRobot.clickOrderTestButton()

        testAppContext.device.pressBack()

        testAppContext.virologyTestingApi.setDefaultTestResult(POSITIVE)

        testAppContext.getPeriodicTasks().schedule(keepPrevious = false)

        await.atMost(
            10,
            SECONDS
        ) until { (testAppContext.getCurrentState() as Isolation).indexCase?.testResult?.result == POSITIVE }

        await.atMost(10, SECONDS) ignoreException NoMatchingViewException::class untilAsserted { testResultRobot.checkActivityDisplaysPositiveAndContinueSelfIsolation() }

        testResultRobot.clickIsolationActionButton()

        assertTrue { testAppContext.temporaryExposureKeyHistoryWasCalled() }

        assertTrue { (testAppContext.getCurrentState() as Isolation).isIndexCaseOnly() }
    }

    private fun completeTestOrdering() {
        positiveSymptomsRobot.checkActivityIsDisplayed()

        positiveSymptomsRobot.clickTestOrderingButton()

        testOrderingRobot.checkActivityIsDisplayed()

        testOrderingRobot.clickOrderTestButton()

        testAppContext.device.pressBack()
    }

    private fun completeQuestionnaireWithSymptoms() {
        questionnaireRobot.checkActivityIsDisplayed()

        questionnaireRobot.selectSymptomsAtPositions(0, 1)

        questionnaireRobot.reviewSymptoms()

        reviewSymptomsRobot.confirmReviewSymptomsScreenIsDisplayed()

        reviewSymptomsRobot.selectCannotRememberDate()

        reviewSymptomsRobot.confirmSelection()
    }
}
