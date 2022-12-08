package uk.nhs.nhsx.covid19.android.app.flow

import com.jeroenmols.featureflag.framework.FeatureFlag.SELF_REPORTING
import org.junit.After
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultOnsetDateRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeature
import uk.nhs.nhsx.covid19.android.app.util.IsolationChecker
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class LinkTestResultFlowTests : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val linkTestResultRobot = LinkTestResultRobot()
    private val linkTestResultSymptomsRobot = LinkTestResultSymptomsRobot()
    private val linkTestResultOnsetDateRobot = LinkTestResultOnsetDateRobot()
    private val testResultRobot = TestResultRobot(testAppContext.app)
    private val shareKeysInformationRobot = ShareKeysInformationRobot()
    private val shareKeysResultRobot = ShareKeysResultRobot()
    private val isolationHelper = IsolationHelper(testAppContext.clock)
    private val isolationChecker = IsolationChecker(testAppContext)

    @After
    fun tearDown() {
        testAppContext.clock.reset()
    }

    @Test
    fun startIndexCase_linkPositiveTestResult_shouldContinueIsolation() = runWithFeature(SELF_REPORTING, enabled = false) {
        testAppContext.setLocalAuthority(TestApplicationContext.WELSH_LOCAL_AUTHORITY)

        testAppContext.setState(
            isolationHelper.selfAssessment().asIsolation()
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        isolationChecker.assertActiveIndexNoContact()

        statusRobot.clickLinkTestResult()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken(MockVirologyTestingApi.POSITIVE_PCR_TOKEN)

        linkTestResultRobot.clickContinue()

        waitFor {
            testResultRobot.checkActivityDisplaysPositiveContinueIsolation(remainingDaysInIsolation = 7)
        }

        testResultRobot.clickIsolationActionButton()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickContinueButton()

        waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }

        shareKeysResultRobot.clickActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        isolationChecker.assertActiveIndexNoContact()
    }

    @Test
    fun startContactCase_linkJustExpiredPositiveTestResult_shouldEndIsolation() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            testAppContext.setLocalAuthority(TestApplicationContext.ENGLISH_LOCAL_AUTHORITY)
            val contactInstant = Instant.now(testAppContext.clock).minus(2, ChronoUnit.DAYS)
            testAppContext.virologyTestingApi.testEndDate = contactInstant.minus(10, ChronoUnit.DAYS)

            val contactDate = contactInstant.toLocalDate(testAppContext.clock.zone)
            testAppContext.setState(
                isolationHelper.contact(
                    exposureDate = contactDate,
                    notificationDate = contactDate,
                ).asIsolation()
            )

            startTestActivity<StatusActivity>()

            statusRobot.checkActivityIsDisplayed()

            isolationChecker.assertActiveContactNoIndex()

            statusRobot.clickLinkTestResult()

            linkTestResultRobot.checkActivityIsDisplayed()

            linkTestResultRobot.enterCtaToken(MockVirologyTestingApi.POSITIVE_PCR_TOKEN)

            linkTestResultRobot.clickContinue()

            linkTestResultSymptomsRobot.clickNo()

            waitFor {
                testResultRobot.checkActivityDisplaysPositiveWontBeInIsolation(ENGLAND)
            }

            testResultRobot.clickGoodNewsActionButton()

            shareKeysInformationRobot.checkActivityIsDisplayed()

            shareKeysInformationRobot.clickContinueButton()

            waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }

            shareKeysResultRobot.clickActionButton()

            waitFor { statusRobot.checkActivityIsDisplayed() }

            isolationChecker.assertExpiredIndexNoContact()
        }

    @Test
    fun startContactCase_linkTooOldPositiveTestResult_shouldContinueIsolation() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            testAppContext.setLocalAuthority(TestApplicationContext.WELSH_LOCAL_AUTHORITY)

            val contactInstant = Instant.now(testAppContext.clock).minus(2, ChronoUnit.DAYS)
            testAppContext.virologyTestingApi.testEndDate = contactInstant.minus(11, ChronoUnit.DAYS)

            val contactDate = contactInstant.toLocalDate(testAppContext.clock.zone)
            testAppContext.setState(
                isolationHelper.contact(
                    exposureDate = contactDate,
                    notificationDate = contactDate
                ).asIsolation()
            )
            val contactExpiryDate = contactDate.plusDays(DurationDays().england.contactCase.toLong())

            startTestActivity<StatusActivity>()

            statusRobot.checkActivityIsDisplayed()

            isolationChecker.assertActiveContactNoIndex()

            statusRobot.clickLinkTestResult()

            linkTestResultRobot.checkActivityIsDisplayed()

            linkTestResultRobot.enterCtaToken(MockVirologyTestingApi.POSITIVE_PCR_TOKEN)

            linkTestResultRobot.clickContinue()

            linkTestResultSymptomsRobot.clickNo()

            val remainingDaysInIsolation = ChronoUnit.DAYS.between(
                LocalDate.now(testAppContext.clock),
                contactExpiryDate
            ).toInt()
            waitFor {
                testResultRobot.checkActivityDisplaysPositiveContinueIsolation(remainingDaysInIsolation)
            }

            testResultRobot.clickIsolationActionButton()

            waitFor { statusRobot.checkActivityIsDisplayed() }

            isolationChecker.assertActiveContactNoIndex()
        }

    @Test
    fun startDefault_linkPositivePCRTestResult_noSymptoms_shouldIsolate() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            testAppContext.setLocalAuthority(TestApplicationContext.ENGLISH_LOCAL_AUTHORITY)
            startDefaultLinkPositiveTestResultNoSymptomsShouldIsolate(
                MockVirologyTestingApi.POSITIVE_PCR_TOKEN,
                ENGLAND
            )
        }

    @Test
    fun startDefault_linkPositiveLFDTestResult_noSymptoms_shouldIsolate_wales() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            testAppContext.setLocalAuthority(TestApplicationContext.WELSH_LOCAL_AUTHORITY)
            startDefaultLinkPositiveTestResultNoSymptomsShouldIsolate(
                MockVirologyTestingApi.POSITIVE_LFD_TOKEN,
                WALES
            )
        }

    @Test
    fun startDefault_linkPositivePCRTestResult_confirmSymptoms_selectSymptomsDate_shouldIsolate() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            testAppContext.setLocalAuthority(TestApplicationContext.ENGLISH_LOCAL_AUTHORITY)
            startDefaultLinkPositiveTestResultConfirmSymptomsSelectSymptomsDateShouldIsolate(
                MockVirologyTestingApi.POSITIVE_PCR_TOKEN,
                ENGLAND
            )
        }

    @Test
    fun startDefault_linkPositiveLFDTestResult_confirmSymptoms_selectSymptomsDate_shouldIsolate_wales() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            testAppContext.setLocalAuthority(TestApplicationContext.WELSH_LOCAL_AUTHORITY)
            startDefaultLinkPositiveTestResultConfirmSymptomsSelectSymptomsDateShouldIsolate(
                MockVirologyTestingApi.POSITIVE_LFD_TOKEN,
                WALES
            )
        }

    private fun startDefaultLinkPositiveTestResultConfirmSymptomsSelectSymptomsDateShouldIsolate(
        testType: String,
        postCodeDistrict: PostCodeDistrict
    ) {
        val now = Instant.parse("2021-01-10T10:00:00Z")
        testAppContext.clock.currentInstant = now
        testAppContext.virologyTestingApi.testEndDate = now.minus(2, ChronoUnit.DAYS)
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickLinkTestResult()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken(testType)

        linkTestResultRobot.clickContinue()

        waitFor { linkTestResultSymptomsRobot.checkActivityIsDisplayed() }

        linkTestResultSymptomsRobot.clickYes()

        linkTestResultOnsetDateRobot.checkActivityIsDisplayed()

        linkTestResultOnsetDateRobot.clickSelectDate()

        val onsetDate = now.toLocalDate(testAppContext.clock.zone).minusDays(3)

        linkTestResultOnsetDateRobot.selectDayOfMonth(onsetDate.dayOfMonth)

        linkTestResultOnsetDateRobot.clickContinueButton()

        waitFor {
            testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(postCodeDistrict)
        }

        testResultRobot.clickIsolationActionButton()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickContinueButton()

        waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }

        shareKeysResultRobot.clickActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        isolationChecker.assertActiveIndexNoContact()
    }

    private fun startDefaultLinkPositiveTestResultNoSymptomsShouldIsolate(
        testType: String,
        postCodeDistrict: PostCodeDistrict
    ) {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickLinkTestResult()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken(testType)

        linkTestResultRobot.clickContinue()

        waitFor { linkTestResultSymptomsRobot.checkActivityIsDisplayed() }

        linkTestResultSymptomsRobot.clickNo()

        waitFor {
            testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(postCodeDistrict)
        }

        testResultRobot.clickIsolationActionButton()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickContinueButton()

        waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }

        shareKeysResultRobot.clickActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        isolationChecker.assertActiveIndexNoContact()
    }
}
