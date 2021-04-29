package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.SymptomsDate
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals

class SymptomsOnsetDateCalculatorTest {

    private val fixedClock = Clock.fixed(Instant.parse("2020-07-10T00:00:00Z"), ZoneOffset.UTC)
    private val testEndDate = Instant.parse("2020-07-10T12:00:00Z")
    private val testEndDateMinusThreeDays = testEndDate.minus(3, ChronoUnit.DAYS).toLocalDate(fixedClock.zone)

    private val testResult = ReceivedTestResult(
        diagnosisKeySubmissionToken = "token",
        testEndDate = testEndDate,
        testResult = POSITIVE,
        testKitType = LAB_RESULT,
        diagnosisKeySubmissionSupported = true
    )

    private val testSubject = SymptomsOnsetDateCalculator(fixedClock)

    @Test
    fun `can get most trustworthy onset date when in isolation but not as index case`() {
        val state = Isolation(
            Instant.now(fixedClock),
            DurationDays(),
            contactCase = ContactCase(
                startDate = Instant.parse("2020-07-08T12:00:00Z"),
                null,
                expiryDate = LocalDate.now().plusDays(5)
            )
        )

        val mostTrustworthyDate = testSubject.getMostTrustworthyOnsetDate(testResult, state)
        assertEquals(testEndDateMinusThreeDays, mostTrustworthyDate)
    }

    @Test
    fun `can get most trustworthy onset date when in isolation as index case`() {
        val symptomsOnsetDate = Instant.parse("2020-07-08T12:00:00Z").toLocalDate(fixedClock.zone)
        val state = Isolation(
            Instant.now(fixedClock),
            DurationDays(),
            indexCase = IndexCase(
                symptomsOnsetDate = symptomsOnsetDate,
                expiryDate = LocalDate.now().plusDays(5),
                selfAssessment = true
            )
        )

        val mostTrustworthyDate = testSubject.getMostTrustworthyOnsetDate(testResult, state)
        assertEquals(symptomsOnsetDate, mostTrustworthyDate)
    }

    @Test
    fun `can get most trustworthy onset date when in default but last isolation was index case`() {
        val symptomsOnsetDate = Instant.parse("2020-07-09T12:00:00Z").toLocalDate(fixedClock.zone)
        val previousIsolation = Isolation(
            Instant.now(fixedClock),
            DurationDays(),
            indexCase = IndexCase(
                symptomsOnsetDate = symptomsOnsetDate,
                expiryDate = LocalDate.now().plusDays(5),
                selfAssessment = true
            )
        )

        val state = Default(previousIsolation = previousIsolation)

        val mostTrustworthyDate = testSubject.getMostTrustworthyOnsetDate(testResult, state)

        assertEquals(symptomsOnsetDate, mostTrustworthyDate)
    }

    @Test
    fun `can get most trustworthy onset date when in default and last isolation was not index case`() {
        val previousIsolation = Isolation(
            Instant.now(fixedClock),
            DurationDays(),
            contactCase = ContactCase(
                startDate = Instant.parse("2020-07-08T12:00:00Z"),
                null,
                expiryDate = LocalDate.now().plusDays(5)
            )
        )

        val state = Default(previousIsolation = previousIsolation)

        val mostTrustworthyDate = testSubject.getMostTrustworthyOnsetDate(testResult, state)

        assertEquals(testEndDateMinusThreeDays, mostTrustworthyDate)
    }

    @Test
    fun `can get explicit onset date from test result if it is set`() {
        val symptomsOnsetDate = SymptomsDate(Instant.parse("2020-07-06T12:00:00Z").toLocalDate(fixedClock.zone))
        val testResultWithExplicitOnset = testResult.copy(symptomsOnsetDate = symptomsOnsetDate)

        val date = testSubject.symptomsOnsetDateFromTestResult(testResultWithExplicitOnset)

        assertEquals(symptomsOnsetDate.explicitDate, date)
    }

    @Test
    fun `can calculate onset date from test result end date if explicit date is not set`() {
        val date = testSubject.symptomsOnsetDateFromTestResult(testResult)

        assertEquals(testEndDateMinusThreeDays, date)
    }
}
