package uk.nhs.nhsx.covid19.android.app.state

import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals

class CalculateExpirationNotificationTimeTest {

    private val fixedClock = Clock.fixed(Instant.parse("2021-01-10T10:00:00Z"), ZoneOffset.UTC)

    private val testSubject = CalculateExpirationNotificationTime(fixedClock)

    @Test
    fun `expiration notification time is day before expiration at 9pm`() {
        val expiryDate = LocalDate.now(fixedClock)

        val expirationNotificationTime = testSubject(expiryDate)

        val expectedNotificationTime = Instant.parse("2021-01-09T21:00:00Z")
        assertEquals(expectedNotificationTime, expirationNotificationTime)
    }
}
