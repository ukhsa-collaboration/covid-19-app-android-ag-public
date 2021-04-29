package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.ExposureWindowUtils.Companion.getExposureWindow
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
import kotlin.test.assertTrue

class EvaluateIfConsideredRiskyTest {

    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val isolationConfigurationProvider = mockk<IsolationConfigurationProvider>()
    private val baseDate: Instant = Instant.parse("2020-07-20T00:00:00Z")
    private val clock = Clock.fixed(baseDate, ZoneOffset.UTC)
    private val evaluateIfConsideredRisky =
        EvaluateIfConsideredRisky(isolationConfigurationProvider, isolationStateMachine, clock)
    private val durationDays = DurationDays()

    @Before
    fun setUp() {
        every { isolationConfigurationProvider.durationDays } returns durationDays
    }

    @Test
    fun `returns false when no risky exposures passed`() {
        every { isolationStateMachine.readState() } returns Default()

        val exposureWindow = getExposureWindow(listOf())
        val isConsideredRisky = evaluateIfConsideredRisky(exposureWindow, 1.0, 2.0)

        assertFalse(isConsideredRisky)
    }

    @Test
    fun `return true when risky exposure windows are recent, after DCT opt-in date and above threshold`() {
        every { isolationStateMachine.readState() } returns Default()

        val exposureWindow = getExposureWindow()
        val isConsideredRisky = evaluateIfConsideredRisky(exposureWindow, 10.0, 2.0)

        assertTrue(isConsideredRisky)
    }

    @Test
    fun `return true when exposure was exactly max duration of contact case days ago`() {
        every { isolationStateMachine.readState() } returns Default()

        val contactCaseExpiryDate = baseDate.minus(durationDays.contactCase.toLong(), ChronoUnit.DAYS)
        val exposureWindow = getExposureWindow(millisSinceEpoch = contactCaseExpiryDate.toEpochMilli())
        val isConsideredRisky = evaluateIfConsideredRisky(exposureWindow, 10.0, 2.0)

        assertTrue(isConsideredRisky)
    }

    @Test
    fun `return false if exposure more than max duration of contact case days ago`() {
        every { isolationStateMachine.readState() } returns Default()

        val expiredContactDate = baseDate.minus(durationDays.contactCase + 1L, ChronoUnit.DAYS)
        val exposureWindow = getExposureWindow(millisSinceEpoch = expiredContactDate.toEpochMilli())
        val isConsideredRisky = evaluateIfConsideredRisky(exposureWindow, 10.0, 2.0)

        assertFalse(isConsideredRisky)
    }

    @Test
    fun `return false if exposure was before DCT opt-in`() {
        setUpDefaultDueToDctOptIn()

        val oneDayBeforeDctOptIn = baseDate.minus(1, ChronoUnit.DAYS).toEpochMilli()
        val exposureWindow = getExposureWindow(millisSinceEpoch = oneDayBeforeDctOptIn)
        val isConsideredRisky = evaluateIfConsideredRisky(exposureWindow, 10.0, 2.0)

        assertFalse(isConsideredRisky)
    }

    @Test
    fun `return true if exposure was on same day as DCT opt-in`() {
        setUpDefaultDueToDctOptIn()

        val exposureWindow = getExposureWindow()
        val isConsideredRisky = evaluateIfConsideredRisky(exposureWindow, 10.0, 2.0)

        assertTrue(isConsideredRisky)
    }

    @Test
    fun `return true if exposure was after DCT opt-in`() {
        setUpDefaultDueToDctOptIn()

        val oneDayAfterDctOptIn = baseDate.plus(1, ChronoUnit.DAYS).toEpochMilli()
        val exposureWindow = getExposureWindow(millisSinceEpoch = oneDayAfterDctOptIn)
        val isConsideredRisky = evaluateIfConsideredRisky(exposureWindow, 10.0, 2.0)

        assertTrue(isConsideredRisky)
    }

    @Test
    fun `return true if currently in isolation`() {
        every { isolationStateMachine.readState() } returns contactCaseIsolation

        val exposureWindow = getExposureWindow()
        val isConsideredRisky = evaluateIfConsideredRisky(exposureWindow, 10.0, 2.0)

        assertTrue(isConsideredRisky)
    }

    @Test
    fun `return true if calculated risk is equal to risk threshold`() {
        every { isolationStateMachine.readState() } returns Default()

        val exposureWindow = getExposureWindow()
        val isConsideredRisky = evaluateIfConsideredRisky(exposureWindow, 10.0, 10.0)

        assertTrue(isConsideredRisky)
    }

    @Test
    fun `return false if calculated risk is below risk threshold`() {
        every { isolationStateMachine.readState() } returns Default()

        val exposureWindow = getExposureWindow()
        val isConsideredRisky = evaluateIfConsideredRisky(exposureWindow, 2.0, 12.0)

        assertFalse(isConsideredRisky)
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
