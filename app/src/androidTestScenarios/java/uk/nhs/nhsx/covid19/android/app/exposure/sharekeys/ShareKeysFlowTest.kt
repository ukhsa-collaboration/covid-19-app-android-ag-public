package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import com.jeroenmols.featureflag.framework.FeatureFlag.SELF_REPORTING
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.exposure.executeWithTheUserDecliningExposureKeySharing
import uk.nhs.nhsx.covid19.android.app.exposure.setTemporaryExposureKeyHistoryResolutionRequired
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.AppWillNotNotifyOtherUsersRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelectTestDateRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportAdviceRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportCheckAnswersRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportThankYouRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysReminderRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestKitTypeRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestTypeRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeature
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.LocalAuthoritySetupHelper
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.SymptomsDate
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ShareKeysFlowTest : EspressoTest(), LocalAuthoritySetupHelper {

    private val testResultRobot = TestResultRobot(testAppContext.app)
    private val shareKeysInformationRobot = ShareKeysInformationRobot()
    private val shareKeysReminderRobot = ShareKeysReminderRobot()
    private val shareKeysResultRobot = ShareKeysResultRobot()
    private val statusRobot = StatusRobot()
    private val selfReportTestTypeRobot = TestTypeRobot()
    private val selfReportCheckAnswersRobot = SelfReportCheckAnswersRobot()
    private val selfReportThankYouRobot = SelfReportThankYouRobot()
    private val selfReportAdviceRobot = SelfReportAdviceRobot()
    private val selfReportShareKeysInformationRobot = SelfReportShareKeysInformationRobot()
    private val selfReportTestKitTypeRobot = TestKitTypeRobot()
    private val selfReportAppWillNotNotifyOtherUsersRobot = AppWillNotNotifyOtherUsersRobot()
    private val selfReportSelectTestDateRobot = SelectTestDateRobot()
    private val selfReportSymptomsRobot = SelfReportSymptomsRobot()

    private val receivedTestResult = ReceivedTestResult(
        diagnosisKeySubmissionToken = "token",
        testEndDate = testAppContext.clock.instant(),
        testResult = POSITIVE,
        testKitType = LAB_RESULT,
        diagnosisKeySubmissionSupported = true,
        requiresConfirmatoryTest = false,
        symptomsOnsetDate = SymptomsDate(LocalDate.now(testAppContext.clock).minusDays(2))
    )

    @Before
    fun setUp() {
        givenLocalAuthorityIsInEngland()
    }

    @Test
    fun whenSuccessfullySharingKeys_ShareKeysInfoIsRemoved() {
        testAppContext.getUnacknowledgedTestResultsProvider().add(receivedTestResult)
        startTestActivity<StatusActivity>()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(ENGLAND) }
        testResultRobot.clickIsolationActionButton()
        assertNotNull(testAppContext.getKeySharingInfoProvider().keySharingInfo)
        shareKeysInformationRobot.checkActivityIsDisplayed()
        shareKeysInformationRobot.clickContinueButton()
        assertNull(testAppContext.getKeySharingInfoProvider().keySharingInfo)
    }

    @Test
    fun whenAppIsLaunched_andInitialFlowWasNotCompleted_shouldShowInitialKeySharingFlow() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            testAppContext.getUnacknowledgedTestResultsProvider().add(receivedTestResult)
            startTestActivity<StatusActivity>()
            waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(ENGLAND) }
            testResultRobot.clickIsolationActionButton()
            assertNotNull(testAppContext.getKeySharingInfoProvider().keySharingInfo)

            startTestActivity<StatusActivity>()
            shareKeysInformationRobot.checkActivityIsDisplayed()
            shareKeysInformationRobot.clickContinueButton()
            shareKeysResultRobot.checkActivityIsDisplayed()
            shareKeysResultRobot.clickActionButton()
            statusRobot.checkActivityIsDisplayed()
            assertNull(testAppContext.getKeySharingInfoProvider().keySharingInfo)
        }

    @Test
    fun whenAppIsLaunched_andInitialFlowIsDeniedMoreThan24HoursLater_shouldRemoveShareKeysInfo() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            testAppContext.getUnacknowledgedTestResultsProvider().add(receivedTestResult)
            startTestActivity<StatusActivity>()
            waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(ENGLAND) }
            testResultRobot.clickIsolationActionButton()
            assertNotNull(testAppContext.getKeySharingInfoProvider().keySharingInfo)
            testAppContext.clock.currentInstant = testAppContext.clock.instant().plus(25, ChronoUnit.HOURS)

            startTestActivity<StatusActivity>()
            testAppContext.setTemporaryExposureKeyHistoryResolutionRequired(testAppContext.app, false)
            shareKeysInformationRobot.checkActivityIsDisplayed()
            shareKeysInformationRobot.clickContinueButton()
            waitFor { statusRobot.checkActivityIsDisplayed() }
            assertNull(testAppContext.getKeySharingInfoProvider().keySharingInfo)

            startTestActivity<StatusActivity>()
            statusRobot.checkActivityIsDisplayed()
        }

    @Test
    fun whenDeclineSharingKeysInitially_whenAppIsLaunchedMoreThan24HoursLater_showReminderScreen() {
        testAppContext.getUnacknowledgedTestResultsProvider().add(receivedTestResult)
        startTestActivity<StatusActivity>()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(ENGLAND) }
        testResultRobot.clickIsolationActionButton()
        assertNotNull(testAppContext.getKeySharingInfoProvider().keySharingInfo)
        testAppContext.setTemporaryExposureKeyHistoryResolutionRequired(testAppContext.app, false)
        shareKeysInformationRobot.checkActivityIsDisplayed()
        shareKeysInformationRobot.clickContinueButton()
        testAppContext.clock.currentInstant = testAppContext.clock.instant().plus(25, ChronoUnit.HOURS)

        startTestActivity<StatusActivity>()
        testAppContext.setTemporaryExposureKeyHistoryResolutionRequired(testAppContext.app, true)
        shareKeysReminderRobot.checkActivityIsDisplayed()
        shareKeysReminderRobot.clickShareResultsButton()
        shareKeysResultRobot.checkActivityIsDisplayed()
        assertNull(testAppContext.getKeySharingInfoProvider().keySharingInfo)
        shareKeysResultRobot.clickActionButton()
        statusRobot.checkActivityIsDisplayed()
    }

    @Test
    fun selfReportingFlow_whenDeclineSharingKeysInitially_whenAppIsLaunchedMoreThan24HoursLater_showReminderScreen() {
        runWithFeature(SELF_REPORTING, enabled = true) {
            startTestActivity<StatusActivity>()
            statusRobot.clickLinkTestResult()
            waitFor { selfReportTestTypeRobot.checkActivityIsDisplayed() }
            selfReportTestTypeRobot.clickPositiveButton()
            selfReportTestTypeRobot.clickContinueButton()

            waitFor { selfReportShareKeysInformationRobot.checkActivityIsDisplayed() }
            testAppContext.executeWithTheUserDecliningExposureKeySharing {

                selfReportShareKeysInformationRobot.clickContinueButton()

                waitFor { selfReportAppWillNotNotifyOtherUsersRobot.checkActivityIsDisplayed() }
            }
            selfReportAppWillNotNotifyOtherUsersRobot.clickContinue()

            waitFor { selfReportTestKitTypeRobot.checkActivityIsDisplayed() }
            selfReportTestKitTypeRobot.clickPCRButton()
            selfReportTestKitTypeRobot.clickContinueButton()

            waitFor { selfReportSelectTestDateRobot.checkActivityIsDisplayed() }
            selfReportSelectTestDateRobot.selectCannotRememberDate()
            selfReportSelectTestDateRobot.clickContinueButton()

            waitFor { selfReportSymptomsRobot.checkActivityIsDisplayed() }
            selfReportSymptomsRobot.clickNoButton()
            selfReportSymptomsRobot.clickContinueButton()

            waitFor { selfReportCheckAnswersRobot.checkActivityIsDisplayed() }
            selfReportCheckAnswersRobot.clickContinueButton()
            waitFor { selfReportThankYouRobot.checkActivityIsDisplayed() }
            selfReportThankYouRobot.clickContinue()
            waitFor { selfReportAdviceRobot.checkActivityIsDisplayed() }
            selfReportAdviceRobot.clickPrimaryBackToHomeButton()
            waitFor { statusRobot.checkActivityIsDisplayed() }
            assertNotNull(testAppContext.getKeySharingInfoProvider().keySharingInfo)
            testAppContext.clock.currentInstant = testAppContext.clock.instant().plus(25, ChronoUnit.HOURS)

            startTestActivity<StatusActivity>()
            testAppContext.setTemporaryExposureKeyHistoryResolutionRequired(testAppContext.app, true)
            shareKeysReminderRobot.checkActivityIsDisplayed()
            shareKeysReminderRobot.clickShareResultsButton()
            shareKeysResultRobot.checkActivityIsDisplayed()
            assertNull(testAppContext.getKeySharingInfoProvider().keySharingInfo)
            shareKeysResultRobot.clickActionButton()
            statusRobot.checkActivityIsDisplayed()
        }
    }

    @Test
    fun whenDeclineSharingKeysInitially_whenAppIsLaunchedWithin24Hours_showStatusScreen() {
        testAppContext.getUnacknowledgedTestResultsProvider().add(receivedTestResult)
        startTestActivity<StatusActivity>()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(ENGLAND) }
        testResultRobot.clickIsolationActionButton()
        assertNotNull(testAppContext.getKeySharingInfoProvider().keySharingInfo)
        testAppContext.setTemporaryExposureKeyHistoryResolutionRequired(testAppContext.app, false)
        shareKeysInformationRobot.checkActivityIsDisplayed()
        shareKeysInformationRobot.clickContinueButton()
        testAppContext.clock.currentInstant = testAppContext.clock.instant().plus(1, ChronoUnit.HOURS)

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()
    }
}
