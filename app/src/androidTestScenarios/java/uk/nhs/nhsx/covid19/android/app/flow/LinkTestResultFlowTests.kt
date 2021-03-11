package uk.nhs.nhsx.covid19.android.app.flow

import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultOnsetDateRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.test.assertTrue

class LinkTestResultFlowTests : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val linkTestResultRobot = LinkTestResultRobot()
    private val linkTestResultSymptomsRobot = LinkTestResultSymptomsRobot()
    private val linkTestResultOnsetDateRobot = LinkTestResultOnsetDateRobot()
    private val testResultRobot = TestResultRobot(testAppContext.app)
    private val shareKeysInformationRobot = ShareKeysInformationRobot()

    @Before
    fun setUp() {
        testAppContext.setLocalAuthority(TestApplicationContext.ENGLISH_LOCAL_AUTHORITY)
    }

    @Test
    fun startIndexCase_linkTestResult_shouldContinueIsolate() = notReported {
        testAppContext.setState(
            state = Isolation(
                isolationStart = Instant.now(),
                isolationConfiguration = DurationDays(),
                indexCase = IndexCase(
                    symptomsOnsetDate = LocalDate.now().minusDays(3),
                    expiryDate = LocalDate.now().plus(7, ChronoUnit.DAYS),
                    selfAssessment = true
                )
            )
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertTrue { (testAppContext.getCurrentState() as Isolation).isIndexCaseOnly() }

        statusRobot.clickLinkTestResult()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken(MockVirologyTestingApi.POSITIVE_PCR_TOKEN)

        linkTestResultRobot.clickContinue()

        waitFor {
            testResultRobot.checkActivityDisplaysPositiveContinueIsolation(remainingDaysInIsolation = 7)
        }

        testResultRobot.clickIsolationActionButton()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickIUnderstandButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        assertTrue { testAppContext.getCurrentState() is Isolation }
    }

    @Test
    fun startContactCase_linkTestResult_shouldEndIsolation() = notReported {
        val contactDate = Instant.now().minus(2, ChronoUnit.DAYS)
        testAppContext.virologyTestingApi.testEndDate = contactDate.minus(10, ChronoUnit.DAYS)

        testAppContext.setState(
            state = Isolation(
                isolationStart = contactDate,
                isolationConfiguration = DurationDays(),
                contactCase = ContactCase(
                    startDate = contactDate,
                    notificationDate = null,
                    expiryDate = contactDate.plus(10, ChronoUnit.DAYS).toLocalDate(ZoneOffset.UTC)
                )
            )
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertTrue { (testAppContext.getCurrentState() as Isolation).isContactCaseOnly() }

        statusRobot.clickLinkTestResult()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken(MockVirologyTestingApi.POSITIVE_PCR_TOKEN)

        linkTestResultRobot.clickContinue()

        linkTestResultSymptomsRobot.clickNo()

        waitFor {
            testResultRobot.checkActivityDisplaysPositiveWontBeInIsolation()
        }

        testResultRobot.clickGoodNewsActionButton()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickIUnderstandButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        assertTrue { testAppContext.getCurrentState() is Default }
    }

    @Test
    fun startContactCase_linkOldTestResult_shouldContinueIsolate() = notReported {
        val contactDate = Instant.now().minus(2, ChronoUnit.DAYS)
        testAppContext.virologyTestingApi.testEndDate = contactDate.minus(11, ChronoUnit.DAYS)

        testAppContext.setState(
            state = Isolation(
                isolationStart = contactDate,
                isolationConfiguration = DurationDays(),
                contactCase = ContactCase(
                    startDate = contactDate,
                    notificationDate = null,
                    expiryDate = contactDate.plus(10, ChronoUnit.DAYS).toLocalDate(ZoneOffset.UTC)
                )
            )
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertTrue { (testAppContext.getCurrentState() as Isolation).isContactCaseOnly() }

        statusRobot.clickLinkTestResult()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken(MockVirologyTestingApi.POSITIVE_PCR_TOKEN)

        linkTestResultRobot.clickContinue()

        linkTestResultSymptomsRobot.clickNo()

        waitFor {
            testResultRobot.checkActivityDisplaysPositiveContinueIsolation(remainingDaysInIsolation = 8)
        }

        testResultRobot.clickIsolationActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        assertTrue { (testAppContext.getCurrentState() as Isolation).isContactCaseOnly() }
    }

    @Test
    fun startDefault_linkTestResult_noSymptoms_shouldIsolate() = notReported {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickLinkTestResult()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken(MockVirologyTestingApi.POSITIVE_PCR_TOKEN)

        linkTestResultRobot.clickContinue()

        waitFor { linkTestResultSymptomsRobot.checkActivityIsDisplayed() }

        linkTestResultSymptomsRobot.clickNo()

        waitFor {
            testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(remainingDaysInIsolation = 9)
        }

        testResultRobot.clickIsolationActionButton()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickIUnderstandButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        assertTrue { testAppContext.getCurrentState() is Isolation }
    }

    @Test
    fun startDefault_linkTestResult_confirmSymptoms_selectSymptomsDate_shouldIsolate() = notReported {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickLinkTestResult()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken(MockVirologyTestingApi.POSITIVE_PCR_TOKEN)

        linkTestResultRobot.clickContinue()

        waitFor { linkTestResultSymptomsRobot.checkActivityIsDisplayed() }

        linkTestResultSymptomsRobot.clickYes()

        linkTestResultOnsetDateRobot.checkActivityIsDisplayed()

        linkTestResultOnsetDateRobot.clickSelectDate()

        val now = LocalDate.now()
        val onsetDate = now.minusDays(3)
        if (onsetDate.monthValue < now.monthValue) {
            linkTestResultOnsetDateRobot.selectPreviousMonth()
            waitFor { linkTestResultOnsetDateRobot.checkDayIsEnabled(onsetDate.dayOfMonth) }
        }
        linkTestResultOnsetDateRobot.selectDayOfMonth(onsetDate.dayOfMonth)

        linkTestResultOnsetDateRobot.clickContinueButton()

        waitFor {
            testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(remainingDaysInIsolation = 8)
        }

        testResultRobot.clickIsolationActionButton()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickIUnderstandButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        assertTrue { testAppContext.getCurrentState() is Isolation }
    }
}
