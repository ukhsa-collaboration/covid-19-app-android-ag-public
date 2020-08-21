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
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOrderingRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.test.assertTrue

class MultipleTestOrderingFlowTests : EspressoTest() {
    private val statusRobot = StatusRobot()

    private val testOrderingRobot = TestOrderingRobot()

    private val testResultRobot = TestResultRobot()

    @Test
    fun startIndexCase_receiveMultipleTestResults() {
        testAppContext.setState(
            state = Isolation(
                isolationStart = Instant.now(),
                expiryDate = LocalDate.now().plus(7, DAYS),
                indexCase = IndexCase(
                    symptomsOnsetDate = LocalDate.now().minusDays(3),
                    testResult = null
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

        testAppContext.virologyTestingApi.testResultForPollingToken = mutableMapOf(firstToken to NEGATIVE)

        testAppContext.getPeriodicTasks().scheduleVirologyTestResultFetching()

        await.atMost(
            10,
            SECONDS
        ) until { testAppContext.getCurrentState() is Default }

        testResultRobot.checkActivityDisplaysNegativeAndFinishIsolation()

        testResultRobot.clickGoodNewsActionButton()

        testAppContext.virologyTestingApi.testResultForPollingToken[secondToken] = POSITIVE

        testAppContext.getPeriodicTasks().scheduleVirologyTestResultFetching()

        await.atMost(10, SECONDS) ignoreException NoMatchingViewException::class untilAsserted {
            testResultRobot.checkActivityDisplaysPositiveAndFinishIsolation()
        }

        testResultRobot.clickGoodNewsActionButton()

        assertTrue { testAppContext.temporaryExposureKeyHistoryWasCalled() }

        assertTrue { testAppContext.getCurrentState() is Default }
    }

    private fun orderTest() {
        statusRobot.clickOrderTest()

        testOrderingRobot.checkActivityIsDisplayed()

        testOrderingRobot.clickOrderTestButton()

        testAppContext.device.pressBack()
    }
}
