package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import com.google.android.gms.nearby.exposurenotification.DiagnosisKeysDataMapping
import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import com.google.android.gms.nearby.exposurenotification.Infectiousness
import com.google.android.gms.nearby.exposurenotification.ReportType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.remote.ExposureConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureConfigurationResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskScore
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskScore.InitialData
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskScore.ObservationType.log
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskScore.PowerLossParameters
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskScore.RssiParameters
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskScore.SmootherParameters
import uk.nhs.nhsx.covid19.android.app.remote.data.V2RiskCalculation
import kotlin.test.assertEquals
import kotlin.test.fail

class ExposureWindowRiskManagerTest {
    private val exposureNotificationApi = mockk<ExposureNotificationApi>(relaxed = true)
    private val exposureConfigurationApi = mockk<ExposureConfigurationApi>()
    private val riskCalculator = mockk<ExposureWindowRiskCalculator>()

    private val expectedRisk =
        RiskCalculationResult(
            DayRisk(startOfDayMillis = 0, calculatedRisk = 0.0, riskCalculationVersion = 2, matchedKeyCount = 1),
            listOf()
        )
    private val expectedExposureWindows = listOf(mockk<ExposureWindow>())
    private val v2RiskCalculation = V2RiskCalculation(
        daysSinceOnsetToInfectiousness = listOf(
            0, 0, 0, 0, 0, 0, 0, 0, 0,
            1, 1, 1,
            2, 2, 2, 2, 2, 2,
            1, 1, 1, 1, 1, 1,
            0, 0, 0, 0, 0
        ),
        infectiousnessWeights = listOf(0.0, 0.4, 1.0),
        reportTypeWhenMissing = 1,
        riskThreshold = 690.0
    )

    private lateinit var exposureWindowRiskManager: ExposureWindowRiskManager

    @Before
    fun setup() {
        coEvery { exposureNotificationApi.getExposureWindows() } returns expectedExposureWindows
        coEvery { exposureNotificationApi.getDiagnosisKeysDataMapping() } returns someOtherDataMapping()
        coEvery { exposureConfigurationApi.getExposureConfiguration() } returns mockConfig()
        every { riskCalculator(any(), any(), any()) } returns expectedRisk
        exposureWindowRiskManager =
            ExposureWindowRiskManager(
                exposureNotificationApi,
                exposureConfigurationApi,
                riskCalculator
            )
    }

    @Test
    fun `calls get exposure windows with token`() = runBlocking {
        exposureWindowRiskManager.getRisk()

        coVerify { exposureNotificationApi.getExposureWindows() }
    }

    @Test
    fun `calls get configuration`() = runBlocking {
        exposureWindowRiskManager.getRisk()

        coVerify { exposureConfigurationApi.getExposureConfiguration() }
    }

    @Test
    fun `calls risk calculator with exposure windows and threshold`() = runBlocking {
        val risk = exposureWindowRiskManager.getRisk()

        coVerify { riskCalculator(expectedExposureWindows, v2RiskCalculation, any()) }
        assertEquals(expectedRisk, risk)
    }

    @Test
    fun `sets diagnosis keys data mapping if current data mapping is different`() = runBlocking {
        val expectedDataMapping = getDataMapping()
        coEvery { exposureNotificationApi.getDiagnosisKeysDataMapping() } returns someOtherDataMapping()

        exposureWindowRiskManager.getRisk()

        coVerify { exposureNotificationApi.setDiagnosisKeysDataMapping(expectedDataMapping) }
    }

    @Test
    fun `does not set diagnosis keys data mapping if data mapping is unchanged`() = runBlocking {
        coEvery { exposureNotificationApi.getDiagnosisKeysDataMapping() } returns getDataMapping()

        exposureWindowRiskManager.getRisk()

        coVerify(exactly = 0) { exposureNotificationApi.setDiagnosisKeysDataMapping(any()) }
    }

    @Test
    fun `catches error when setDiagnosisKeysDataMapping rate limit exceeded`() {
        runBlocking {
            every { exposureNotificationApi.setDiagnosisKeysDataMapping(any()) } throws Exception("RATE_LIMIT_EXCEEDED")

            try {
                exposureWindowRiskManager.getRisk()
            } catch (e: Exception) {
                fail("Exception should have been caught")
            }
        }
    }

    private fun someOtherDataMapping() = DiagnosisKeysDataMapping.DiagnosisKeysDataMappingBuilder()
        .setDaysSinceOnsetToInfectiousness(mapOf())
        .setReportTypeWhenMissing(ReportType.SELF_REPORT)
        .setInfectiousnessWhenDaysSinceOnsetMissing(Infectiousness.NONE)
        .build()

    private fun getDataMapping(): DiagnosisKeysDataMapping =
        DiagnosisKeysDataMapping.DiagnosisKeysDataMappingBuilder()
            .setDaysSinceOnsetToInfectiousness(daysSinceOnsetToInfectiousness())
            .setInfectiousnessWhenDaysSinceOnsetMissing(Infectiousness.HIGH)
            .setReportTypeWhenMissing(ReportType.CONFIRMED_TEST)
            .build()

    private fun daysSinceOnsetToInfectiousness(): Map<Int, Int> = mutableMapOf<Int, Int>().apply {
        for (i in -14..14) {
            when (i) {
                in -5..-3 -> this[i] = Infectiousness.STANDARD
                in -2..3 -> this[i] = Infectiousness.HIGH
                in 4..9 -> this[i] = Infectiousness.STANDARD
                else -> this[i] = Infectiousness.NONE
            }
        }
    }

    private fun mockConfig() = ExposureConfigurationResponse(
        exposureNotification = mockk(),
        v2RiskCalculation = v2RiskCalculation,
        riskScore = RiskScore(
            sampleResolution = 0.0,
            expectedDistance = 0.0,
            minimumDistance = 0.0,
            rssiParameters = RssiParameters(
                weightCoefficient = 0.0,
                intercept = 0.0,
                covariance = 0.0
            ),
            powerLossParameters = PowerLossParameters(
                wavelength = 0.0,
                pathLossFactor = 0.0,
                refDeviceLoss = 0.0
            ),
            observationType = log,
            initialData = InitialData(
                mean = 0.0,
                covariance = 0.0
            ),
            smootherParameters = SmootherParameters(
                alpha = 0.0,
                beta = 0.0,
                kappa = 0.0
            )
        )
    )
}
