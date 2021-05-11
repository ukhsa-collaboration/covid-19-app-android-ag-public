package uk.nhs.nhsx.covid19.android.app.receiver

import android.content.Intent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.FieldInjectionUnitTest

class AlarmRestarterTest : FieldInjectionUnitTest() {

    private val testSubject = AlarmRestarter().apply {
        isolationExpirationAlarmController = mockk(relaxed = true)
        exposureNotificationReminderAlarmController = mockk(relaxed = true)
        submitAnalyticsAlarmController = mockk(relaxed = true)
        resumeContactTracingNotificationTimeProvider = mockk()
        exposureNotificationRetryAlarmController = mockk(relaxed = true)
    }

    private val intent = mockk<Intent>(relaxed = true)

    @Test
    fun `intent action is not ACTION_BOOT_COMPLETED or ACTION_MY_PACKAGE_REPLACED has no side-effects`() {
        every { intent.action } returns Intent.ACTION_LOCKED_BOOT_COMPLETED

        testSubject.onReceive(context, intent)

        verify(exactly = 0) { testSubject.exposureNotificationRetryAlarmController.onAlarmTriggered() }
    }

    @Test
    fun `intent action is ACTION_BOOT_COMPLETED calls exposureNotificationRetryAlarmController`() {
        every { intent.action } returns Intent.ACTION_BOOT_COMPLETED
        every { testSubject.resumeContactTracingNotificationTimeProvider.value } returns null

        testSubject.onReceive(context, intent)

        verify(exactly = 1) { testSubject.exposureNotificationRetryAlarmController.onDeviceRebooted() }
    }

    @Test
    fun `intent action is ACTION_MY_PACKAGE_REPLACED calls exposureNotificationRetryAlarmController`() {
        every { intent.action } returns Intent.ACTION_MY_PACKAGE_REPLACED
        every { testSubject.resumeContactTracingNotificationTimeProvider.value } returns null

        testSubject.onReceive(context, intent)

        verify(exactly = 1) { testSubject.exposureNotificationRetryAlarmController.onDeviceRebooted() }
    }

    @Test
    fun `intent action is ACTION_BOOT_COMPLETED calls submitAnalyticsAlarmController`() {
        every { intent.action } returns Intent.ACTION_BOOT_COMPLETED
        every { testSubject.resumeContactTracingNotificationTimeProvider.value } returns null

        testSubject.onReceive(context, intent)

        verify(exactly = 1) { testSubject.submitAnalyticsAlarmController.onDeviceRebooted() }
    }

    @Test
    fun `intent action is ACTION_MY_PACKAGE_REPLACED calls submitAnalyticsAlarmController`() {
        every { intent.action } returns Intent.ACTION_MY_PACKAGE_REPLACED
        every { testSubject.resumeContactTracingNotificationTimeProvider.value } returns null

        testSubject.onReceive(context, intent)

        verify(exactly = 1) { testSubject.submitAnalyticsAlarmController.onDeviceRebooted() }
    }

    @Test
    fun `intent action is ACTION_BOOT_COMPLETED calls isolationExpirationAlarmController`() {
        every { intent.action } returns Intent.ACTION_BOOT_COMPLETED
        every { testSubject.resumeContactTracingNotificationTimeProvider.value } returns null

        testSubject.onReceive(context, intent)

        verify(exactly = 1) { testSubject.isolationExpirationAlarmController.onDeviceRebooted() }
    }

    @Test
    fun `intent action is ACTION_MY_PACKAGE_REPLACED calls isolationExpirationAlarmController`() {
        every { intent.action } returns Intent.ACTION_MY_PACKAGE_REPLACED
        every { testSubject.resumeContactTracingNotificationTimeProvider.value } returns null

        testSubject.onReceive(context, intent)

        verify(exactly = 1) { testSubject.isolationExpirationAlarmController.onDeviceRebooted() }
    }

    @Test
    fun `intent action is ACTION_BOOT_COMPLETED calls exposureNotificationReminderAlarmController when resumeContactTracingNotificationTimeProvider contains time`() {
        val nowEpochMilli = Instant.now().toEpochMilli()
        every { intent.action } returns Intent.ACTION_BOOT_COMPLETED
        every { testSubject.resumeContactTracingNotificationTimeProvider.value } returns nowEpochMilli

        testSubject.onReceive(context, intent)

        verify(exactly = 1) { testSubject.exposureNotificationReminderAlarmController.setup(Instant.ofEpochMilli(nowEpochMilli)) }
    }

    @Test
    fun `intent action is ACTION_MY_PACKAGE_REPLACED calls exposureNotificationReminderAlarmController when resumeContactTracingNotificationTimeProvider contains time`() {
        val nowEpochMilli = Instant.now().toEpochMilli()
        every { intent.action } returns Intent.ACTION_MY_PACKAGE_REPLACED
        every { testSubject.resumeContactTracingNotificationTimeProvider.value } returns nowEpochMilli

        testSubject.onReceive(context, intent)

        verify(exactly = 1) { testSubject.exposureNotificationReminderAlarmController.setup(Instant.ofEpochMilli(nowEpochMilli)) }
    }

    @Test
    fun `intent action is ACTION_BOOT_COMPLETED does not call exposureNotificationReminderAlarmController when resumeContactTracingNotificationTimeProvider is empty`() {
        every { intent.action } returns Intent.ACTION_BOOT_COMPLETED
        every { testSubject.resumeContactTracingNotificationTimeProvider.value } returns null

        testSubject.onReceive(context, intent)

        verify(exactly = 0) { testSubject.exposureNotificationReminderAlarmController.setup(any()) }
    }

    @Test
    fun `intent action is ACTION_MY_PACKAGE_REPLACED does not call exposureNotificationReminderAlarmController when resumeContactTracingNotificationTimeProvider is empty`() {
        every { intent.action } returns Intent.ACTION_MY_PACKAGE_REPLACED
        every { testSubject.resumeContactTracingNotificationTimeProvider.value } returns null

        testSubject.onReceive(context, intent)

        verify(exactly = 0) { testSubject.exposureNotificationReminderAlarmController.setup(any()) }
    }
}
