package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EvaluateTestingAdviceToShow.TestingAdviceToShow
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals

class EvaluateTestingAdviceToShowTest {
    private val mockGetRiskyContactEncounterDate = mockk<GetRiskyContactEncounterDate>()
    private val mockLocalAuthorityPostCodeProvider = mockk<LocalAuthorityPostCodeProvider>()

    private val shouldShowExtendedTestingAdvice = EvaluateTestingAdviceToShow(
        mockGetRiskyContactEncounterDate,
        mockLocalAuthorityPostCodeProvider
    )

    private val encounterDate = LocalDate.of(2021, 9, 10)
    private val day8 = LocalDate.of(2021, 9, 18)

    @Before
    fun setUp() {
        every { mockGetRiskyContactEncounterDate() } returns encounterDate
    }

    @Test
    fun `Welsh user within extended advice window should see extended testing advice with day 8 date`() = runBlocking {
        coEvery { mockLocalAuthorityPostCodeProvider.getPostCodeDistrict() } returns WALES
        val clock = Clock.fixed(Instant.parse("2021-09-11T10:00:00Z"), ZoneOffset.UTC)

        val actual = shouldShowExtendedTestingAdvice(clock)

        val expected = TestingAdviceToShow.WalesWithinAdviceWindow(date = day8)
        assertEquals(expected, actual)
    }

    @Test
    fun `Welsh user outside extended advice window should see default advice`() = runBlocking {
        coEvery { mockLocalAuthorityPostCodeProvider.getPostCodeDistrict() } returns WALES
        val clock = Clock.fixed(Instant.parse("2021-09-16T10:00:00Z"), ZoneOffset.UTC)

        val actual = shouldShowExtendedTestingAdvice(clock)

        val expected = TestingAdviceToShow.Default
        assertEquals(expected, actual)
    }

    @Test
    fun `English user always sees no change to current advice`() = runBlocking {
        coEvery { mockLocalAuthorityPostCodeProvider.getPostCodeDistrict() } returns ENGLAND
        val clock = Clock.fixed(Instant.parse("2021-09-11T10:00:00Z"), ZoneOffset.UTC)

        val actual = shouldShowExtendedTestingAdvice(clock)

        val expected = TestingAdviceToShow.Default
        assertEquals(expected, actual)
    }

    @Test
    fun `returns UnknownExposureDate when risky contact encounter date is null`() = runBlocking {
        coEvery { mockLocalAuthorityPostCodeProvider.getPostCodeDistrict() } returns WALES
        every { mockGetRiskyContactEncounterDate() } returns null
        val clock = Clock.fixed(Instant.parse("2021-09-16T10:00:00Z"), ZoneOffset.UTC)

        val actual = shouldShowExtendedTestingAdvice(clock)

        val expected = TestingAdviceToShow.UnknownExposureDate
        assertEquals(expected, actual)
    }
}
