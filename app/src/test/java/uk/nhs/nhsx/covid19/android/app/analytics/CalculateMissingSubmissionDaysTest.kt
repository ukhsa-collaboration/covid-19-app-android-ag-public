package uk.nhs.nhsx.covid19.android.app.analytics

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsWindow
import java.time.LocalDate
import kotlin.test.assertEquals

class CalculateMissingSubmissionDaysTest {

    private val analyticsSubmissionLogStorage = mockk<AnalyticsSubmissionLogStorage>()

    val testSubject = CalculateMissingSubmissionDays(analyticsSubmissionLogStorage)

    @Test
    fun testCalculationAllDaysSubmitted() {
        every { analyticsSubmissionLogStorage.getLogForAnalyticsWindow(LocalDate.of(2020, 10, 8)) } returns setOf(
            LocalDate.of(2020, 10, 1),
            LocalDate.of(2020, 10, 2),
            LocalDate.of(2020, 10, 3),
            LocalDate.of(2020, 10, 4),
            LocalDate.of(2020, 10, 5),
            LocalDate.of(2020, 10, 6),
            LocalDate.of(2020, 10, 7),
        )

        val missingDays = testSubject.invoke(AnalyticsWindow(startDate = "2020-10-08T00:00:00Z", endDate = "2020-10-09T00:00:00Z"))
        assertEquals(0, missingDays)
    }

    @Test
    fun testCalculationOneDayMissing() {
        every { analyticsSubmissionLogStorage.getLogForAnalyticsWindow(LocalDate.of(2020, 10, 8)) } returns setOf(
            LocalDate.of(2020, 10, 1),
            LocalDate.of(2020, 10, 2),
            LocalDate.of(2020, 10, 3),
            LocalDate.of(2020, 10, 5),
            LocalDate.of(2020, 10, 6),
            LocalDate.of(2020, 10, 7),
        )

        val missingDays = testSubject.invoke(AnalyticsWindow(startDate = "2020-10-08T00:00:00Z", endDate = "2020-10-09T00:00:00Z"))
        assertEquals(1, missingDays)
    }

    @Test
    fun testCalculationAllDaysMissing() {
        every { analyticsSubmissionLogStorage.getLogForAnalyticsWindow(LocalDate.of(2020, 10, 8)) } returns setOf()

        val missingDays = testSubject.invoke(AnalyticsWindow(startDate = "2020-10-08T00:00:00Z", endDate = "2020-10-09T00:00:00Z"))
        assertEquals(7, missingDays)
    }

    @Test
    fun testAnalyticsWindowConversion() {
        val analyticsWindow = AnalyticsWindow(startDate = "2020-10-08T00:00:00Z", endDate = "2020-10-09T00:00:00Z")

        assertEquals(LocalDate.of(2020, 10, 8), analyticsWindow.startDateToLocalDate())
    }
}
