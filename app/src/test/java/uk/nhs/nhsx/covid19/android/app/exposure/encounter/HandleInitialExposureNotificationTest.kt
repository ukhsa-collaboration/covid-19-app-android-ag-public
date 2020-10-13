package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import com.google.android.gms.nearby.exposurenotification.ExposureInformation
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification.InitialCircuitBreakerResult
import uk.nhs.nhsx.covid19.android.app.remote.ExposureCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.ExposureConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureCircuitBreakerResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureConfigurationResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureNotification
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskCalculation
import kotlin.test.assertEquals

class HandleInitialExposureNotificationTest {

    private val exposureNotificationApi = mockk<ExposureNotificationApi>()
    private val exposureCircuitBreakerApi = mockk<ExposureCircuitBreakerApi>()
    private val configurationApi = mockk<ExposureConfigurationApi>()
    private val riskCalculator = mockk<RiskCalculator>()

    private val testSubject = HandleInitialExposureNotification(
        exposureNotificationApi,
        exposureCircuitBreakerApi,
        configurationApi,
        riskCalculator
    )

    @Before
    fun setUp() {
        coEvery { exposureNotificationApi.getExposureInformation(any()) } returns listOf(
            ExposureInformation
                .ExposureInformationBuilder()
                .build()
        )
        coEvery { exposureNotificationApi.getExposureSummary(any()) } returns
            ExposureSummary.ExposureSummaryBuilder().build()
    }

    @Test
    fun `circuit breaker responses yes will return yes`() = runBlocking {

        val exposureDateTimestamp = 0L
        val configuration = getConfigurationWithThreshold(threshold = 900)
        coEvery { configurationApi.getExposureConfiguration() } returns configuration

        every { riskCalculator.invoke(any(), configuration.riskCalculation) } returns DayRisk(exposureDateTimestamp, 1000.00)

        coEvery {
            exposureCircuitBreakerApi.submitExposureInfo(any())
        } returns ExposureCircuitBreakerResponse(approvalToken = "token", approval = "yes")

        val result = testSubject.invoke("token")

        assertEquals(
            Success(InitialCircuitBreakerResult.Yes(exposureDateTimestamp)),
            result
        )
    }

    @Test
    fun `circuit breaker responses no will not change the status state`() = runBlocking {

        val configuration = getConfigurationWithThreshold(threshold = 900)

        coEvery { configurationApi.getExposureConfiguration() } returns configuration

        every { riskCalculator.invoke(any(), configuration.riskCalculation) } returns DayRisk(0L, 1000.00)

        coEvery {
            exposureCircuitBreakerApi.submitExposureInfo(any())
        } returns ExposureCircuitBreakerResponse(approvalToken = "token", approval = "no")

        val result = testSubject.invoke("")

        assertEquals(
            Success(InitialCircuitBreakerResult.No),
            result
        )
    }

    @Test
    fun `circuit breaker responses pending will return pending`() = runBlocking {
        val configuration = getConfigurationWithThreshold(threshold = 900)

        val exposureDateTimestamp = 0L
        coEvery { configurationApi.getExposureConfiguration() } returns configuration

        every { riskCalculator.invoke(any(), configuration.riskCalculation) } returns DayRisk(exposureDateTimestamp, 1000.00)

        coEvery {
            exposureCircuitBreakerApi.submitExposureInfo(any())
        } returns ExposureCircuitBreakerResponse(approvalToken = "token", approval = "pending")

        val result = testSubject.invoke("")

        assertEquals(
            Success(InitialCircuitBreakerResult.Pending(exposureDateTimestamp)),
            result
        )
    }

    @Test
    fun `on network error will return failure`() = runBlocking {
        val configuration = getConfigurationWithThreshold(threshold = 900)

        val testException = Exception()
        coEvery { configurationApi.getExposureConfiguration() } returns configuration

        every { riskCalculator.invoke(any(), configuration.riskCalculation) } returns DayRisk(0L, 1000.00)

        coEvery {
            exposureCircuitBreakerApi.submitExposureInfo(any())
        } throws testException

        val result = testSubject.invoke("")

        assertEquals(
            Failure(testException),
            result
        )
    }

    @Test
    fun `when maximum score is below threshold returns success without making any network calls`() =
        runBlocking {
            val configuration = getConfigurationWithThreshold(threshold = 1000)

            coEvery { configurationApi.getExposureConfiguration() } returns configuration

            every { riskCalculator.invoke(any(), configuration.riskCalculation) } returns null

            coVerify(exactly = 0) { configurationApi.getExposureConfiguration() }
            coVerify(exactly = 0) { exposureCircuitBreakerApi.submitExposureInfo(any()) }

            val result = testSubject.invoke("approval_token")

            assertEquals(
                Success(InitialCircuitBreakerResult.No),
                result
            )
        }

    private fun getConfigurationWithThreshold(threshold: Int = 900) =
        ExposureConfigurationResponse(
            exposureNotification = ExposureNotification(
                minimumRiskScore = 11,
                attenuationDurationThresholds = listOf(55, 63),
                attenuationLevelValues = listOf(0, 1, 1, 1, 1, 1, 1, 1),
                daysSinceLastExposureLevelValues = listOf(5, 5, 5, 5, 5, 5, 5, 5),
                durationLevelValues = listOf(0, 0, 0, 1, 1, 1, 1, 0),
                transmissionRiskLevelValues = listOf(1, 3, 4, 5, 6, 7, 8, 6),
                attenuationWeight = 50.0,
                daysSinceLastExposureWeight = 20,
                durationWeight = 50.0,
                transmissionRiskWeight = 50.0
            ),
            riskCalculation = RiskCalculation(
                durationBucketWeights = listOf(1.0, 0.5, 0.0),
                riskThreshold = threshold
            )
        )
}
