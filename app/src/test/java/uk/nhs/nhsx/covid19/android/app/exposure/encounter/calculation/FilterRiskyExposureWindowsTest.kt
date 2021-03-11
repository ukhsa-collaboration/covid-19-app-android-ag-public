package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.ExposureWindowUtils.Companion.getExposureWindowWithRisk
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class FilterRiskyExposureWindowsTest {

    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val isolationConfigurationProvider = mockk<IsolationConfigurationProvider>()
    private val baseDate: Instant = Instant.parse("2020-07-20T00:00:00Z")
    private val clock = Clock.fixed(baseDate, ZoneOffset.UTC)

    private val filterRiskyExposureWindows =
        FilterRiskyExposureWindows(isolationConfigurationProvider, isolationStateMachine, clock)

    private val durationDays = DurationDays()

    @Before
    fun setUp() {
        every { isolationConfigurationProvider.durationDays } returns durationDays
    }

    @Test
    fun `return empty list when no exposure windows present`() {
        val riskyExposureWindows = filterRiskyExposureWindows(listOf(), 100.0)
        assertEquals(listOf<ExposureWindowWithRisk>(), riskyExposureWindows)
    }

    @Test
    fun `return risky exposure windows that are recent, after DCT opt-in date and above threshold`() {
        every { isolationStateMachine.readState() } returns Default()

        val exposureWindowsWithRisk = listOf(getExposureWindowWithRisk(calculatedRisk = 101.0))

        val riskyExposureWindows = filterRiskyExposureWindows(exposureWindowsWithRisk, 100.0)

        assertEquals(exposureWindowsWithRisk, riskyExposureWindows)
    }

    @Test
    fun `return risky exposure windows if exposure was exactly max duration of contact case days ago`() {
        every { isolationStateMachine.readState() } returns Default()

        val contactCaseExpiryDate = baseDate.minus(durationDays.contactCase.toLong(), ChronoUnit.DAYS)

        val exposureWindowsWithRisk = listOf(
            getExposureWindowWithRisk(
                calculatedRisk = 101.0,
                millisSinceEpoch = contactCaseExpiryDate.toEpochMilli()
            )
        )

        val riskyExposureWindows = filterRiskyExposureWindows(exposureWindowsWithRisk, 100.0)

        assertEquals(exposureWindowsWithRisk, riskyExposureWindows)
    }

    @Test
    fun `do not return risky exposure windows if exposure more than max duration of contact case days ago`() {
        every { isolationStateMachine.readState() } returns Default()

        val expiredContactDate = baseDate.minus(durationDays.contactCase + 1L, ChronoUnit.DAYS)

        val exposureWindowsWithRisk = listOf(
            getExposureWindowWithRisk(
                calculatedRisk = 101.0,
                millisSinceEpoch = expiredContactDate.toEpochMilli()
            )
        )

        val riskyExposureWindows = filterRiskyExposureWindows(exposureWindowsWithRisk, 100.0)

        assertEquals(listOf<ExposureWindowWithRisk>(), riskyExposureWindows)
    }

    @Test
    fun `do not return risky exposure windows if exposure was before DCT opt-in`() {
        setUpDefaultDueToDctOptIn()

        val oneDayBeforeDctOptIn = baseDate.minus(1, ChronoUnit.DAYS).toEpochMilli()

        val exposureWindowsWithRisk = listOf(
            getExposureWindowWithRisk(calculatedRisk = 101.0, millisSinceEpoch = oneDayBeforeDctOptIn)
        )

        val riskyExposureWindows = filterRiskyExposureWindows(exposureWindowsWithRisk, 100.0)

        assertEquals(listOf<ExposureWindowWithRisk>(), riskyExposureWindows)
    }

    @Test
    fun `return risky exposure windows if exposure was on same day as DCT opt-in`() {
        setUpDefaultDueToDctOptIn()

        val exposureWindowsWithRisk = listOf(
            getExposureWindowWithRisk(calculatedRisk = 101.0, millisSinceEpoch = baseDate.toEpochMilli())
        )

        val riskyExposureWindows = filterRiskyExposureWindows(exposureWindowsWithRisk, 100.0)

        assertEquals(exposureWindowsWithRisk, riskyExposureWindows)
    }

    @Test
    fun `return risky exposure windows if exposure was after DCT opt-in`() {
        setUpDefaultDueToDctOptIn()

        val oneDayAfterDctOptIn = baseDate.plus(1, ChronoUnit.DAYS).toEpochMilli()

        val exposureWindowsWithRisk = listOf(
            getExposureWindowWithRisk(calculatedRisk = 101.0, millisSinceEpoch = oneDayAfterDctOptIn)
        )

        val riskyExposureWindows = filterRiskyExposureWindows(exposureWindowsWithRisk, 100.0)

        assertEquals(exposureWindowsWithRisk, riskyExposureWindows)
    }

    @Test
    fun `return risky exposure windows if currently in isolation`() {
        every { isolationStateMachine.readState() } returns contactCaseIsolation

        val exposureWindowsWithRisk = listOf(getExposureWindowWithRisk(calculatedRisk = 101.0))

        val riskyExposureWindows = filterRiskyExposureWindows(exposureWindowsWithRisk, 100.0)

        assertEquals(exposureWindowsWithRisk, riskyExposureWindows)
    }

    @Test
    fun `return risky exposure windows if calculated risk is equal to risk threshold`() {
        every { isolationStateMachine.readState() } returns Default()

        val exposureWindowsWithRisk = listOf(getExposureWindowWithRisk(calculatedRisk = 100.0))

        val riskyExposureWindows = filterRiskyExposureWindows(exposureWindowsWithRisk, 100.0)

        assertEquals(exposureWindowsWithRisk, riskyExposureWindows)
    }

    @Test
    fun `do not return risky exposure windows if calculated risk is below risk threshold`() {
        every { isolationStateMachine.readState() } returns Default()

        val exposureWindowsWithRisk = listOf(getExposureWindowWithRisk(calculatedRisk = 99.9))

        val riskyExposureWindows = filterRiskyExposureWindows(exposureWindowsWithRisk, 100.0)

        assertEquals(listOf<ExposureWindowWithRisk>(), riskyExposureWindows)
    }

    private fun setUpDefaultDueToDctOptIn() {
        val defaultDueToDctOptIn = Default(
            previousIsolation = contactCaseIsolation.copy(
                contactCase = contactCaseIsolation.contactCase!!.copy(
                    dailyContactTestingOptInDate = baseDate.atOffset(ZoneOffset.UTC).toLocalDate()
                )
            )
        )

        every { isolationStateMachine.readState() } returns defaultDueToDctOptIn
    }

    private val contactCaseIsolation = Isolation(
        isolationStart = Instant.now(),
        isolationConfiguration = durationDays,
        contactCase = ContactCase(
            startDate = Instant.parse("2020-05-19T12:00:00Z"),
            notificationDate = null,
            expiryDate = LocalDate.now().plusDays(5),
            dailyContactTestingOptInDate = null
        )
    )
}
