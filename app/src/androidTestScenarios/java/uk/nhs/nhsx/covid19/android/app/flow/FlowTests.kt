package uk.nhs.nhsx.covid19.android.app.flow

import androidx.test.espresso.NoMatchingViewException
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import com.jeroenmols.featureflag.framework.TestSetting.USE_WEB_VIEW_FOR_INTERNAL_BROWSER
import kotlinx.coroutines.runBlocking
import org.awaitility.kotlin.await
import org.awaitility.kotlin.ignoreException
import org.awaitility.kotlin.until
import org.awaitility.kotlin.untilAsserted
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R.plurals
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.retry.RetryFlakyTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BrowserRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.EncounterDetectionRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.IsolationExpirationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.QuestionnaireRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ReviewSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SymptomsAdviceIsolateRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOrderingRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.DAYS
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlowTests : EspressoTest() {

    private val statusRobot = StatusRobot()

    private val questionnaireRobot = QuestionnaireRobot()

    private val reviewSymptomsRobot = ReviewSymptomsRobot()

    private val positiveSymptomsRobot = SymptomsAdviceIsolateRobot()

    private val testOrderingRobot = TestOrderingRobot()

    private val testResultRobot = TestResultRobot()

    private val encounterDetectionRobot = EncounterDetectionRobot()
    private val browserRobot = BrowserRobot()

    @Before
    fun setUp() {
        FeatureFlagTestHelper.clearFeatureFlags()
        FeatureFlagTestHelper.enableFeatureFlag(USE_WEB_VIEW_FOR_INTERNAL_BROWSER)
    }

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    private val isolationExpirationRobot = IsolationExpirationRobot()

    @Test
    fun startDefault_endNegativeTestResult() = notReported {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertEquals(Default(), testAppContext.getCurrentState())

        statusRobot.clickReportSymptoms()

        completeQuestionnaireWithSymptoms()

        assertTrue { (testAppContext.getCurrentState() as Isolation).isIndexCaseOnly() }

        completeTestOrdering()

        statusRobot.checkIsolationViewIsDisplayed()

        testAppContext.virologyTestingApi.setDefaultTestResult(NEGATIVE)

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        await.atMost(10, SECONDS) ignoreException NoMatchingViewException::class untilAsserted {
            testResultRobot.checkActivityDisplaysNegativeAndFinishIsolation()
        }

        testResultRobot.clickGoodNewsActionButton()

        await.atMost(10, SECONDS) until {
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
                    expiryDate = LocalDate.now().plus(7, ChronoUnit.DAYS)
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

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        waitFor { testResultRobot.checkActivityDisplaysPositiveAndContinueSelfIsolation() }

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
                    expiryDate = dateNow.plus(7, ChronoUnit.DAYS)
                )
            )
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertTrue { (testAppContext.getCurrentState() as Isolation).isIndexCaseOnly() }

        testAppContext.getExposureNotificationTokenProvider().add("test")
        testAppContext.getPeriodicTasks().schedule()

        await.atMost(10, SECONDS) until {
            (testAppContext.getCurrentState() as Isolation).isBothCases()
        }

        waitFor { encounterDetectionRobot.checkActivityIsDisplayed() }

        val contactCaseDays =
            testAppContext.getIsolationConfigurationProvider().durationDays.contactCase
        val expectedExpiryDate = dateNow.plus(contactCaseDays.toLong(), ChronoUnit.DAYS)
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
    fun startContactCase_endNegativeTestResult() = notReported {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        testAppContext.getExposureNotificationTokenProvider().add("test")
        testAppContext.getPeriodicTasks().schedule()

        await.atMost(10, SECONDS) until {
            testAppContext.getCurrentState() is Isolation &&
                (testAppContext.getCurrentState() as Isolation).isContactCaseOnly()
        }

        waitFor { encounterDetectionRobot.checkActivityIsDisplayed() }

        encounterDetectionRobot.clickIUnderstandButton()

        statusRobot.clickReportSymptoms()

        completeQuestionnaireWithSymptoms()

        assertTrue { (testAppContext.getCurrentState() as Isolation).isBothCases() }

        completeTestOrdering()

        statusRobot.checkIsolationViewIsDisplayed()

        testAppContext.virologyTestingApi.setDefaultTestResult(NEGATIVE)

        testAppContext.getPeriodicTasks().schedule()

        await.atMost(10, SECONDS) until {
            testAppContext.getTestResultsProvider().testResults.values.any { it.testResult == NEGATIVE }
        }

        assertTrue { (testAppContext.getCurrentState() as Isolation).isBothCases() }

        await.atMost(10, SECONDS) ignoreException NoMatchingViewException::class untilAsserted {
            testResultRobot.checkActivityDisplaysNegativeAndContinueSelfIsolation()
        }
    }

    @Test
    fun startContactCase_endPositiveTestResult() = notReported {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        testAppContext.getExposureNotificationTokenProvider().add("test")
        testAppContext.getPeriodicTasks().schedule()

        await.atMost(10, SECONDS) until {
            testAppContext.getCurrentState() is Isolation &&
                (testAppContext.getCurrentState() as Isolation).isContactCaseOnly()
        }

        await.atMost(10, SECONDS) ignoreException NoMatchingViewException::class untilAsserted {
            encounterDetectionRobot.clickIUnderstandButton()
        }

        statusRobot.clickReportSymptoms()

        completeQuestionnaireWithSymptoms()

        assertTrue { (testAppContext.getCurrentState() as Isolation).isBothCases() }

        completeTestOrdering()

        statusRobot.checkIsolationViewIsDisplayed()

        testAppContext.virologyTestingApi.setDefaultTestResult(POSITIVE)

        testAppContext.getPeriodicTasks().schedule()

        await.atMost(10, SECONDS) until {
            testAppContext.getTestResultsProvider().testResults.values.any { it.testResult == POSITIVE }
        }

        assertTrue { (testAppContext.getCurrentState() as Isolation).isBothCases() }

        await.atMost(10, SECONDS) ignoreException NoMatchingViewException::class untilAsserted {
            testResultRobot.checkActivityDisplaysPositiveAndContinueSelfIsolation()
        }
    }

    @RetryFlakyTest
    @Test
    fun startIndexCase_endDefaultStateDueToExpiration() {
        testAppContext.setState(
            state = Isolation(
                isolationStart = Instant.now(),
                isolationConfiguration = DurationDays(),
                indexCase = IndexCase(
                    symptomsOnsetDate = LocalDate.now().minusDays(3),
                    expiryDate = LocalDate.now().plus(1, DAYS)
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

        await.atMost(
            10,
            SECONDS
        ) ignoreException NoMatchingViewException::class untilAsserted {
            isolationExpirationRobot.checkIsolationWillFinish()
        }

        val from = Instant.now().plus(1, ChronoUnit.DAYS)

        val fromClock = Clock.fixed(from, ZoneOffset.UTC)

        testAppContext.setStateMachineClock(fromClock)

        await.atMost(10, SECONDS) until { testAppContext.getCurrentState() is Default }
    }

    private fun completeTestOrdering() = notReported {
        positiveSymptomsRobot.checkActivityIsDisplayed()

        positiveSymptomsRobot.clickBottomActionButton()

        testOrderingRobot.checkActivityIsDisplayed()

        testOrderingRobot.clickOrderTestButton()

        waitFor { browserRobot.checkActivityIsDisplayed() }

        browserRobot.clickCloseButton()
    }

    private fun completeQuestionnaireWithSymptoms() = notReported {
        questionnaireRobot.checkActivityIsDisplayed()

        questionnaireRobot.selectSymptomsAtPositions(0, 1)

        questionnaireRobot.reviewSymptoms()

        reviewSymptomsRobot.confirmReviewSymptomsScreenIsDisplayed()

        reviewSymptomsRobot.selectCannotRememberDate()

        reviewSymptomsRobot.confirmSelection()
    }
}
