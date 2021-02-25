package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import com.google.android.gms.nearby.exposurenotification.ExposureWindow.Builder
import com.google.android.gms.nearby.exposurenotification.Infectiousness
import com.google.android.gms.nearby.exposurenotification.ReportType
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ExposureWindowsMatched
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.V2RiskCalculation
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.riskscore.RiskScoreCalculator
import uk.nhs.riskscore.RiskScoreCalculatorConfiguration
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertNull
import com.google.android.gms.nearby.exposurenotification.ScanInstance as GoogleScanInstance
import uk.nhs.riskscore.ScanInstance as NHSScanInstance

@ExperimentalCoroutinesApi
class ExposureWindowRiskCalculatorTest {
    private val isolationConfigurationProvider = mockk<IsolationConfigurationProvider>()

    private val baseDate = Instant.parse("2020-07-20T00:00:00Z")
    private val clock = Clock.fixed(baseDate, ZoneOffset.UTC)
    private val riskScoreCalculatorProvider = mockk<RiskScoreCalculatorProvider>()
    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxed = true)

    private val riskScoreCalculator = mockk<RiskScoreCalculator>()

    private val someRiskScoreCalculatorConfig = mockk<RiskScoreCalculatorConfiguration>()
    private val someRiskCalculation = V2RiskCalculation(
        daysSinceOnsetToInfectiousness = listOf(),
        infectiousnessWeights = listOf(1.0, 1.0, 1.0),
        reportTypeWhenMissing = 0,
        riskThreshold = 0.0
    )
    private val someScanInstance = getGoogleScanInstance(50)
    private val expectedRiskScore = 100.0
    private val testScope = TestCoroutineScope()
    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxed = true)

    private val riskCalculator = ExposureWindowRiskCalculator(
        clock,
        isolationConfigurationProvider,
        riskScoreCalculatorProvider,
        analyticsEventProcessor,
        testScope,
        isolationStateMachine
    )

    @Before
    fun setup() {
        every { riskScoreCalculatorProvider.riskScoreCalculator(any()) } returns riskScoreCalculator
        every { riskScoreCalculatorProvider.getRiskCalculationVersion() } returns 2
        every { riskScoreCalculator.calculate(any()) } returns expectedRiskScore
        every { isolationConfigurationProvider.durationDays } returns DurationDays()
        every { isolationStateMachine.readState() } returns Default()
    }

    @Test
    fun `calls risk score calculator with mapped scan instances for each exposure window`() =
        testScope.runBlockingTest {
            val expectedAttenuationValue = 55
            val expectedSecondsSinceLastScan = 180
            val scanInstances = listOf(
                getGoogleScanInstance(expectedAttenuationValue, expectedSecondsSinceLastScan)
            )
            val exposureWindows = listOf(getExposureWindow(scanInstances))

            riskCalculator(exposureWindows, someRiskCalculation, someRiskScoreCalculatorConfig)

            val expectedInstances = listOf(
                NHSScanInstance(expectedAttenuationValue, expectedSecondsSinceLastScan)
            )
            coVerify(exactly = 1) { analyticsEventProcessor.track(ExposureWindowsMatched(1, 0)) }
            verify { riskScoreCalculator.calculate(expectedInstances) }
        }

    @Test
    fun `drops any scan instances with seconds since last exposure of 0`() = testScope.runBlockingTest {
        val scanInstances = listOf(
            getGoogleScanInstance(50, 0)
        )
        val exposureWindows = listOf(getExposureWindow(scanInstances))

        riskCalculator(exposureWindows, someRiskCalculation, someRiskScoreCalculatorConfig)

        verify { riskScoreCalculator.calculate(listOf()) }
    }

    @Test
    fun `returns null if no risk score exceeds threshold`() = testScope.runBlockingTest {
        val riskCalculation = someRiskCalculation.copy(riskThreshold = 900.0)
        val exposureWindows = listOf(getExposureWindow(listOf()))
        every { riskScoreCalculator.calculate(any()) } returns 2.0

        val risk = riskCalculator(exposureWindows, riskCalculation, someRiskScoreCalculatorConfig)

        coVerify(exactly = 1) { analyticsEventProcessor.track(ExposureWindowsMatched(0, 1)) }

        assertNull(risk)
    }

    @Test
    fun `returns day risk when risk score exceeds threshold`() = testScope.runBlockingTest {
        val riskCalculation = someRiskCalculation.copy(riskThreshold = 50.0)
        val exposureWindows = listOf(getExposureWindow(scanInstances = listOf()))

        val risk =
            riskCalculator(exposureWindows, riskCalculation, someRiskScoreCalculatorConfig)

        val expectedRisk = DayRisk(
            baseDate.toEpochMilli(),
            expectedRiskScore * 60,
            2,
            1,
            exposureWindows
        )

        coVerify(exactly = 1) { analyticsEventProcessor.track(ExposureWindowsMatched(1, 0)) }

        assertEquals(expectedRisk, risk)
    }

    @Test
    fun `returns day risk when risk score exceeds threshold with only recent exposure window`() =
        testScope.runBlockingTest {
            val riskCalculation = someRiskCalculation.copy(riskThreshold = 50.0)
            val recentExposureWindow = getExposureWindow(scanInstances = listOf())
            val exposureWindows = listOf(
                recentExposureWindow,
                getExposureWindow(scanInstances = listOf(), millisSinceEpoch = 0L)
            )

            val risk =
                riskCalculator(exposureWindows, riskCalculation, someRiskScoreCalculatorConfig)

            val expectedRisk = DayRisk(
                baseDate.toEpochMilli(),
                expectedRiskScore * 60,
                2,
                1,
                listOf(recentExposureWindow)
            )

            coVerify(exactly = 1) { analyticsEventProcessor.track(ExposureWindowsMatched(1, 1)) }

            assertEquals(expectedRisk, risk)
        }

    @Test
    fun `returns risk for most recent day which exceeds threshold`() = testScope.runBlockingTest {
        val olderDate = todayMinusDays(3)
        val newerDate = todayMinusDays(2)
        val exposureWindows = listOf(
            getExposureWindow(
                millisSinceEpoch = olderDate.toStartOfDayEpochMillis()
            ),
            getExposureWindow(
                millisSinceEpoch = newerDate.toStartOfDayEpochMillis()
            )
        )

        val risk =
            riskCalculator(exposureWindows, someRiskCalculation, someRiskScoreCalculatorConfig)

        val expectedDateMillis = newerDate.toStartOfDayEpochMillis()
        val expectedRisk =
            DayRisk(
                startOfDayMillis = expectedDateMillis,
                calculatedRisk = expectedRiskScore * 60,
                riskCalculationVersion = 2,
                matchedKeyCount = 1,
                exposureWindows = exposureWindows
            )

        coVerify(exactly = 1) { analyticsEventProcessor.track(ExposureWindowsMatched(2, 0)) }

        assertEquals(expectedRisk, risk)
    }

    private fun todayMinusDays(days: Long) =
        baseDate.atZone(ZoneOffset.UTC).minusDays(days).toLocalDate()

    @Test
    fun `returns risk item with highest score when there are multiple from the same day`() = testScope.runBlockingTest {
        val millisSinceEpoch = baseDate.toEpochMilli()
        val higherRiskScanInstance = getGoogleScanInstance(30)
        val lowerRiskScanInstance = getGoogleScanInstance(50)
        val exposureWindows = listOf(
            getExposureWindow(listOf(lowerRiskScanInstance), millisSinceEpoch),
            getExposureWindow(listOf(higherRiskScanInstance), millisSinceEpoch)
        )
        val higherRiskScore = 200.0
        val lowerRiskScore = 100.0
        higherRiskScanInstance.returnsRiskScoreOf(higherRiskScore)
        lowerRiskScanInstance.returnsRiskScoreOf(lowerRiskScore)

        val risk =
            riskCalculator(exposureWindows, someRiskCalculation, someRiskScoreCalculatorConfig)

        val expectedRisk =
            DayRisk(
                startOfDayMillis = millisSinceEpoch,
                calculatedRisk = higherRiskScore * 60,
                riskCalculationVersion = 2,
                matchedKeyCount = 1,
                exposureWindows = exposureWindows
            )
        coVerify(exactly = 1) { analyticsEventProcessor.track(ExposureWindowsMatched(2, 0)) }
        assertEquals(expectedRisk, risk)
    }

    @Test
    fun `multiplies calculated risk score by infectiousness factor`() = testScope.runBlockingTest {
        val expectedInfectiousness = Infectiousness.STANDARD
        val exposureWindows = listOf(
            getExposureWindow(listOf(someScanInstance), infectiousness = expectedInfectiousness)
        )
        val expectedInfectiousnessFactor = 0.4
        val riskCalculation = someRiskCalculation.copy(
            infectiousnessWeights = listOf(0.0, expectedInfectiousnessFactor, 1.0)
        )

        val risk =
            riskCalculator(exposureWindows, riskCalculation, someRiskScoreCalculatorConfig)

        val expectedRiskScore = expectedInfectiousnessFactor * expectedRiskScore * 60

        coVerify(exactly = 1) { analyticsEventProcessor.track(ExposureWindowsMatched(1, 0)) }

        assertEquals(expectedRiskScore, risk?.calculatedRisk)
    }

    @Test
    fun `disregards exposure from longer than one isolation period ago`() = testScope.runBlockingTest {
        val oldDate = baseDate.atZone(ZoneOffset.UTC).toLocalDate().minusDays(11)
        val exposureWindows = listOf(
            getExposureWindow(millisSinceEpoch = oldDate.toStartOfDayEpochMillis())
        )
        every { isolationConfigurationProvider.durationDays } returns DurationDays(contactCase = 10)

        val risk =
            riskCalculator(exposureWindows, someRiskCalculation, someRiskScoreCalculatorConfig)

        coVerify(exactly = 1) { analyticsEventProcessor.track(ExposureWindowsMatched(0, 1)) }

        assertNull(risk)
    }

    @Test
    fun `disregards exposure from before user opted in to daily contact testing`() = testScope.runBlockingTest {
        every { isolationStateMachine.readState() } returns Default(previousIsolation = contactCaseOnlyIsolation)

        val exposureDateInMillis =
            contactCaseOnlyIsolation.contactCase!!.dailyContactTestingOptInDate!!.minusDays(1).toStartOfDayEpochMillis()

        val exposureWindows = listOf(getExposureWindow(millisSinceEpoch = exposureDateInMillis))

        val risk = riskCalculator(exposureWindows, someRiskCalculation, someRiskScoreCalculatorConfig)

        coVerify(exactly = 1) {
            analyticsEventProcessor.track(
                ExposureWindowsMatched(
                    totalRiskyExposures = 0,
                    totalNonRiskyExposures = 1
                )
            )
        }

        assertEquals(expected = null, risk)
    }

    @Test
    fun `returns risk item if exposure after or on the same day as user opted in to daily contact testing`() =
        testScope.runBlockingTest {
            every { isolationStateMachine.readState() } returns Default(previousIsolation = contactCaseOnlyIsolation)

            val riskCalculation = someRiskCalculation.copy(riskThreshold = 50.0)
            val exposureDateInMillis =
                contactCaseOnlyIsolation.contactCase!!.dailyContactTestingOptInDate!!.toStartOfDayEpochMillis()
            val exposureWindows = listOf(getExposureWindow(millisSinceEpoch = exposureDateInMillis))

            val risk = riskCalculator(exposureWindows, riskCalculation, someRiskScoreCalculatorConfig)

            val expectedRisk = DayRisk(
                exposureDateInMillis,
                expectedRiskScore * 60,
                2,
                1,
                exposureWindows
            )

            coVerify(exactly = 1) {
                analyticsEventProcessor.track(
                    ExposureWindowsMatched(
                        totalRiskyExposures = 1,
                        totalNonRiskyExposures = 0
                    )
                )
            }

            assertEquals(expectedRisk, risk)
        }

    private val contactCaseOnlyIsolation = Isolation(
        isolationStart = baseDate.minus(3, ChronoUnit.DAYS),
        isolationConfiguration = DurationDays(),
        contactCase = ContactCase(
            startDate = baseDate.minus(3, ChronoUnit.DAYS),
            notificationDate = baseDate.minus(3, ChronoUnit.DAYS),
            expiryDate = baseDate.minus(2, ChronoUnit.DAYS).atOffset(ZoneOffset.UTC).toLocalDate(),
            dailyContactTestingOptInDate = baseDate.minus(2, ChronoUnit.DAYS).atOffset(ZoneOffset.UTC).toLocalDate()
        )
    )

    private fun GoogleScanInstance.returnsRiskScoreOf(riskScore: Double) {
        val scanInstances = listOf(NHSScanInstance(minAttenuationDb, secondsSinceLastScan))
        every { riskScoreCalculator.calculate(scanInstances) } returns riskScore
    }

    private fun getExposureWindow(
        scanInstances: List<com.google.android.gms.nearby.exposurenotification.ScanInstance> = listOf(
            someScanInstance
        ),
        millisSinceEpoch: Long = baseDate.toEpochMilli(),
        infectiousness: Int = Infectiousness.HIGH
    ): ExposureWindow {
        return Builder().apply {
            setDateMillisSinceEpoch(millisSinceEpoch)
            setReportType(ReportType.CONFIRMED_TEST)
            setScanInstances(scanInstances)
            setInfectiousness(infectiousness)
        }.build()
    }

    private fun getGoogleScanInstance(
        minAttenuation: Int,
        secondsSinceLastScan: Int = 180,
        typicalAttenuation: Int = 68
    ) = GoogleScanInstance.Builder().apply {
        setMinAttenuationDb(minAttenuation)
        setSecondsSinceLastScan(secondsSinceLastScan)
        setTypicalAttenuationDb(typicalAttenuation)
    }.build()

    private fun LocalDate.toStartOfDayEpochMillis(): Long =
        atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
}
