package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.CountrySpecificConfiguration
import uk.nhs.nhsx.covid19.android.app.state.GetLatestConfiguration
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CalculateKeySubmissionDateRangeTest {
    private val getLatestConfiguration = mockk<GetLatestConfiguration>()
    private val fixedClock = Clock.fixed(Instant.parse("2020-07-15T12:00:00Z"), ZoneOffset.UTC)

    val testSubject = CalculateKeySubmissionDateRange(
        getLatestConfiguration,
        fixedClock
    )

    @Before
    fun setUp() {
        val configuration = mockk<CountrySpecificConfiguration>()
        every { getLatestConfiguration() } returns configuration
        every { configuration.contactCase } returns 11
    }

    @Test
    fun `test calculation with onsetDate based start date`() {
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
}
