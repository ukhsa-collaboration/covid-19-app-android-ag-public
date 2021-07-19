package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.setTemporaryExposureKeyHistoryResolutionRequired
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysReminderRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.SymptomsDate
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ShareKeysFlowTest : EspressoTest() {

    private val testResultRobot = TestResultRobot(testAppContext.app)
    private val shareKeysInformationRobot = ShareKeysInformationRobot()
    private val shareKeysReminderRobot = ShareKeysReminderRobot()
    private val shareKeysResultRobot = ShareKeysResultRobot()
    private val statusRobot = StatusRobot()

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
        testAppContext.getUnacknowledgedTestResultsProvider().add(receivedTestResult)
    }

    @Test
    fun whenSuccessfullySharingKeys_ShareKeysInfoIsRemoved() {
        startTestActivity<StatusActivity>()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }
        testResultRobot.clickIsolationActionButton()
        assertNotNull(testAppContext.getKeySharingInfoProvider().keySharingInfo)
        shareKeysInformationRobot.checkActivityIsDisplayed()
        shareKeysInformationRobot.clickContinueButton()
        assertNull(testAppContext.getKeySharingInfoProvider().keySharingInfo)
    }

    @Test
    fun whenAppIsLaunched_andInitialFlowWasNotCompleted_shouldShowInitialKeySharingFlow() {
        startTestActivity<StatusActivity>()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }
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
    fun whenAppIsLaunched_andInitialFlowIsDeniedMoreThan24HoursLater_shouldRemoveShareKeysInfo() {
        startTestActivity<StatusActivity>()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }
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
        startTestActivity<StatusActivity>()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }
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
    fun whenDeclineSharingKeysInitially_whenAppIsLaunchedWithin24Hours_showStatusScreen() {
        startTestActivity<StatusActivity>()
        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }
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
