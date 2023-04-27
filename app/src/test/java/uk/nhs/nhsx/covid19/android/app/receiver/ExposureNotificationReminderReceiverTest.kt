package uk.nhs.nhsx.covid19.android.app.receiver

import android.content.Intent
import com.jeroenmols.featureflag.framework.FeatureFlag.DECOMMISSIONING_CLOSURE_SCREEN
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.FieldInjectionUnitTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeature

class ExposureNotificationReminderReceiverTest : FieldInjectionUnitTest() {

    private val testSubject = ExposureNotificationReminderReceiver().apply {
        notificationProvider = mockk(relaxUnitFun = true)
        scheduleContactTracingActivationAdditionalReminderIfNeeded = mockk(relaxUnitFun = true)
        exposureNotificationApi = mockk(relaxUnitFun = true)
    }

    private val intent = mockk<Intent>()

    @Test
    fun `on receive triggers reminder notification and schedules additional reminder if exposure notifications are disabled`() =
        runBlocking {
            runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = false) {
                coEvery { testSubject.exposureNotificationApi.isEnabled() } returns false
                testSubject.onReceive(context, intent)
                verify {
                    testSubject.notificationProvider.showExposureNotificationReminder()
                    testSubject.scheduleContactTracingActivationAdditionalReminderIfNeeded()
                }
            }
        }

    @Test
    fun `on receive does not trigger reminder notification and schedules additional reminder if exposure notifications are enabled`() =
        runBlocking {
            runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = false) {
                coEvery { testSubject.exposureNotificationApi.isEnabled() } returns true
                testSubject.onReceive(context, intent)

                verify(exactly = 0) { testSubject.notificationProvider.showExposureNotificationReminder() }
                verify(exactly = 0) { testSubject.scheduleContactTracingActivationAdditionalReminderIfNeeded() }
            }
        }

    @Test
    fun `on receive does not trigger reminder notification and schedules additional reminder if in decommissioning state`() =
        runBlocking {
            runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = true) {
                coEvery { testSubject.exposureNotificationApi.isEnabled() } returns false
                testSubject.onReceive(context, intent)

                verify(exactly = 0) { testSubject.notificationProvider.showExposureNotificationReminder() }
                verify(exactly = 0) { testSubject.scheduleContactTracingActivationAdditionalReminderIfNeeded() }
            }
        }
}
