package uk.nhs.nhsx.covid19.android.app.status.contacttracinghub

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationReminderAlarmController
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class ScheduleContactTracingActivationAdditionalReminderIfNeededTest {

    private val exposureNotificationReminderAlarmController =
        mockk<ExposureNotificationReminderAlarmController>(relaxUnitFun = true)
    private val contactTracingActivationReminderProvider =
        mockk<ContactTracingActivationReminderProvider>(relaxUnitFun = true)
    private val instant = Instant.parse("2021-01-10T10:00:00Z")
    private val utcClock = Clock.fixed(instant, ZoneOffset.UTC)

    private fun createTestSubject(clock: Clock = utcClock) =
        ScheduleContactTracingActivationAdditionalReminderIfNeeded(
            exposureNotificationReminderAlarmController,
            contactTracingActivationReminderProvider,
            clock
        )

    private val contactTracingReminder = mockk<ContactTracingActivationReminder>()

    @Before
    fun setUp() {
        every { contactTracingReminder.hasAlreadyScheduledAdditionalReminder() } returns false
        every { contactTracingActivationReminderProvider.reminder } returns contactTracingReminder
    }

    @Test
    fun `when no reminder stored locally then do not schedule additional reminder`() {
        every { contactTracingActivationReminderProvider.reminder } returns null

        val scheduleContactTracingActivationAdditionalReminderIfNeeded = createTestSubject()

        scheduleContactTracingActivationAdditionalReminderIfNeeded()

        verify(exactly = 0) { contactTracingActivationReminderProvider setProperty "reminder" value null }
        verify(exactly = 0) { exposureNotificationReminderAlarmController.setup(any()) }
    }

    @Test
    fun `do not schedule additional reminder and reset stored reminder data if additional reminder already scheduled`() {
        every { contactTracingReminder.hasAlreadyScheduledAdditionalReminder() } returns true
        every { contactTracingActivationReminderProvider.reminder } returns contactTracingReminder

        val scheduleContactTracingActivationAdditionalReminderIfNeeded = createTestSubject()

        scheduleContactTracingActivationAdditionalReminderIfNeeded()

        verify { contactTracingActivationReminderProvider setProperty "reminder" value null }
        verify(exactly = 0) { exposureNotificationReminderAlarmController.setup(any()) }
    }

    @Test
    fun `when no additional reminder already scheduled then schedule additional reminder the next day at 4pm when user is in UTC time zone`() {
        every { contactTracingReminder.additionalReminderCount } returns 0

        val scheduleContactTracingActivationAdditionalReminderIfNeeded = createTestSubject()

        scheduleContactTracingActivationAdditionalReminderIfNeeded()

        val expectedInstant = Instant.parse("2021-01-11T16:00:00Z")
        val expectedContactTracingReminder = ContactTracingActivationReminder(
            alarmTime = expectedInstant.toEpochMilli(),
            additionalReminderCount = 1
        )

        verify { exposureNotificationReminderAlarmController.setup(expectedInstant) }
        verify { contactTracingActivationReminderProvider setProperty "reminder" value eq(expectedContactTracingReminder) }
    }

    @Test
    fun `when no additional reminder already scheduled then schedule additional reminder the next day at 4pm when user is in Paris time zone`() {
        every { contactTracingReminder.additionalReminderCount } returns 0

        val parisClock = Clock.fixed(instant, ZoneId.of("Europe/Paris"))
        val scheduleContactTracingActivationAdditionalReminderIfNeeded = createTestSubject(clock = parisClock)

        scheduleContactTracingActivationAdditionalReminderIfNeeded()

        val expectedInstant = ZonedDateTime.parse("2021-01-11T16:00:00+01:00[Europe/Paris]").toInstant()
        val expectedContactTracingReminder = ContactTracingActivationReminder(
            alarmTime = expectedInstant.toEpochMilli(),
            additionalReminderCount = 1
        )

        verify { exposureNotificationReminderAlarmController.setup(expectedInstant) }
        verify { contactTracingActivationReminderProvider setProperty "reminder" value eq(expectedContactTracingReminder) }
    }

    @Test
    fun `when no additional reminder already scheduled then schedule additional reminder the next day at 4pm when user is in Australia-Canberra time zone`() {
        every { contactTracingReminder.additionalReminderCount } returns 0

        val canberraClock = Clock.fixed(instant, ZoneId.of("Australia/Canberra"))
        val scheduleContactTracingActivationAdditionalReminderIfNeeded = createTestSubject(clock = canberraClock)

        scheduleContactTracingActivationAdditionalReminderIfNeeded()

        val expectedInstant = ZonedDateTime.parse("2021-01-11T16:00:00+11:00[Australia/Canberra]").toInstant()
        val expectedContactTracingReminder = ContactTracingActivationReminder(
            alarmTime = expectedInstant.toEpochMilli(),
            additionalReminderCount = 1
        )

        verify { exposureNotificationReminderAlarmController.setup(expectedInstant) }
        verify { contactTracingActivationReminderProvider setProperty "reminder" value eq(expectedContactTracingReminder) }
    }
}
