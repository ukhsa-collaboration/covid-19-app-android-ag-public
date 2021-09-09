package uk.nhs.nhsx.covid19.android.app.status

import androidx.test.filters.FlakyTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result
import uk.nhs.nhsx.covid19.android.app.exposure.setExposureNotificationResolutionRequired
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider.ContactTracingHubAction.NAVIGATE_AND_TURN_ON
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider.ContactTracingHubAction.ONLY_NAVIGATE
import uk.nhs.nhsx.covid19.android.app.remote.MockLocalMessagesApi
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.LANDSCAPE
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity.StatusActivityAction.NavigateToContactTracingHub
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity.StatusActivityAction.NavigateToLocalMessage
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ContactTracingHubRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.IsolationHubRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LocalMessageRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.MoreAboutAppRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.QrScannerRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SettingsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestingHubRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setScreenOrientation
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import java.time.LocalDate

class StatusActivityTest : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val moreAboutAppRobot = MoreAboutAppRobot()
    private val qrScannerRobot = QrScannerRobot()
    private val settingsRobot = SettingsRobot()
    private val contactTracingHubRobot = ContactTracingHubRobot()
    private val isolationHubRobot = IsolationHubRobot()
    private val testingHubRobot = TestingHubRobot()
    private val localMessageRobot = LocalMessageRobot()
    private val isolationHelper = IsolationHelper(testAppContext.clock)

    @Test
    fun clickMoreAboutApp() {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickMoreAboutApp()

        moreAboutAppRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickVenueCheckIn() {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickVenueCheckIn()

        qrScannerRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickSettings_whenBackPressed_settingsButtonShouldBeEnabled() {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickSettings()

        settingsRobot.checkActivityIsDisplayed()

        testAppContext.device.pressBack()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        statusRobot.checkSettingsIsEnabled()
    }

    @Test
    fun enableEncounterDetection_whenSuccessful_contactTracingShouldBeOn() {
        testAppContext.getExposureNotificationApi().setEnabled(false)
        testAppContext.getExposureNotificationApi().activationResult = Result.Success()

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()
        statusRobot.checkContactTracingStoppedIsDisplayed()
        statusRobot.clickActivateContactTracingButton()

        waitFor { statusRobot.checkContactTracingActiveIsDisplayed() }
    }

    @Test
    fun enableEncounterDetection_whenError_shouldShowError_contactTracingShouldBeOff() {
        testAppContext.getExposureNotificationApi().setEnabled(false)
        testAppContext.getExposureNotificationApi().activationResult = Result.Error()

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()
        statusRobot.checkContactTracingStoppedIsDisplayed()
        statusRobot.clickActivateContactTracingButton()

        waitFor { statusRobot.checkErrorIsDisplayed() }

        waitFor { statusRobot.checkContactTracingStoppedIsDisplayed() }
    }

    @Test
    fun enableEncounterDetection_whenResolutionNeededAndSuccessful_contactTracingShouldBeOn() {
        testAppContext.getExposureNotificationApi().setEnabled(false)
        testAppContext.setExposureNotificationResolutionRequired(testAppContext.app, true)

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()
        statusRobot.checkContactTracingStoppedIsDisplayed()
        statusRobot.clickActivateContactTracingButton()

        waitFor { statusRobot.checkContactTracingActiveIsDisplayed() }
    }

    @Test
    fun enableEncounterDetection_whenResolutionNeededAndNotSuccessful_contactTracingShouldBeOff() {
        testAppContext.getExposureNotificationApi().setEnabled(false)
        testAppContext.setExposureNotificationResolutionRequired(testAppContext.app, false)

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()
        statusRobot.checkContactTracingStoppedIsDisplayed()
        statusRobot.clickActivateContactTracingButton()

        waitFor { statusRobot.checkContactTracingStoppedIsDisplayed() }
    }

    @Test
    fun startStatusActivity_whenContactTracingHubActionIsNavigateOnly_thenContactTracingHubIsDisplayed() {
        startTestActivity<StatusActivity> {
            putExtra(
                StatusActivity.STATUS_ACTIVITY_ACTION,
                NavigateToContactTracingHub(action = ONLY_NAVIGATE)
            )
        }

        waitFor { contactTracingHubRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun startStatusActivity_whenContactTracingHubActionIsNavigateAndTurnOn_thenNavigateToContactTracingHubAndTurnOnContactTracing() {
        testAppContext.getExposureNotificationApi().setEnabled(false)
        testAppContext.getExposureNotificationApi().activationResult = Result.Success()

        startTestActivity<StatusActivity> {
            putExtra(
                StatusActivity.STATUS_ACTIVITY_ACTION,
                NavigateToContactTracingHub(action = NAVIGATE_AND_TURN_ON)
            )
        }

        waitFor { contactTracingHubRobot.checkActivityIsDisplayed() }
        waitFor { contactTracingHubRobot.checkContactTracingToggledOnIsDisplayed() }
    }

    @Test
    @FlakyTest
    fun startStatusActivity_whenNavigateToContactTracingHubViaBundle_thenPressBackAndRotateDevice_shouldShowStatusActivity() {
        testAppContext.getExposureNotificationApi().setEnabled(false)
        testAppContext.getExposureNotificationApi().activationResult = Result.Success()

        startTestActivity<StatusActivity> {
            putExtra(
                StatusActivity.STATUS_ACTIVITY_ACTION,
                NavigateToContactTracingHub(action = NAVIGATE_AND_TURN_ON)
            )
        }

        waitFor { contactTracingHubRobot.checkActivityIsDisplayed() }
        waitFor { contactTracingHubRobot.checkContactTracingToggledOnIsDisplayed() }

        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        setScreenOrientation(LANDSCAPE)
        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    @FlakyTest
    fun startStatusActivity_whenNavigateToLocalMessageScreenViaBundle_thenPressBackAndRotateDevice_shouldShowStatusActivity() {
        testAppContext.setLocalAuthority("E07000240")
        testAppContext.setPostCode("AL1")
        testAppContext.getLocalMessagesProvider().localMessages = MockLocalMessagesApi.successResponse

        startTestActivity<StatusActivity> {
            putExtra(StatusActivity.STATUS_ACTIVITY_ACTION, NavigateToLocalMessage)
        }

        waitFor { localMessageRobot.checkActivityIsDisplayed() }

        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        setScreenOrientation(LANDSCAPE)
        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun startStatusActivity_whenDoesNotHaveLocalMessage_bannerIsNotDisplayed() {
        testAppContext.setLocalAuthority("E07000240")
        testAppContext.setPostCode("AL1")
        testAppContext.getLocalMessagesProvider().localMessages = MockLocalMessagesApi.emptyResponse

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        waitFor { statusRobot.checkLocalMessageBannerIsNotDisplayed() }
    }

    @Test
    fun startStatusActivity_whenHasLocalMessage_bannerIsDisplayed() {
        testAppContext.setLocalAuthority("E07000240")
        testAppContext.setPostCode("AL1")
        testAppContext.getLocalMessagesProvider().localMessages = MockLocalMessagesApi.successResponse

        startTestActivity<StatusActivity>()

        runBackgroundTasks()

        statusRobot.checkActivityIsDisplayed()

        waitFor { statusRobot.checkLocalMessageBannerIsDisplayed() }
    }

    fun whenIsolating_withAnimationDisabled_shouldShowStaticImage() {
        testAppContext.setState(isolationHelper.contact().asIsolation())

        startTestActivity<StatusActivity>()

        statusRobot.checkNoAnimationIsDisplayed(isIsolating = true)
    }

    fun whenNotIsolating_withAnimationDisabled_shouldShowStaticImage() {
        testAppContext.setState(isolationHelper.neverInIsolation())

        startTestActivity<StatusActivity>()

        statusRobot.checkNoAnimationIsDisplayed(isIsolating = false)
    }

    fun whenIsolating_withAnimationDisabled_contactTracingOff_shouldNotShowStaticImage() {
        testAppContext.setState(isolationHelper.contact().asIsolation())
        testAppContext.getExposureNotificationApi().setEnabled(false)

        startTestActivity<StatusActivity>()

        statusRobot.checkStaticImageIsNotDisplayed(isIsolating = true)
    }

    fun whenNotIsolating_withAnimationDisabled_contactTracingOff_shouldNotShowStaticImage() {
        testAppContext.setState(isolationHelper.neverInIsolation())
        testAppContext.getExposureNotificationApi().setEnabled(false)

        startTestActivity<StatusActivity>()

        statusRobot.checkStaticImageIsNotDisplayed(isIsolating = false)
    }

    @Test
    fun clickReportSymptoms_whenBackPressed_reportSymptomsButtonShouldBeEnabled() {
        startTestActivity<StatusActivity>()

        statusRobot.clickReportSymptoms()

        testAppContext.device.pressBack()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        statusRobot.checkReportSymptomsIsEnabled()
    }

    @Test
    fun clickVenueCheckIn_whenBackPressed_venueCheckInButtonShouldBeEnabled() {
        startTestActivity<StatusActivity>()

        statusRobot.clickVenueCheckIn()

        testAppContext.device.pressBack()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        statusRobot.checkVenueCheckInIsEnabled()
    }

    @Test
    fun clickMoreAboutApp_whenBackPressed_moreAboutAppButtonShouldBeEnabled() {
        startTestActivity<StatusActivity>()

        statusRobot.clickMoreAboutApp()

        testAppContext.device.pressBack()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        statusRobot.checkMoreAboutAppIsEnabled()
    }

    @Test
    fun clickLinkTestResult_whenBackPressed_linkTestResultButtonShouldBeEnabled() {
        startTestActivity<StatusActivity>()

        statusRobot.clickLinkTestResult()

        testAppContext.device.pressBack()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        statusRobot.checkLinkTestResultIsEnabled()
    }

    @Test
    fun clickRiskAreaView_whenBackPressed_riskAreaViewShouldBeEnabled() {
        testAppContext.setPostCode("CM2")

        startTestActivity<StatusActivity>()

        // This is necessary because ExposureApplication does not invoke the download tasks when onboarding is not completed
        runBackgroundTasks()

        waitFor { statusRobot.checkAreaRiskViewIsDisplayed() }

        statusRobot.clickAreaRiskView()

        testAppContext.device.pressBack()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        statusRobot.checkAreaRiskViewIsEnabled()
    }

    @Test
    fun whenUserIsNotIsolating_reportSymptomsButtonShouldBeDisplayed() {
        testAppContext.setState(isolationHelper.neverInIsolation())

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        waitFor { statusRobot.checkReportSymptomsIsDisplayed() }
    }

    @Test
    fun whenUserIsContactCase_reportSymptomsButtonShouldBeDisplayed() {
        testAppContext.setState(isolationHelper.contact().asIsolation())

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        waitFor { statusRobot.checkReportSymptomsIsDisplayed() }
    }

    @Test
    fun whenUserIsIndexCaseTriggeredBySelfAssessment_reportSymptomsButtonShouldNotBeDisplayed() {
        testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        waitFor { statusRobot.checkReportSymptomsIsNotDisplayed() }
    }

    @Test
    fun whenUserIsIndexCaseTriggeredByPositiveTestResultWithUnknownOnsetDate_reportSymptomsButtonShouldBeDisplayed() {
        testAppContext.setState(
            AcknowledgedTestResult(
                testEndDate = LocalDate.now(testAppContext.clock),
                testResult = POSITIVE,
                testKitType = LAB_RESULT,
                acknowledgedDate = LocalDate.now(testAppContext.clock),
                requiresConfirmatoryTest = false
            ).asIsolation()
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        waitFor { statusRobot.checkReportSymptomsIsDisplayed() }
    }

    @Test
    fun clickToggleContactTracing_whenBackPressed_toggleContactTracingButtonShouldBeEnabled() {
        startTestActivity<StatusActivity>()

        statusRobot.clickToggleContactTracing()

        contactTracingHubRobot.checkActivityIsDisplayed()

        testAppContext.device.pressBack()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        statusRobot.checkToggleContactTracingIsEnabled()
    }

    @Test
    fun clickTestingHub_whenBackPressed_TestingHubButtonShouldBeEnabled() {
        startTestActivity<StatusActivity>()

        statusRobot.clickTestingHub()

        testingHubRobot.checkActivityIsDisplayed()

        testAppContext.device.pressBack()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        statusRobot.checkTestingHubIsEnabled()
    }

    @Test
    fun whenIsolating_isolationHubButtonShouldBeDisplayed() {
        testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.checkIsolationHubIsDisplayed()
    }

    @Test
    fun whenNotIsolating_isolationHubButtonShouldNotBeDisplayed() {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.checkIsolationHubIsNotDisplayed()
    }

    @Test
    fun clickIsolationHub_whenBackPressed_isolationHubButtonShouldBeEnabled() {
        testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

        startTestActivity<StatusActivity>()

        statusRobot.clickIsolationHub()

        isolationHubRobot.checkActivityIsDisplayed()

        testAppContext.device.pressBack()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        statusRobot.checkIsolationHubIsEnabled()
    }
}
