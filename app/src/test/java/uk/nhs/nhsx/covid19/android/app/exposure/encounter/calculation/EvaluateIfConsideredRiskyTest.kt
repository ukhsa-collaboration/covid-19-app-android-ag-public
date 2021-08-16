package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.ExposureWindowUtils.Companion.getExposureWindow
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.test.assertTrue

class EvaluateIfConsideredRiskyTest {

    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxUnitFun = true)
    private val isolationConfigurationProvider = mockk<IsolationConfigurationProvider>()
    private val baseDate: Instant = Instant.parse("2020-07-20T00:00:00Z")
    private val clock = Clock.fixed(baseDate, ZoneOffset.UTC)
    private val evaluateIfConsideredRisky =
        EvaluateIfConsideredRisky(isolationConfigurationProvider, isolationStateMachine, clock)
    private val durationDays = DurationDays()
    private val isolationHelper = IsolationHelper(clock)

    @Before
    fun setUp() {
        every { isolationConfigurationProvider.durationDays } returns durationDays
    }

    @Test
    fun `returns false when no risky exposures passed`() {
        every { isolationStateMachine.readState() } returns isolationHelper.neverInIsolation()

        val exposureWindow = getExposureWindow(listOf())
        val isConsideredRisky = evaluateIfConsideredRisky(exposureWindow, 1.0, 2.0)

        assertFalse(isConsideredRisky)
    }

    @Test
    fun `return true when risky exposure windows are recent, after contact isolation opt-out date and above threshold`() {
        every { isolationStateMachine.readState() } returns isolationHelper.neverInIsolation()

        val exposureWindow = getExposureWindow()
        val isConsideredRisky = evaluateIfConsideredRisky(exposureWindow, 10.0, 2.0)

        assertTrue(isConsideredRisky)
    }

    @Test
    fun `return true when exposure was exactly max duration of contact case days ago`() {
        every { isolationStateMachine.readState() } returns isolationHelper.neverInIsolation()

        val contactCaseExpiryDate = baseDate.minus(durationDays.contactCase.toLong(), ChronoUnit.DAYS)
        val exposureWindow = getExposureWindow(millisSinceEpoch = contactCaseExpiryDate.toEpochMilli())
        val isConsideredRisky = evaluateIfConsideredRisky(exposureWindow, 10.0, 2.0)

        assertTrue(isConsideredRisky)
    }

    @Test
    fun `return false if exposure more than max duration of contact case days ago`() {
        every { isolationStateMachine.readState() } returns isolationHelper.neverInIsolation()

        val expiredContactDate = baseDate.minus(durationDays.contactCase + 1L, ChronoUnit.DAYS)
        val exposureWindow = getExposureWindow(millisSinceEpoch = expiredContactDate.toEpochMilli())
        val isConsideredRisky = evaluateIfConsideredRisky(exposureWindow, 10.0, 2.0)

        assertFalse(isConsideredRisky)
    }

    @Test
    fun `return false if exposure was before contact isolation opt-out date`() {
        setUpExpiredContactCaseDueToContactIsolationOptOut()

        val oneDayBeforeContactIsolationOptOut = baseDate.minus(1, ChronoUnit.DAYS).toEpochMilli()
        val exposureWindow = getExposureWindow(millisSinceEpoch = oneDayBeforeContactIsolationOptOut)
        val isConsideredRisky = evaluateIfConsideredRisky(exposureWindow, 10.0, 2.0)

        assertFalse(isConsideredRisky)
    }

    @Test
    fun `return false if exposure was on same day as contact isolation opt-out date`() {
        setUpExpiredContactCaseDueToContactIsolationOptOut()

        val exposureWindow = getExposureWindow()
        val isConsideredRisky = evaluateIfConsideredRisky(exposureWindow, 10.0, 2.0)

        assertFalse(isConsideredRisky)
    }

    @Test
    fun `return true if exposure was after contact isolation opt-out date`() {
        setUpExpiredContactCaseDueToContactIsolationOptOut()

        val oneDayAfterContactIsolationOptOut = baseDate.plus(1, ChronoUnit.DAYS).toEpochMilli()
        val exposureWindow = getExposureWindow(millisSinceEpoch = oneDayAfterContactIsolationOptOut)
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
        every { isolationStateMachine.readState() } returns isolationHelper.neverInIsolation()

        val exposureWindow = getExposureWindow()
        val isConsideredRisky = evaluateIfConsideredRisky(exposureWindow, 10.0, 10.0)

        assertTrue(isConsideredRisky)
    }

    @Test
    fun `return false if calculated risk is below risk threshold`() {
        every { isolationStateMachine.readState() } returns isolationHelper.neverInIsolation()

        val exposureWindow = getExposureWindow()
        val isConsideredRisky = evaluateIfConsideredRisky(exposureWindow, 2.0, 12.0)

        assertFalse(isConsideredRisky)
    }

    private fun setUpExpiredContactCaseDueToContactIsolationOptOut() {
        val expiredContactCaseDueToContactIsolationOptOut = isolationHelper.contactCaseWithOptOutDate(
            optOutOfContactIsolation = baseDate.toLocalDate(ZoneOffset.UTC)
        ).asIsolation()

        every { isolationStateMachine.readState() } returns expiredContactCaseDueToContactIsolationOptOut
    }

    private val contactCaseIsolation = isolationHelper.contactCase().asIsolation()
}
