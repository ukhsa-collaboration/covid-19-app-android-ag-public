package uk.nhs.nhsx.covid19.android.app.state

import io.mockk.mockk
import io.mockk.verifyOrder
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowEncounterDetection
import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationRetryAlarmController
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox

class ExposureNotificationHandlerTest {

    private val userInbox = mockk<UserInbox>(relaxUnitFun = true)
    private val notificationProvider = mockk<NotificationProvider>(relaxUnitFun = true)
    private val exposureNotificationRetryAlarmController =
        mockk<ExposureNotificationRetryAlarmController>(relaxUnitFun = true)

    private val testSubject = ExposureNotificationHandler(
        userInbox,
        notificationProvider,
        exposureNotificationRetryAlarmController
    )

    @Test
    fun `show new exposure notification`() {
        testSubject.show()

        verifyOrder {
            userInbox.addUserInboxItem(ShowEncounterDetection)
            notificationProvider.showExposureNotification()
            exposureNotificationRetryAlarmController.setupNextAlarm()
        }
    }

    @Test
    fun `cancel existing exposure notification`() {
        testSubject.cancel()

        verifyOrder {
            userInbox.clearItem(ShowEncounterDetection)
            exposureNotificationRetryAlarmController.cancel()
        }
    }
}
