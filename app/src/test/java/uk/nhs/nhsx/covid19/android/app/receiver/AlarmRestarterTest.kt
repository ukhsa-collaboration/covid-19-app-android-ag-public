package uk.nhs.nhsx.covid19.android.app.receiver

import android.content.Intent
import com.jeroenmols.featureflag.framework.FeatureFlag.SUBMIT_ANALYTICS_VIA_ALARM_MANAGER
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.FieldInjectionUnitTest
import uk.nhs.nhsx.covid19.android.app.status.contacttracinghub.ContactTracingActivationReminder
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeature
import java.time.Instant

class AlarmRestarterTest : FieldInjectionUnitTest() {

    private val testSubject = AlarmRestarter().apply {
        isolationExpirationAlarmController = mockk(relaxed = true)
        exposureNotificationReminderAlarmController = mockk(relaxed = true)
        submitAnalyticsAlarmController = mockk(relaxed = true)
        migrateContactTracingActivationReminderProvider = mockk(relaxUnitFun = true)
        contactTracingActivationReminderProvider = mockk()
        exposureNotificationRetryAlarmController = mockk(relaxed = true)
    }

    private val intent = mockk<Intent>()

    @Before
    override fun setUp() {
        super.setUp()
        every { intent.action } returns Intent.ACTION_BOOT_COMPLETED
        every { testSubject.contactTracingActivationReminderProvider.reminder } returns null
    }

    @Test
    fun `intent action is not ACTION_BOOT_COMPLETED or ACTION_MY_PACKAGE_REPLACED has no side-effects`() {
        every { intent.action } returns Intent.ACTION_LOCKED_BOOT_COMPLETED

        testSubject.onReceive(context, intent)

        verify(exactly = 0) { testSubject.exposureNotificationRetryAlarmController.onAlarmTriggered() }
    }

    @Test
    fun `intent action is ACTION_BOOT_COMPLETED`() {
        testSubject.onReceive(context, intent)

        with(testSubject) {
            verify {
                exposureNotificationRetryAlarmController.onDeviceRebooted()
                submitAnalyticsAlarmController.onDeviceRebooted()
                isolationExpirationAlarmController.onDeviceRebooted()
                migrateContactTracingActivationReminderProvider()
            }
        }
    }

    @Test
    fun `intent action is ACTION_BOOT_COMPLETED with feature flag SUBMIT_ANALYTICS_VIA_ALARM_MANAGER disabled`() =
        runWithFeature(SUBMIT_ANALYTICS_VIA_ALARM_MANAGER, enabled = false) {
            every { intent.action } returns Intent.ACTION_BOOT_COMPLETED

            testSubject.onReceive(context, intent)

            with(testSubject) {
                verify {
                    exposureNotificationRetryAlarmController.onDeviceRebooted()
                    isolationExpirationAlarmController.onDeviceRebooted()
                    migrateContactTracingActivationReminderProvider()
                }
                verify(exactly = 0) { submitAnalyticsAlarmController.onDeviceRebooted() }
            }
        }

    @Test
    fun `intent action is ACTION_MY_PACKAGE_REPLACED`() {
        every { intent.action } returns Intent.ACTION_MY_PACKAGE_REPLACED

        testSubject.onReceive(context, intent)

        with(testSubject) {
            verify {
                exposureNotificationRetryAlarmController.onDeviceRebooted()
                submitAnalyticsAlarmController.onDeviceRebooted()
                isolationExpirationAlarmController.onDeviceRebooted()
                migrateContactTracingActivationReminderProvider()
            }
        }
    }

    @Test
    fun `when time stored in ContactTracingActivationReminderProvider then set up exposure notification reminder`() {
        val nowEpochMilli = Instant.now().toEpochMilli()

        every { testSubject.contactTracingActivationReminderProvider.reminder } returns
            ContactTracingActivationReminder(nowEpochMilli)

        testSubject.onReceive(context, intent)

        with(testSubject) {
            verify {
                exposureNotificationRetryAlarmController.onDeviceRebooted()
                submitAnalyticsAlarmController.onDeviceRebooted()
                isolationExpirationAlarmController.onDeviceRebooted()
            }
            verifyOrder {
                migrateContactTracingActivationReminderProvider()
                exposureNotificationReminderAlarmController.setup(Instant.ofEpochMilli(nowEpochMilli))
            }
        }
    }

    @Test
    fun `when no time stored in ContactTracingActivationReminderProvider then do not set up exposure notification reminder`() {
        every { intent.action } returns Intent.ACTION_BOOT_COMPLETED

        testSubject.onReceive(context, intent)

        with(testSubject) {
            verify {
                exposureNotificationRetryAlarmController.onDeviceRebooted()
                submitAnalyticsAlarmController.onDeviceRebooted()
                isolationExpirationAlarmController.onDeviceRebooted()
                migrateContactTracingActivationReminderProvider()
            }
            verify(exactly = 0) { exposureNotificationReminderAlarmController.setup(any()) }
        }
    }
}
