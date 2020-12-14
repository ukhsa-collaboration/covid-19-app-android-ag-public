package uk.nhs.nhsx.covid19.android.app.status

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result
import uk.nhs.nhsx.covid19.android.app.exposure.setExposureNotificationResolutionRequired
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Token
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.LANDSCAPE
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.PORTRAIT
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationReminderRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.MoreAboutAppRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.QrScannerRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setScreenOrientation
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS

class StatusActivityTest : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val moreAboutAppRobot = MoreAboutAppRobot()
    private val qrScannerRobot = QrScannerRobot()
    private val exposureNotificationReminderRobot = ExposureNotificationReminderRobot()

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
    fun clickEncounterDetectionSwitchAndDontRemind() = notReported {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickEncounterDetectionSwitch()

        exposureNotificationReminderRobot.checkDialogIsDisplayed()

        setScreenOrientation(LANDSCAPE)

        waitFor { exposureNotificationReminderRobot.checkDialogIsDisplayed() }

        setScreenOrientation(PORTRAIT)

        waitFor { exposureNotificationReminderRobot.checkDialogIsDisplayed() }

        waitFor { exposureNotificationReminderRobot.clickDontRemindMe() }

        waitFor { statusRobot.checkEncounterDetectionSwitchIsNotChecked() }

        statusRobot.clickEncounterDetectionSwitch()

        statusRobot.checkEncounterDetectionSwitchIsChecked()
    }

    @Test
    fun clickEncounterDetectionSwitchAndRemindIn4Hours() = notReported {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickEncounterDetectionSwitch()

        exposureNotificationReminderRobot.checkDialogIsDisplayed()

        exposureNotificationReminderRobot.clickRemindMeIn4Hours()

        exposureNotificationReminderRobot.checkConfirmationDialogIsDisplayed()

        exposureNotificationReminderRobot.clickConfirmationDialogOk()

        statusRobot.checkEncounterDetectionSwitchIsNotChecked()
    }

    @Test
    fun enableEncounterDetection_whenSuccessful_encounterDetectionSwitchShouldBeChecked() = notReported {
        testAppContext.getExposureNotificationApi().setEnabled(false)
        testAppContext.getExposureNotificationApi().activationResult = Result.Success()

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickEncounterDetectionSwitch()

        waitFor { statusRobot.checkEncounterDetectionSwitchIsChecked() }
    }

    @Test
    fun enableEncounterDetection_whenError_shouldShowError_encounterDetectionSwitchShouldNotBeChecked() = notReported {
        testAppContext.getExposureNotificationApi().setEnabled(false)
        testAppContext.getExposureNotificationApi().activationResult = Result.Error()

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickEncounterDetectionSwitch()

        waitFor { statusRobot.checkErrorIsDisplayed() }

        waitFor { statusRobot.checkEncounterDetectionSwitchIsNotChecked() }
    }

    @Test
    fun enableEncounterDetection_whenResolutionNeededAndSuccessful_encounterDetectionSwitchShouldBeChecked() = notReported {
        testAppContext.getExposureNotificationApi().setEnabled(false)
        testAppContext.setExposureNotificationResolutionRequired(testAppContext.app, true)

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickEncounterDetectionSwitch()

        waitFor { statusRobot.checkEncounterDetectionSwitchIsChecked() }
    }

    @Test
    fun enableEncounterDetection_whenResolutionNeededAndNotSuccessful_encounterDetectionSwitchShouldNotBeChecked() = notReported {
        testAppContext.getExposureNotificationApi().setEnabled(false)
        testAppContext.setExposureNotificationResolutionRequired(testAppContext.app, false)

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickEncounterDetectionSwitch()

        waitFor { statusRobot.checkEncounterDetectionSwitchIsNotChecked() }
    }

    @Test
    fun startStatusActivity_whenNotificationFlagNull_encounterDetectionShouldNotBeActivated() = notReported {
        testAppContext.getExposureNotificationApi().setEnabled(false)
        testAppContext.getExposureNotificationApi().activationResult = Result.Success()

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        waitFor { statusRobot.checkEncounterDetectionSwitchIsNotChecked() }
    }

    @Test
    fun startStatusActivity_whenNotificationFlagEmpty_encounterDetectionShouldNotBeActivated() = notReported {
        testAppContext.getExposureNotificationApi().setEnabled(false)
        testAppContext.getExposureNotificationApi().activationResult = Result.Success()

        startTestActivity<StatusActivity> {
            putExtra(NotificationProvider.TAP_EXPOSURE_NOTIFICATION_REMINDER_FLAG, "")
        }

        statusRobot.checkActivityIsDisplayed()

        waitFor { statusRobot.checkEncounterDetectionSwitchIsNotChecked() }
    }

    @Test
    fun startStatusActivity_whenNotificationFlagNotEmpty_encounterDetectionShouldBeActivated() = notReported {
        testAppContext.getExposureNotificationApi().setEnabled(false)
        testAppContext.getExposureNotificationApi().activationResult = Result.Success()

        startTestActivity<StatusActivity> {
            putExtra(NotificationProvider.TAP_EXPOSURE_NOTIFICATION_REMINDER_FLAG, "x")
        }

        statusRobot.checkActivityIsDisplayed()

        waitFor { statusRobot.checkEncounterDetectionSwitchIsChecked() }
    }

    @Test
    fun startStatusActivity_whenUserCannotClaimIsolationPayment_isolationPaymentButtonShouldNotBeDisplayed() = notReported {
        testAppContext.setState(Default())

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        waitFor { statusRobot.checkIsolationPaymentButtonIsNotDisplayed() }
    }

    @Test
    fun startStatusActivity_whenUserCanClaimIsolationPaymentAndHasToken_isolationPaymentButtonShouldBeDisplayed() = notReported {
        testAppContext.setState(
            Isolation(
                isolationStart = Instant.now().minus(20, DAYS),
                isolationConfiguration = DurationDays(),
                contactCase = ContactCase(
                    startDate = Instant.now().minus(10, DAYS),
                    notificationDate = Instant.now().minus(2, DAYS),
                    expiryDate = LocalDate.now().plusDays(30)
                )
            )
        )
        testAppContext.getIsolationPaymentTokenStateProvider().tokenState = Token("token")

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        waitFor { statusRobot.checkIsolationPaymentButtonIsDisplayed() }
    }

    @Test
    fun startStatusActivity_whenUserCanClaimIsolationPaymentAndDoesNotHaveToken_isolationPaymentButtonShouldBeNotDisplayed_onReceivingToken_buttonShouldBeDisplayed() = notReported {
        testAppContext.setState(
            Isolation(
                isolationStart = Instant.now().minus(20, DAYS),
                isolationConfiguration = DurationDays(),
                contactCase = ContactCase(
                    startDate = Instant.now().minus(10, DAYS),
                    notificationDate = Instant.now().minus(2, DAYS),
                    expiryDate = LocalDate.now().plusDays(30)
                )
            )
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        waitFor { statusRobot.checkIsolationPaymentButtonIsNotDisplayed() }

        testAppContext.getIsolationPaymentTokenStateProvider().tokenState = Token("token")

        waitFor { statusRobot.checkIsolationPaymentButtonIsDisplayed() }
    }
}
