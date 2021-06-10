package uk.nhs.nhsx.covid19.android.app.state

import io.mockk.mockk
import io.mockk.verifyOrder
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationRetryAlarmController
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.ShouldShowEncounterDetectionActivityProvider

class ExposureNotificationHandlerTest {

    private val shouldShowEncounterDetectionActivityProvider =
        mockk<ShouldShowEncounterDetectionActivityProvider>(relaxUnitFun = true)
    private val notificationProvider = mockk<NotificationProvider>(relaxUnitFun = true)
    private val exposureNotificationRetryAlarmController =
        mockk<ExposureNotificationRetryAlarmController>(relaxUnitFun = true)

    private val testSubject = ExposureNotificationHandler(
        shouldShowEncounterDetectionActivityProvider,
        notificationProvider,
        exposureNotificationRetryAlarmController
    )

    @Test
    fun `show new exposure notification`() {
        testSubject.show()

        verifyOrder {
            shouldShowEncounterDetectionActivityProvider setProperty "value" value eq(true)
            notificationProvider.showExposureNotification()
            exposureNotificationRetryAlarmController.setupNextAlarm()
        }
    }

    @Test
    fun `cancel existing exposure notification`() {
        testSubject.cancel()

        verifyOrder {
            shouldShowEncounterDetectionActivityProvider setProperty "value" value null
            exposureNotificationRetryAlarmController.cancel()
        }
    }
}
