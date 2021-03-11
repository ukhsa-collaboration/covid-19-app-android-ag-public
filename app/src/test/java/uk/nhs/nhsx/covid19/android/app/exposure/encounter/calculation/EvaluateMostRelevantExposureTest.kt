package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.ExposureWindowUtils.Companion.getExposureWindowWithRisk
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class EvaluateMostRelevantExposureTest {
    private val evaluateMostRelevantExposure = EvaluateMostRelevantExposure()

    @Test
    fun `returns null when no risky exposures passed`() {
        val relevantRisk = evaluateMostRelevantExposure(listOf())
        assertNull(relevantRisk)
    }

    @Test
    fun `returns the day risk corresponding to the most recent encounter`() {
        val olderDate = todayMinusDays(3)
        val newerDate = todayMinusDays(2)
        val exposureWindowsWithRisk = listOf(
            getExposureWindowWithRisk(
                millisSinceEpoch = olderDate.toStartOfDayEpochMillis(),
                calculatedRisk = 200.0
            ),
            getExposureWindowWithRisk(
                millisSinceEpoch = newerDate.toStartOfDayEpochMillis(),
                calculatedRisk = 90.0
            ),
            getExposureWindowWithRisk(
                millisSinceEpoch = newerDate.toStartOfDayEpochMillis(),
                calculatedRisk = 100.0
            )
        )

        val expectedDateMillis = newerDate.toStartOfDayEpochMillis()
        val expectedRisk =
            DayRisk(
                startOfDayMillis = expectedDateMillis,
                calculatedRisk = 100.0,
                riskCalculationVersion = 2,
                matchedKeyCount = 1
            )

        val relevantRisk = evaluateMostRelevantExposure(exposureWindowsWithRisk)
        assertEquals(expectedRisk, relevantRisk)
    }

    private val baseDate: Instant = Instant.parse("2020-07-20T00:00:00Z")

    private fun todayMinusDays(days: Long): LocalDate =
        baseDate.atZone(ZoneOffset.UTC).minusDays(days).toLocalDate()

    private fun LocalDate.toStartOfDayEpochMillis(): Long =
        atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
}
