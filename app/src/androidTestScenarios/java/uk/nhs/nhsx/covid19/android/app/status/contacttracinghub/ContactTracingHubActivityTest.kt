package uk.nhs.nhsx.covid19.android.app.status.contacttracinghub

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result
import uk.nhs.nhsx.covid19.android.app.exposure.setExposureNotificationResolutionRequired
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.LANDSCAPE
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.PORTRAIT
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.status.contacttracinghub.ContactTracingHubActivity.Companion.SHOULD_TURN_ON_CONTACT_TRACING
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ContactTracingHubRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationReminderRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setScreenOrientation

class ContactTracingHubActivityTest : EspressoTest() {

    private val contactTracingHubRobot = ContactTracingHubRobot()
    private val exposureNotificationReminderRobot = ExposureNotificationReminderRobot()

    @Test
    fun enableEncounterDetection_whenSuccessful_contactTracingShouldBeOn() = notReported {
        testAppContext.getExposureNotificationApi().setEnabled(false)
        testAppContext.getExposureNotificationApi().activationResult = Result.Success()

        startActivityAndVerifyContactTracingToggledOff()

        waitFor { contactTracingHubRobot.checkContactTracingToggledOnIsDisplayed() }
    }

    @Test
    fun enableEncounterDetection_whenError_shouldShowError_contactTracingShouldBeOff() = notReported {
        testAppContext.getExposureNotificationApi().setEnabled(false)
        testAppContext.getExposureNotificationApi().activationResult = Result.Error()

        startActivityAndVerifyContactTracingToggledOff()

        waitFor { contactTracingHubRobot.checkErrorIsDisplayed() }

        waitFor { contactTracingHubRobot.checkContactTracingToggledOffIsDisplayed() }
    }

    @Test
    fun enableEncounterDetection_whenResolutionNeededAndSuccessful_contactTracingShouldBeOn() = notReported {
        testAppContext.getExposureNotificationApi().setEnabled(false)
        testAppContext.setExposureNotificationResolutionRequired(testAppContext.app, true)

        startActivityAndVerifyContactTracingToggledOff()

        waitFor { contactTracingHubRobot.checkContactTracingToggledOnIsDisplayed() }
    }

    @Test
    fun enableEncounterDetection_whenResolutionNeededAndNotSuccessful_contactTracingShouldBeOff() = notReported {
        testAppContext.getExposureNotificationApi().setEnabled(false)
        testAppContext.setExposureNotificationResolutionRequired(testAppContext.app, false)

        startActivityAndVerifyContactTracingToggledOff()

        waitFor { contactTracingHubRobot.checkContactTracingToggledOffIsDisplayed() }
    }

    @Test
    fun toggleContactTracingReminder_thenCancel_contactTracingShouldRemainOn() = notReported {
        testAppContext.getExposureNotificationApi().setEnabled(true)

        startTestActivity<ContactTracingHubActivity>()

        contactTracingHubRobot.checkActivityIsDisplayed()
        contactTracingHubRobot.clickContactTracingToggle()

        exposureNotificationReminderRobot.checkDialogIsDisplayed()
        exposureNotificationReminderRobot.clickCancelButton()

        waitFor { contactTracingHubRobot.checkContactTracingToggledOnIsDisplayed() }
    }

    @Test
    fun toggleContactTracingReminder_remindIn4Hours_shouldSwitchOffContactTracing() = notReported {
        testAppContext.getExposureNotificationApi().setEnabled(true)

        startTestActivity<ContactTracingHubActivity>()

        contactTracingHubRobot.checkActivityIsDisplayed()
        contactTracingHubRobot.clickContactTracingToggle()

        exposureNotificationReminderRobot.checkDialogIsDisplayed()
        exposureNotificationReminderRobot.clickRemindMeIn4Hours()
        exposureNotificationReminderRobot.checkConfirmationDialogIsDisplayed()
        exposureNotificationReminderRobot.clickConfirmationDialogOk()

        waitFor { contactTracingHubRobot.checkContactTracingToggledOffIsDisplayed() }
    }

    @Test
    fun toggleContactTracingReminder_rotateScreen_thenClickCancel_shouldDismissDialogAndShowContactTracingScreen() = notReported {
        testAppContext.getExposureNotificationApi().setEnabled(true)

        startTestActivity<ContactTracingHubActivity>()

        contactTracingHubRobot.checkActivityIsDisplayed()
        contactTracingHubRobot.clickContactTracingToggle()

        exposureNotificationReminderRobot.checkDialogIsDisplayed()

        setScreenOrientation(LANDSCAPE)

        waitFor { exposureNotificationReminderRobot.checkDialogIsDisplayed() }

        setScreenOrientation(PORTRAIT)

        waitFor { exposureNotificationReminderRobot.checkDialogIsDisplayed() }
        exposureNotificationReminderRobot.clickCancelButton()

        contactTracingHubRobot.checkActivityIsDisplayed()
        contactTracingHubRobot.checkContactTracingToggledOnIsDisplayed()
    }

    @Test
    fun startActivityWithShouldTurnOnContactTracingSetToFalse_shouldNotTurnOnContactTracing() = notReported {
        testAppContext.getExposureNotificationApi().setEnabled(false)
        testAppContext.getExposureNotificationApi().activationResult = Result.Success()

        startTestActivity<ContactTracingHubActivity> {
            putExtra(SHOULD_TURN_ON_CONTACT_TRACING, false)
        }

        contactTracingHubRobot.checkActivityIsDisplayed()

        waitFor { contactTracingHubRobot.checkContactTracingToggledOffIsDisplayed() }
    }

    @Test
    fun startActivityWithShouldTurnOnContactTracingSetToTrue_shouldTurnOnContactTracing() = notReported {
        testAppContext.getExposureNotificationApi().setEnabled(false)
        testAppContext.getExposureNotificationApi().activationResult = Result.Success()

        startTestActivity<ContactTracingHubActivity> {
            putExtra(SHOULD_TURN_ON_CONTACT_TRACING, true)
        }

        contactTracingHubRobot.checkActivityIsDisplayed()

        waitFor { contactTracingHubRobot.checkContactTracingToggledOnIsDisplayed() }
    }

    private fun startActivityAndVerifyContactTracingToggledOff() {
        startTestActivity<ContactTracingHubActivity>()

        contactTracingHubRobot.checkActivityIsDisplayed()
        contactTracingHubRobot.checkContactTracingToggledOffIsDisplayed()
        contactTracingHubRobot.clickContactTracingToggle()
    }
}
