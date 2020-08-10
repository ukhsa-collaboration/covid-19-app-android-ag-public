package uk.nhs.nhsx.covid19.android.app.state

import android.app.AlarmManager
import android.content.Context
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class IsolationExpirationAlarmControllerTest {

    private val context = mockk<Context>(relaxed = true)
    private val alarmManager = mockk<AlarmManager>(relaxed = true)
    private val testSubject = IsolationExpirationAlarmController(context, alarmManager)

    @Test
    fun `setupExpirationCheck calls AlarmManager with proper time`() {
        val expiryDate = LocalDate.of(2020, 7, 20)
        val zone = ZoneId.of("Europe/London")

        testSubject.setupExpirationCheck(expiryDate, zone)

        val alarmTime = Instant.parse("2020-07-19T20:00:00Z").toEpochMilli()

        verify {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmTime,
                any()
            )
        }
    }
}
