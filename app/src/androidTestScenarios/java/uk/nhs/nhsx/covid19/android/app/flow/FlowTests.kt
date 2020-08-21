package uk.nhs.nhsx.covid19.android.app.flow

import androidx.test.espresso.NoMatchingViewException
import org.awaitility.kotlin.await
import org.awaitility.kotlin.ignoreException
import org.awaitility.kotlin.until
import org.awaitility.kotlin.untilAsserted
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R.plurals
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.EncounterDetectionRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.QuestionnaireRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ReviewSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SymptomsAdviceIsolateRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOrderingRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testordering.LatestTestResult
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit.MINUTES
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

        testAppContext.getPeriodicTasks().scheduleVirologyTestResultFetching()

        await.atMost(10, SECONDS) until {
            testAppContext.getCurrentState() is Default
        }

        await.atMost(10, SECONDS) ignoreException NoMatchingViewException::class untilAsserted {
            testResultRobot.checkActivityDisplaysNegativeAndFinishIsolation()
        }
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

        testAppContext.getPeriodicTasks().scheduleVirologyTestResultFetching()

        await.atMost(10, SECONDS) until {
            (testAppContext.getCurrentState() as Isolation).indexCase?.testResult?.result == POSITIVE
        }

        await.atMost(10, SECONDS) ignoreException NoMatchingViewException::class untilAsserted {
            testResultRobot.checkActivityDisplaysPositiveAndContinueSelfIsolation()
        }

        testResultRobot.clickIsolationActionButton()

        assertTrue { testAppContext.temporaryExposureKeyHistoryWasCalled() }

        assertTrue { (testAppContext.getCurrentState() as Isolation).isIndexCaseOnly() }
    }

    @Test
    fun startIndexCase_endContactCase() {
        val dateNow = LocalDate.now()

        testAppContext.setState(
            state = Isolation(
                isolationStart = Instant.now(),
                expiryDate = dateNow.plus(7, ChronoUnit.DAYS),
                indexCase = IndexCase(
                    symptomsOnsetDate = dateNow.minusDays(3),
                    testResult = null
                )
            )
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertTrue { (testAppContext.getCurrentState() as Isolation).isIndexCaseOnly() }

        testAppContext.getPeriodicTasks().scheduleExposureCircuitBreakerInitial("test")

        await.atMost(10, MINUTES) until {
            (testAppContext.getCurrentState() as Isolation).isBothCases()
        }

        await.atMost(10, SECONDS) ignoreException NoMatchingViewException::class untilAsserted {
            encounterDetectionRobot.checkActivityIsDisplayed()
        }

        val contactCaseDays =
            testAppContext.getIsolationConfigurationProvider().durationDays.contactCase
        val expectedExpiryDate = dateNow.plus(
            contactCaseDays.toLong(),
            ChronoUnit.DAYS
        )
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
    fun startContactCase_endNegativeTestResult() {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        testAppContext.getPeriodicTasks().scheduleExposureCircuitBreakerInitial("test")

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

        testAppContext.virologyTestingApi.setDefaultTestResult(NEGATIVE)

        testAppContext.getPeriodicTasks().schedule(keepPrevious = false)

        await.atMost(10, SECONDS) until {
            testAppContext.getLatestTestResultProvider() is LatestTestResult
        }

        assertTrue { (testAppContext.getCurrentState() as Isolation).isBothCases() }

        await.atMost(10, SECONDS) ignoreException NoMatchingViewException::class untilAsserted {
            testResultRobot.checkActivityDisplaysNegativeAndContinueSelfIsolation()
        }
    }

    @Test
    fun startContactCase_endPositiveTestResult() {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        testAppContext.getPeriodicTasks().scheduleExposureCircuitBreakerInitial("test")

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

        testAppContext.getPeriodicTasks().schedule(keepPrevious = false)

        await.atMost(10, SECONDS) until {
            testAppContext.getLatestTestResultProvider() is LatestTestResult
        }

        assertTrue { (testAppContext.getCurrentState() as Isolation).isBothCases() }

        await.atMost(10, SECONDS) ignoreException NoMatchingViewException::class untilAsserted {
            testResultRobot.checkActivityDisplaysPositiveAndContinueSelfIsolation()
        }
    }

    private fun completeTestOrdering() {
        positiveSymptomsRobot.checkActivityIsDisplayed()

        positiveSymptomsRobot.clickBottomActionButton()

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
