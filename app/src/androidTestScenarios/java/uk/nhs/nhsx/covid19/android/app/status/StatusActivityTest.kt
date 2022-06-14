package uk.nhs.nhsx.covid19.android.app.status

import androidx.test.filters.FlakyTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.jeroenmols.featureflag.framework.FeatureFlag.COVID19_GUIDANCE_HOME_SCREEN_BUTTON_ENGLAND
import com.jeroenmols.featureflag.framework.FeatureFlag.COVID19_GUIDANCE_HOME_SCREEN_BUTTON_WALES
import com.jeroenmols.featureflag.framework.FeatureFlag.LOCAL_COVID_STATS
import com.jeroenmols.featureflag.framework.FeatureFlag.SELF_ISOLATION_HOME_SCREEN_BUTTON_ENGLAND
import com.jeroenmols.featureflag.framework.FeatureFlag.SELF_ISOLATION_HOME_SCREEN_BUTTON_WALES
import com.jeroenmols.featureflag.framework.FeatureFlag.TESTING_FOR_COVID19_HOME_SCREEN_BUTTON
import com.jeroenmols.featureflag.framework.FeatureFlag.VENUE_CHECK_IN_BUTTON
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result
import uk.nhs.nhsx.covid19.android.app.exposure.setExposureNotificationResolutionRequired
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider.ContactTracingHubAction.NAVIGATE_AND_TURN_ON
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider.ContactTracingHubAction.ONLY_NAVIGATE
import uk.nhs.nhsx.covid19.android.app.remote.MockLocalMessagesApi
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.LANDSCAPE
import uk.nhs.nhsx.covid19.android.app.report.config.TestConfiguration
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity.StatusActivityAction.NavigateToContactTracingHub
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity.StatusActivityAction.NavigateToLocalMessage
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.retry.RetryFlakyTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ContactTracingHubRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.IsolationHubRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LocalMessageRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.MoreAboutAppRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.QrScannerRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SettingsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestingHubRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeature
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatureEnabled
import uk.nhs.nhsx.covid19.android.app.testhelpers.setScreenOrientation
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.LocalAuthoritySetupHelper
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import java.time.LocalDate

@RunWith(Parameterized::class)
class StatusActivityTest(override val configuration: TestConfiguration) : EspressoTest(), LocalAuthoritySetupHelper {

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
    fun clickVenueCheckIn() = runWithFeatureEnabled(VENUE_CHECK_IN_BUTTON) {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickVenueCheckIn()

        qrScannerRobot.checkActivityIsDisplayed()
    }

    @Test
    fun checkPositionOfLocalDataButton_whenUserNotInIsolation() {
        runWithFeatureEnabled(LOCAL_COVID_STATS) {
            startTestActivity<StatusActivity>()

            statusRobot.checkLocalDataIsDisplayedAfterCheckInVenueButton()
        }
    }

    @Test
    fun checkLocalDataButtonIsGone_whenFeatureIsDisabledAndUserNotInIsolation() {
        runWithFeature(LOCAL_COVID_STATS, enabled = false) {
            startTestActivity<StatusActivity>()
            statusRobot.checkLocalDataIsNotDisplayed()
        }
    }

    @Test
    fun checkPositionOfLocalDataButton_whenUserInIsolation() {
        runWithFeatureEnabled(LOCAL_COVID_STATS) {
            givenLocalAuthorityIsInEngland()
            testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

            startTestActivity<StatusActivity>()

            statusRobot.checkLocalDataIsDisplayedBeforeSettingsButton()
        }
    }

    @Test
    fun checkLocalDataButtonIsGone_whenFeatureIsDisabledAndUserInIsolation() {
        givenLocalAuthorityIsInEngland()
        runWithFeature(LOCAL_COVID_STATS, enabled = false) {
            testAppContext.setState(isolationHelper.selfAssessment().asIsolation())
            startTestActivity<StatusActivity>()
            statusRobot.checkLocalDataIsNotDisplayed()
        }
    }

    @Test
    fun checkPositionOfLocalDataButton_whenUserInContactIsolation() {
        givenLocalAuthorityIsInEngland()
        runWithFeatureEnabled(LOCAL_COVID_STATS) {
            testAppContext.setState(isolationHelper.contact().asIsolation())

            startTestActivity<StatusActivity>()

            statusRobot.checkLocalDataIsDisplayedBeforeSettingsButton()
        }
    }

