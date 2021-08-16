package uk.nhs.nhsx.covid19.android.app.status.isolationhub

import io.mockk.mockk
import io.mockk.verifyOrder
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ScheduleIsolationHubReminderTest {

    private val isolationHubReminderAlarmController = mockk<IsolationHubReminderAlarmController>(relaxUnitFun = true)
    private val isolationHubReminderTimeProvider = mockk<IsolationHubReminderTimeProvider>(relaxUnitFun = true)
    private val fixedClock = Clock.fixed(Instant.parse("2021-01-01T00:00:00Z"), ZoneOffset.UTC)

    private val scheduleIsolationHubReminder = ScheduleIsolationHubReminder(
        isolationHubReminderAlarmController,
        isolationHubReminderTimeProvider,
        fixedClock
    )

    @Test
    fun `when called, stores time for reminder 24 hours from now and schedules reminder`() {
        scheduleIsolationHubReminder()

        val expectedReminderTimeMillis = Instant.parse("2021-01-02T00:00:00Z").toEpochMilli()

        verifyOrder {
            isolationHubReminderTimeProvider setProperty "value" value eq(expectedReminderTimeMillis)
            isolationHubReminderAlarmController.setup(expectedReminderTimeMillis)
        }
    }
}
