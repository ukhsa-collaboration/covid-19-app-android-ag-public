package uk.nhs.nhsx.covid19.android.app.status

import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import com.jeroenmols.featureflag.framework.TestSetting.USE_WEB_VIEW_FOR_INTERNAL_BROWSER
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result
import uk.nhs.nhsx.covid19.android.app.exposure.setExposureNotificationResolutionRequired
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider.ContactTracingHubAction
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Token
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDate
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueConfigurationDurationDays
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BrowserRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ContactTracingHubRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.MoreAboutAppRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.QrScannerRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SettingsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatureEnabled
import java.time.LocalDate

class StatusActivityTest : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val moreAboutAppRobot = MoreAboutAppRobot()
    private val qrScannerRobot = QrScannerRobot()
    private val settingsRobot = SettingsRobot()
    private val browserRobot = BrowserRobot()
    private val contactTracingHubRobot = ContactTracingHubRobot()
    private val isolationHelper = IsolationHelper(testAppContext.clock)

    @Test
    fun clickMoreAboutApp() = notReported {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickMoreAboutApp()

        moreAboutAppRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickVenueCheckIn() = notReported {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickVenueCheckIn()

        qrScannerRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickSettings_whenBackPressed_settingsButtonShouldBeEnabled() = notReported {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickSettings()

        settingsRobot.checkActivityIsDisplayed()

        testAppContext.device.pressBack()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        statusRobot.checkSettingsIsEnabled()
    }

    @Test
    fun enableEncounterDetection_whenSuccessful_contactTracingShouldBeOn() = notReported {
        testAppContext.getExposureNotificationApi().setEnabled(false)
        testAppContext.getExposureNotificationApi().activationResult = Result.Success()

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()
        statusRobot.checkContactTracingStoppedIsDisplayed()
        statusRobot.clickActivateContactTracingButton()

        waitFor { statusRobot.checkContactTracingActiveIsDisplayed() }
    }

    @Test
    fun enableEncounterDetection_whenError_shouldShowError_contactTracingShouldBeOff() = notReported {
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
    fun enableEncounterDetection_whenResolutionNeededAndSuccessful_contactTracingShouldBeOn() = notReported {
        testAppContext.getExposureNotificationApi().setEnabled(false)
        testAppContext.setExposureNotificationResolutionRequired(testAppContext.app, true)

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()
        statusRobot.checkContactTracingStoppedIsDisplayed()
        statusRobot.clickActivateContactTracingButton()

        waitFor { statusRobot.checkContactTracingActiveIsDisplayed() }
    }

    @Test
    fun enableEncounterDetection_whenResolutionNeededAndNotSuccessful_contactTracingShouldBeOff() = notReported {
        testAppContext.getExposureNotificationApi().setEnabled(false)
        testAppContext.setExposureNotificationResolutionRequired(testAppContext.app, false)

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()
        statusRobot.checkContactTracingStoppedIsDisplayed()
        statusRobot.clickActivateContactTracingButton()

        waitFor { statusRobot.checkContactTracingStoppedIsDisplayed() }
    }

    @Test
    fun startStatusActivity_whenContactTracingHubActionIsNavigateOnly_thenContactTracingHubIsDisplayed() = notReported {
        startTestActivity<StatusActivity> {
            putExtra(StatusActivity.CONTACT_TRACING_HUB_ACTION_EXTRA, ContactTracingHubAction.ONLY_NAVIGATE)
        }

        waitFor { contactTracingHubRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun startStatusActivity_whenContactTracingHubActionIsNavigateAndTurnOn_thenNavigateToContactTracingHubAndTurnOnContactTracing() =
        notReported {
            testAppContext.getExposureNotificationApi().setEnabled(false)
            testAppContext.getExposureNotificationApi().activationResult = Result.Success()

            startTestActivity<StatusActivity> {
                putExtra(StatusActivity.CONTACT_TRACING_HUB_ACTION_EXTRA, ContactTracingHubAction.NAVIGATE_AND_TURN_ON)
            }

            waitFor { contactTracingHubRobot.checkActivityIsDisplayed() }
            waitFor { contactTracingHubRobot.checkContactTracingToggledOnIsDisplayed() }
        }

    @Test
    fun startStatusActivity_whenUserCannotClaimIsolationPayment_isolationPaymentButtonShouldNotBeDisplayed() =
        notReported {
            testAppContext.setState(isolationHelper.neverInIsolation())

            startTestActivity<StatusActivity>()

            statusRobot.checkActivityIsDisplayed()

            waitFor { statusRobot.checkIsolationPaymentButtonIsNotDisplayed() }
        }

    @Test
    fun startStatusActivity_whenUserCanClaimIsolationPaymentAndHasToken_isolationPaymentButtonShouldBeDisplayed() =
        notReported {
            testAppContext.setState(isolationHelper.contactCase().asIsolation())

            testAppContext.getIsolationPaymentTokenStateProvider().tokenState = Token("token")

            startTestActivity<StatusActivity>()

            statusRobot.checkActivityIsDisplayed()

            waitFor { statusRobot.checkIsolationPaymentButtonIsDisplayed() }
        }

    @Test
    fun startStatusActivity_whenUserCanClaimIsolationPaymentAndDoesNotHaveToken_isolationPaymentButtonShouldBeNotDisplayed_onReceivingToken_buttonShouldBeDisplayed() =
        notReported {
            testAppContext.setState(isolationHelper.contactCase().asIsolation())

            startTestActivity<StatusActivity>()

            statusRobot.checkActivityIsDisplayed()

            waitFor { statusRobot.checkIsolationPaymentButtonIsNotDisplayed() }

            UiThreadStatement.runOnUiThread {
                testAppContext.getIsolationPaymentTokenStateProvider().tokenState = Token("token")
            }

            waitFor { statusRobot.checkIsolationPaymentButtonIsDisplayed() }
        }

    @Test
    fun clickReadAdvice_whenBackPressed_readAdviceButtonShouldBeEnabled() = notReported {
        runWithFeatureEnabled(USE_WEB_VIEW_FOR_INTERNAL_BROWSER) {
            startTestActivity<StatusActivity>()

            statusRobot.clickReadAdvice()

            waitFor { browserRobot.checkActivityIsDisplayed() }

            browserRobot.clickCloseButton()

            waitFor { statusRobot.checkActivityIsDisplayed() }

            statusRobot.checkReadAdviceIsEnabled()
        }
    }

    @Test
    fun clickReportSymptoms_whenBackPressed_reportSymptomsButtonShouldBeEnabled() = notReported {
        startTestActivity<StatusActivity>()

        statusRobot.clickReportSymptoms()

        testAppContext.device.pressBack()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        statusRobot.checkReportSymptomsIsEnabled()
    }

    @Test
    fun clickOrderTest_whenBackPressed_orderTestButtonShouldBeEnabled() = notReported {
        testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

        startTestActivity<StatusActivity>()

        statusRobot.clickOrderTest()

        testAppContext.device.pressBack()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        statusRobot.checkOrderTestIsEnabled()
    }

    @Test
    fun whenLastVisitedBookTestTypeVenueAtRisk_orderTestButtonShouldBeDisplayed() = notReported {
        testAppContext.getLastVisitedBookTestTypeVenueDateProvider().lastVisitedVenue =
            LastVisitedBookTestTypeVenueDate(
                LocalDate.now(),
                RiskyVenueConfigurationDurationDays(optionToBookATest = 10)
            )

        startTestActivity<StatusActivity>()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        statusRobot.checkOrderTestIsDisplayed()
    }

    @Test
    fun whenLastVisitedBookTestTypeVenueNotAtRisk_orderTestButtonShouldBeDisplayed() = notReported {
        testAppContext.getLastVisitedBookTestTypeVenueDateProvider().lastVisitedVenue = null

        startTestActivity<StatusActivity>()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        statusRobot.checkOrderTestIsNotDisplayed()
    }

    @Test
    fun clickVenueCheckIn_whenBackPressed_venueCheckInButtonShouldBeEnabled() = notReported {
        startTestActivity<StatusActivity>()

        statusRobot.clickVenueCheckIn()

        testAppContext.device.pressBack()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        statusRobot.checkVenueCheckInIsEnabled()
    }

    @Test
    fun clickMoreAboutApp_whenBackPressed_moreAboutAppButtonShouldBeEnabled() = notReported {
        startTestActivity<StatusActivity>()

        statusRobot.clickMoreAboutApp()

        testAppContext.device.pressBack()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        statusRobot.checkMoreAboutAppIsEnabled()
    }

    @Test
    fun clickFinancialSupport_whenBackPressed_financialSupportButtonShouldBeEnabled() = notReported {
        testAppContext.setIsolationPaymentToken("abc")
        testAppContext.setState(isolationHelper.contactCase().asIsolation())

        startTestActivity<StatusActivity>()

        statusRobot.clickFinancialSupport()

        testAppContext.device.pressBack()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        statusRobot.checkFinancialSupportIsEnabled()
    }

    @Test
    fun clickLinkTestResult_whenBackPressed_linkTestResultButtonShouldBeEnabled() = notReported {
        startTestActivity<StatusActivity>()

        statusRobot.clickLinkTestResult()

        testAppContext.device.pressBack()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        statusRobot.checkLinkTestResultIsEnabled()
    }

    @Test
    fun clickRiskAreaView_whenBackPressed_riskAreaViewShouldBeEnabled() = notReported {
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
    fun startStatusActivity_whenUserIsContactCaseOnly_reportSymptomsButtonShouldBeDisplayed() = notReported {
        testAppContext.setState(isolationHelper.contactCase().asIsolation())

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        waitFor { statusRobot.checkReportSymptomsIsDisplayed() }
    }

    @Test
    fun startStatusActivity_whenUserIsNotInIsolation_reportSymptomsButtonShouldBeDisplayed() = notReported {
        testAppContext.setState(isolationHelper.neverInIsolation())

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        waitFor { statusRobot.checkReportSymptomsIsDisplayed() }
    }

    @Test
    fun startStatusActivity_whenUserIsIndexCaseOnly_reportSymptomsButtonShouldNotBeDisplayed() = notReported {
        testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        waitFor { statusRobot.checkReportSymptomsIsNotDisplayed() }
    }

    @Test
    fun startStatusActivity_whenUserIsIndexAndContactCase_reportSymptomsButtonShouldNotBeDisplayed() = notReported {
        testAppContext.setState(
            IsolationState(
                isolationConfiguration = DurationDays(),
                contactCase = isolationHelper.contactCase(),
                indexInfo = isolationHelper.selfAssessment()
            )
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        waitFor { statusRobot.checkReportSymptomsIsNotDisplayed() }
    }

    @Test
    fun clickToggleContactTracing_whenBackPressed_toggleContactTracingButtonShouldBeEnabled() = notReported {
        startTestActivity<StatusActivity>()

        statusRobot.clickToggleContactTracing()

        contactTracingHubRobot.checkActivityIsDisplayed()

        testAppContext.device.pressBack()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        statusRobot.checkToggleContactTracingIsEnabled()
    }
}
