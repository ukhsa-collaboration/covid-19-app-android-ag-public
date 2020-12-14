package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import com.google.android.gms.nearby.exposurenotification.ExposureInformation
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.remote.ExposureConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureConfigurationResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskCalculation
import kotlin.test.assertEquals

class ExposureInformationRiskManagerTest {
    private val exposureNotificationApi = mockk<ExposureNotificationApi>()
    private val exposureConfigurationApi = mockk<ExposureConfigurationApi>()
    private val riskCalculator = mockk<RiskCalculator>()

    private val expectedExposureInfos = listOf(mockk<ExposureInformation>())
    private val expectedRiskCalculation = RiskCalculation(listOf(0.0), 1)

    private lateinit var exposureInformationRiskManager: ExposureInformationRiskManager

    private val expectedDayRisk =
        DayRisk(
            startOfDayMillis = 0,
            calculatedRisk = 0.0,
            riskCalculationVersion = 1
        )

    @Before
    fun setup() {
        exposureInformationRiskManager =
            ExposureInformationRiskManager(
                exposureNotificationApi,
                exposureConfigurationApi,
                riskCalculator
            )
        coEvery { exposureNotificationApi.getExposureInformation(any()) } returns expectedExposureInfos
        coEvery { exposureConfigurationApi.getExposureConfiguration() } returns mockConfig()
        coEvery { riskCalculator(any(), any()) } returns expectedDayRisk
    }

    @Test
    fun `calls get exposure information with token`() = runBlocking {
        val expectedToken = "some-token"

        exposureInformationRiskManager.getRisk(expectedToken)

        coVerify { exposureNotificationApi.getExposureInformation(expectedToken) }
    }

    @Test
    fun `calls get exposure config`() = runBlocking {
        exposureInformationRiskManager.getRisk("")

        coVerify { exposureConfigurationApi.getExposureConfiguration() }
    }

    @Test
    fun `calls risk calculator with exposure info and config`() = runBlocking {
        val risk = exposureInformationRiskManager.getRisk("")

        coVerify {
            riskCalculator(expectedExposureInfos, expectedRiskCalculation)
        }
        assertEquals(expectedDayRisk, risk)
    }

    private fun mockConfig() = ExposureConfigurationResponse(mockk(), expectedRiskCalculation, mockk(), mockk())
}
