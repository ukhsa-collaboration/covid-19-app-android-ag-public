package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CalculateKeySubmissionDateRangeTest {
    private val isolationConfigurationProvider = mockk<IsolationConfigurationProvider>()
    private val fixedClock = Clock.fixed(Instant.parse("2020-07-15T12:00:00Z"), ZoneOffset.UTC)

    val testSubject = CalculateKeySubmissionDateRange(
        isolationConfigurationProvider = isolationConfigurationProvider,
        clock = fixedClock
    )

    @Test
    fun `test calculation with onsetDate based start date`() {
        every { isolationConfigurationProvider.durationDays } returns isolationConfiguration
        val result = testSubject(
            KeySharingInfo(
                diagnosisKeySubmissionToken = "token",
                acknowledgedDate = Instant.parse("2020-07-15T12:00:00Z"),
                notificationSentDate = null
            ).acknowledgedDate,
            LocalDate.of(2020, 7, 12)
        )

        assertNotNull(result)
        assertEquals(LocalDate.of(2020, 7, 10), result.firstSubmissionDate)
        assertEquals(LocalDate.of(2020, 7, 14), result.lastSubmissionDate)
        assertTrue { result.containsAtLeastOneDay() }
    }

    @Test
    fun `test calculation with isolationDuration based startDate`() {
        every { isolationConfigurationProvider.durationDays } returns isolationConfiguration
        val result = testSubject(
            KeySharingInfo(
                diagnosisKeySubmissionToken = "token",
                acknowledgedDate = Instant.parse("2020-07-15T12:00:00Z"),
                notificationSentDate = null
            ).acknowledgedDate,
            LocalDate.of(2020, 7, 5)
        )

        assertNotNull(result)
        assertEquals(LocalDate.of(2020, 7, 5), result.firstSubmissionDate)
        assertEquals(LocalDate.of(2020, 7, 14), result.lastSubmissionDate)
        assertTrue { result.containsAtLeastOneDay() }
    }

    @Test
    fun `test calculation with startDate later than endDate`() {
        every { isolationConfigurationProvider.durationDays } returns isolationConfiguration
        val result = testSubject(
            KeySharingInfo(
                diagnosisKeySubmissionToken = "token",
                acknowledgedDate = Instant.parse("2020-07-03T12:00:00Z"),
                notificationSentDate = null
            ).acknowledgedDate,
            LocalDate.of(2020, 7, 5)
        )

        assertNotNull(result)
        assertEquals(LocalDate.of(2020, 7, 5), result.firstSubmissionDate)
        assertEquals(LocalDate.of(2020, 7, 2), result.lastSubmissionDate)
        assertFalse { result.containsAtLeastOneDay() }
    }

    val isolationConfiguration = DurationDays(contactCase = 11)
}