    @Test
    fun checkLocalDataButtonIsGone_whenFeatureIsDisabledAndUserInContactIsolation() {
        givenLocalAuthorityIsInEngland()
        runWithFeature(LOCAL_COVID_STATS, enabled = false) {
            testAppContext.setState(isolationHelper.contact().asIsolation())
            startTestActivity<StatusActivity>()
            statusRobot.checkLocalDataIsNotDisplayed()
        }
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

    @Test
    fun whenIsolating_withAnimationDisabled_shouldShowStaticImage() {
        givenLocalAuthorityIsInEngland()
        testAppContext.setState(isolationHelper.contact().asIsolation())

        startTestActivity<StatusActivity>()

        statusRobot.checkNoAnimationIsDisplayed(isIsolating = true)
    }

    @Test
    fun whenNotIsolating_withAnimationDisabled_shouldShowStaticImage() {
        testAppContext.setState(isolationHelper.neverInIsolation())

        startTestActivity<StatusActivity>()

        statusRobot.checkNoAnimationIsDisplayed(isIsolating = false)
    }

    @Test
    fun whenIsolating_withAnimationDisabled_contactTracingOff_shouldNotShowStaticImage() {
        givenLocalAuthorityIsInEngland()
        testAppContext.setState(isolationHelper.contact().asIsolation())
        testAppContext.getExposureNotificationApi().setEnabled(false)

        startTestActivity<StatusActivity>()

        statusRobot.checkStaticImageIsNotDisplayed(isIsolating = true)
    }

    @Test
    fun whenNotIsolating_withAnimationDisabled_contactTracingOff_shouldNotShowStaticImage() {
        testAppContext.setState(isolationHelper.neverInIsolation())
        testAppContext.getExposureNotificationApi().setEnabled(false)

        startTestActivity<StatusActivity>()

        statusRobot.checkStaticImageIsNotDisplayed(isIsolating = false)
    }

    @Test
    fun whenBluetoothEnabled_doNotShowBluetoothStoppedView() {
        testAppContext.setBluetoothEnabled(true)

        startTestActivity<StatusActivity>()

        statusRobot.checkBluetoothStoppedViewIsNotDisplayed()
    }

    @RetryFlakyTest
    @Test
    fun onBluetoothDisabledThenEnabled_viewScreenChanges() {
        testAppContext.setBluetoothEnabled(false)
        testAppContext.getShouldShowBluetoothSplashScreen().setHasBeenShown(true)

        startTestActivity<StatusActivity>()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        waitFor { statusRobot.checkBluetoothStoppedViewIsDisplayed() }

        testAppContext.setBluetoothEnabled(true)

        waitFor { statusRobot.checkContactTracingActiveIsDisplayed() }
        waitFor { statusRobot.checkBluetoothStoppedViewIsNotDisplayed() }
    }

    @RetryFlakyTest
    @Test
    fun onBluetoothEnabledThenDisabled_viewScreenChanges() {
        testAppContext.setBluetoothEnabled(true)
        testAppContext.getShouldShowBluetoothSplashScreen().setHasBeenShown(true)

        startTestActivity<StatusActivity>()

        waitFor { statusRobot.checkBluetoothStoppedViewIsNotDisplayed() }
        waitFor { statusRobot.checkContactTracingActiveIsDisplayed() }

        testAppContext.setBluetoothEnabled(false)

        startTestActivity<StatusActivity>()

        waitFor { statusRobot.checkBluetoothStoppedViewIsDisplayed() }
    }

    @Test
    fun whenBluetoothDisabled_notIsolating_showBluetoothStoppedView() {
        testAppContext.setBluetoothEnabled(false)
        testAppContext.setState(isolationHelper.neverInIsolation())
        testAppContext.getShouldShowBluetoothSplashScreen().setHasBeenShown(true)
        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()
        statusRobot.checkBluetoothStoppedViewIsDisplayed()
    }

    @Test
    fun whenBluetoothDisabled_inIsolation_doNotShowBluetoothStoppedView() {
        givenLocalAuthorityIsInEngland()
        testAppContext.setBluetoothEnabled(false)
        testAppContext.setState(isolationHelper.contact().asIsolation())
        testAppContext.getShouldShowBluetoothSplashScreen().setHasBeenShown(true)
        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()
        statusRobot.checkBluetoothStoppedViewIsNotDisplayed()
    }

    @Test
    fun whenBluetoothDisabled_contactTracingIsOff_showBluetoothStoppedView() {
        testAppContext.setBluetoothEnabled(false)
        testAppContext.getExposureNotificationApi().setEnabled(false)
        testAppContext.getShouldShowBluetoothSplashScreen().setHasBeenShown(true)
        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()
        statusRobot.checkBluetoothStoppedViewIsDisplayed()
    }

    @Test
    fun whenBluetoothDisabled_contactTracingIsOn_showBluetoothStoppedView() {
        testAppContext.setBluetoothEnabled(false)
        testAppContext.getExposureNotificationApi().setEnabled(true)
        testAppContext.getShouldShowBluetoothSplashScreen().setHasBeenShown(true)
        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()
        statusRobot.checkBluetoothStoppedViewIsDisplayed()
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
    fun clickVenueCheckIn_whenBackPressed_venueCheckInButtonShouldBeEnabled() =
        runWithFeatureEnabled(VENUE_CHECK_IN_BUTTON) {
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
        givenLocalAuthorityIsInEngland()
        testAppContext.setState(isolationHelper.contact().asIsolation())

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        waitFor { statusRobot.checkReportSymptomsIsDisplayed() }
    }

    @Test
    fun whenUserIsIndexCaseTriggeredBySelfAssessment_reportSymptomsButtonShouldNotBeDisplayed() {
        givenLocalAuthorityIsInEngland()
        testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        waitFor { statusRobot.checkReportSymptomsIsNotDisplayed() }
    }

    @Test
    fun whenUserIsIndexCaseTriggeredByPositiveTestResultWithUnknownOnsetDate_reportSymptomsButtonShouldBeDisplayed() {
        givenLocalAuthorityIsInEngland()
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

        statusRobot.clickManageContactTracing()

        contactTracingHubRobot.checkActivityIsDisplayed()

        testAppContext.device.pressBack()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        statusRobot.checkToggleContactTracingIsEnabled()
    }

    @Test
    fun clickTestingHub_whenBackPressed_TestingHubButtonShouldBeEnabled() =
        runWithFeatureEnabled(TESTING_FOR_COVID19_HOME_SCREEN_BUTTON) {
            startTestActivity<StatusActivity>()

            statusRobot.clickTestingHub()

            testingHubRobot.checkActivityIsDisplayed()

            testAppContext.device.pressBack()

            waitFor { statusRobot.checkActivityIsDisplayed() }

            statusRobot.checkTestingHubIsEnabled()
        }

    @Test
    fun whenIsolating_selfIsolationHubFeatureFlagIsEnabled_isolationHubButtonShouldBeDisplayed_forEngland() =
        runWithFeatureEnabled(SELF_ISOLATION_HOME_SCREEN_BUTTON_ENGLAND) {
            givenLocalAuthorityIsInEngland()
            testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

            startTestActivity<StatusActivity>()

            statusRobot.checkActivityIsDisplayed()

            statusRobot.checkIsolationHubIsDisplayed()
        }

    @Test
    fun whenIsolating_selfIsolationHubFeatureFlagIsEnabled_isolationHubButtonShouldBeDisplayed_forWales() =
        runWithFeatureEnabled(SELF_ISOLATION_HOME_SCREEN_BUTTON_WALES) {
            givenLocalAuthorityIsInWales()
            testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

            startTestActivity<StatusActivity>()

            statusRobot.checkActivityIsDisplayed()

            statusRobot.checkIsolationHubIsDisplayed()
        }

    @Test
    fun whenIsolating_selfIsolationHubFeatureFlagIsDisabled_ButtonShouldNotBeDisplayed_forEngland() =
        runWithFeature(SELF_ISOLATION_HOME_SCREEN_BUTTON_ENGLAND, enabled = false) {
            givenLocalAuthorityIsInEngland()
            testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

            startTestActivity<StatusActivity>()

            statusRobot.checkActivityIsDisplayed()

            statusRobot.checkIsolationHubIsNotDisplayed()
        }

    @Test
    fun whenIsolating_selfIsolationHubFeatureFlagIsDisabled_ButtonShouldNotBeDisplayed_forWales() =
        runWithFeature(SELF_ISOLATION_HOME_SCREEN_BUTTON_WALES, enabled = false) {
            givenLocalAuthorityIsInWales()
            testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

            startTestActivity<StatusActivity>()

            statusRobot.checkActivityIsDisplayed()

            statusRobot.checkIsolationHubIsNotDisplayed()
        }

    @Test
    fun whenNotIsolating_isolationHubButtonShouldNotBeDisplayed() {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.checkIsolationHubIsNotDisplayed()
    }

    @Test
    fun clickIsolationHub_whenBackPressed_isolationHubButtonShouldBeEnabled_forEngland() =
        runWithFeatureEnabled(SELF_ISOLATION_HOME_SCREEN_BUTTON_ENGLAND) {
            givenLocalAuthorityIsInEngland()
            testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

            startTestActivity<StatusActivity>()

            statusRobot.clickIsolationHub()

            isolationHubRobot.checkActivityIsDisplayed()

            testAppContext.device.pressBack()

            waitFor { statusRobot.checkActivityIsDisplayed() }

            statusRobot.checkIsolationHubIsEnabled()
        }

    @Test
    fun clickIsolationHub_whenBackPressed_isolationHubButtonShouldBeEnabled_forWales() =
        runWithFeatureEnabled(SELF_ISOLATION_HOME_SCREEN_BUTTON_WALES) {
            givenLocalAuthorityIsInWales()
            testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

            startTestActivity<StatusActivity>()

            statusRobot.clickIsolationHub()

            isolationHubRobot.checkActivityIsDisplayed()

            testAppContext.device.pressBack()

            waitFor { statusRobot.checkActivityIsDisplayed() }

            statusRobot.checkIsolationHubIsEnabled()
        }

    @Test
    fun whenCheckInFeatureFlagIsDisabled_optionVenueCheckInIsNotDisplayed() =
        runWithFeature(VENUE_CHECK_IN_BUTTON, enabled = false) {
            testAppContext.setState(isolationHelper.neverInIsolation())

            startTestActivity<StatusActivity>()

            statusRobot.checkVenueCheckIsNotDisplayed()
        }

    @Test
    fun whenTestingHubFeatureFlagIsEnabled_optionTestingHubIsDisplayed() =
        runWithFeatureEnabled(TESTING_FOR_COVID19_HOME_SCREEN_BUTTON) {

            startTestActivity<StatusActivity>()

            statusRobot.checkTestingHubIsDisplayed()
        }

    @Test
    fun whenTestingHubFeatureFlagIsDisabled_optionTestingHubIsNotDisplayed() =
        runWithFeature(TESTING_FOR_COVID19_HOME_SCREEN_BUTTON, enabled = false) {

            startTestActivity<StatusActivity>()

            statusRobot.checkTestingHubIsNotDisplayed()
        }

    @Test
    fun whenCovidGuidanceHubFeatureFlagIsEnabledForEngland_optionTestingHubIsDisplayed() =
        runWithFeatureEnabled(COVID19_GUIDANCE_HOME_SCREEN_BUTTON_ENGLAND) {
            givenLocalAuthorityIsInEngland()

            startTestActivity<StatusActivity>()

            statusRobot.checkCovidGuidanceHubIsDisplayed()
        }

    @Test
    fun whenCovidGuidanceHubFeatureFlagIsDisabledForEngland_optionTestingHubIsNotDisplayed() =
        runWithFeature(COVID19_GUIDANCE_HOME_SCREEN_BUTTON_ENGLAND, enabled = false) {
            givenLocalAuthorityIsInEngland()

            startTestActivity<StatusActivity>()

            statusRobot.checkCovidGuidanceHubIsNotDisplayed()
        }

    @Test
    fun whenCovidGuidanceHubFeatureFlagIsEnabledForWales_optionTestingHubIsDisplayed() =
        runWithFeatureEnabled(COVID19_GUIDANCE_HOME_SCREEN_BUTTON_WALES) {
            givenLocalAuthorityIsInWales()

            startTestActivity<StatusActivity>()

            statusRobot.checkCovidGuidanceHubIsDisplayed()
        }

    @Test
    fun whenCovidGuidanceHubFeatureFlagIsDisabledForWales_optionTestingHubIsNotDisplayed() =
        runWithFeature(COVID19_GUIDANCE_HOME_SCREEN_BUTTON_WALES, enabled = false) {
            givenLocalAuthorityIsInWales()

            startTestActivity<StatusActivity>()

            statusRobot.checkCovidGuidanceHubIsNotDisplayed()
        }

    @Test
    fun newLabelOnReportSymptomsButtonForEngland_shouldBeDisabled() {
        givenLocalAuthorityIsInEngland()
        startTestActivity<StatusActivity>()

        waitFor { statusRobot.checkNewLabelIsDisplayed(false) }
    }
}
