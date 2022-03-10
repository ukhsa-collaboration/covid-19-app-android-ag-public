package uk.nhs.nhsx.covid19.android.app.state

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.Contact
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.OptOutOfContactIsolation
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals

class CalculateContactExpiryDateTest {

    private val fixedClock = Clock.fixed(Instant.parse("2020-01-15T10:00:00Z"), ZoneOffset.UTC)

    private val testSubject = CalculateContactExpiryDate()
    private val isolationConfiguration = IsolationConfiguration()

    @Test
    fun `when contact has opt-out date, use opt-out date as expiry date`() {
        val contact = Contact(
            exposureDate = LocalDate.now(fixedClock).minusDays(3),
            notificationDate = LocalDate.now(fixedClock).minusDays(2),
            optOutOfContactIsolation = OptOutOfContactIsolation(LocalDate.now(fixedClock).minusDays(1))
        )

        val expiryDate = testSubject(contact, isolationConfiguration)

        assertEquals(contact.optOutOfContactIsolation?.date, expiryDate)
    }

    @Test
    fun `when contact has no opt-out date, use exposure date as base`() {
        val contact = Contact(
            exposureDate = LocalDate.now(fixedClock).minusDays(3),
            notificationDate = LocalDate.now(fixedClock).minusDays(2)
        )

        val expiryDate = testSubject(contact, isolationConfiguration)

        val expectedExpiryDate = contact.exposureDate
            .plusDays(isolationConfiguration.contactCase.toLong())

        assertEquals(expectedExpiryDate, expiryDate)
    }
}
