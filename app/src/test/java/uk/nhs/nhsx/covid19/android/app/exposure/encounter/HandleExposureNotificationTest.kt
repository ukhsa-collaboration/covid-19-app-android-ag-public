package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.work.ListenableWorker.Result
import com.google.android.gms.nearby.exposurenotification.ExposureInformation
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTasks
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.remote.ExposureCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.ExposureConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureCircuitBreakerResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureConfigurationResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureNotification
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskCalculation
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnExposedNotification
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import java.time.Instant
import kotlin.test.assertEquals

class HandleExposureNotificationTest {

    private val exposureNotificationApi = mockk<ExposureNotificationApi>()
    private val stateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val periodicTasks = mockk<PeriodicTasks>(relaxed = true)
    private val exposureCircuitBreakerApi = mockk<ExposureCircuitBreakerApi>()
    private val configurationApi = mockk<ExposureConfigurationApi>()
    private val riskCalculator = mockk<RiskCalculator>()

    private val testSubject = HandleExposureNotification(
        exposureNotificationApi,
        stateMachine,
        periodicTasks,
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
    fun `circuit breaker responses yes will change the status state`() = runBlocking {
        every { stateMachine.readState() } returns Default()

        val configuration = getConfigurationWithThreshold(threshold = 900)
        coEvery { configurationApi.getExposureConfiguration() } returns configuration

        every { riskCalculator.invoke(any(), configuration.riskCalculation) } returns (0L to 1000.00)

        coEvery {
            exposureCircuitBreakerApi.submitExposureInfo(any())
        } returns ExposureCircuitBreakerResponse(approvalToken = "token", approval = "yes")

        val result = testSubject.doWork("token")

        verify { stateMachine.processEvent(OnExposedNotification(Instant.ofEpochMilli(0L))) }

        assertEquals(Result.success(), result)
    }

    @Test
    fun `circuit breaker responses no will not change the status state`() = runBlocking {

        val configuration = getConfigurationWithThreshold(threshold = 900)

        coEvery { configurationApi.getExposureConfiguration() } returns configuration

        every { riskCalculator.invoke(any(), configuration.riskCalculation) } returns (0L to 1000.00)

        every { stateMachine.readState() } returns Default()

        coEvery {
            exposureCircuitBreakerApi.submitExposureInfo(any())
        } returns ExposureCircuitBreakerResponse(approvalToken = "token", approval = "no")

        val result = testSubject.doWork("")

        verify(exactly = 0) { stateMachine.processEvent(OnExposedNotification(Instant.ofEpochMilli(0L))) }

        assertEquals(Result.success(), result)
    }

    @Test
    fun `circuit breaker responses pending will trigger circuit breaker polling`() = runBlocking {
        every { stateMachine.readState() } returns Default()

        val configuration = getConfigurationWithThreshold(threshold = 900)

        coEvery { configurationApi.getExposureConfiguration() } returns configuration

        every { riskCalculator.invoke(any(), configuration.riskCalculation) } returns (0L to 1000.00)

        coEvery {
            exposureCircuitBreakerApi.submitExposureInfo(any())
        } returns ExposureCircuitBreakerResponse(approvalToken = "token", approval = "pending")

        val result = testSubject.doWork("")

        verify(exactly = 1) { periodicTasks.scheduleExposureCircuitBreakerPolling("token", 0L) }

        assertEquals(Result.success(), result)
    }

    @Test
    fun `on network error will retry`() = runBlocking {
        every { stateMachine.readState() } returns Default()

        val configuration = getConfigurationWithThreshold(threshold = 900)

        coEvery { configurationApi.getExposureConfiguration() } returns configuration

        every { riskCalculator.invoke(any(), configuration.riskCalculation) } returns (0L to 1000.00)

        coEvery {
            exposureCircuitBreakerApi.submitExposureInfo(any())
        } throws Exception()

        val result = testSubject.doWork("")

        assertEquals(Result.retry(), result)
    }

    @Test
    fun `when maximum score is below threshold returns success without making any network calls`() =
        runBlocking {
            every { stateMachine.readState() } returns Default()
            val configuration = getConfigurationWithThreshold(threshold = 1000)

            coEvery { configurationApi.getExposureConfiguration() } returns configuration

            every { riskCalculator.invoke(any(), configuration.riskCalculation) } returns null

            coVerify(exactly = 0) { configurationApi.getExposureConfiguration() }
            coVerify(exactly = 0) { exposureCircuitBreakerApi.submitExposureInfo(any()) }

            val result = testSubject.doWork("approval_token")

            assertEquals(Result.success(), result)
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
