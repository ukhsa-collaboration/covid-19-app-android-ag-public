package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification.InitialCircuitBreakerResult
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification.InitialCircuitBreakerResult.Yes
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.DayRisk
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.ExposureWindowRiskManager
import uk.nhs.nhsx.covid19.android.app.remote.ExposureCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureCircuitBreakerResponse
import java.time.Clock
import kotlin.test.assertEquals

class HandleInitialExposureNotificationTest {
    private val exposureCircuitBreakerApi = mockk<ExposureCircuitBreakerApi>()
    private val exposureWindowRiskManager = mockk<ExposureWindowRiskManager>()

    private val testSubject = HandleInitialExposureNotification(
        exposureCircuitBreakerApi,
        exposureWindowRiskManager,
        Clock.systemUTC()
    )

    @Test
    fun `circuit breaker responses yes will return yes`() = runBlocking {

        val exposureDateTimestamp = 0L
        coEvery { exposureWindowRiskManager.getRisk(any()) } returns DayRisk(
            exposureDateTimestamp,
            1000.00,
            2
        )

        coEvery {
            exposureCircuitBreakerApi.submitExposureInfo(any())
        } returns ExposureCircuitBreakerResponse(approvalToken = "token", approval = "yes")

        val result = testSubject.invoke("token")

        assertEquals(
            Success(Yes(exposureDateTimestamp)),
            result
        )
    }

    @Test
    fun `circuit breaker responses no will not change the status state`() = runBlocking {

        coEvery { exposureWindowRiskManager.getRisk(any()) } returns DayRisk(
            0L,
            1000.00,
            2
        )

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
        val exposureDateTimestamp = 0L
        coEvery { exposureWindowRiskManager.getRisk(any()) } returns DayRisk(
            exposureDateTimestamp,
            1000.00,
            2
        )

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

        val testException = Exception()
        coEvery { exposureWindowRiskManager.getRisk(any()) } returns DayRisk(
            0L,
            1000.00,
            2
        )

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
    fun `when maximum score is below threshold returns skipped without making any network calls`() =
        runBlocking {
            coEvery { exposureWindowRiskManager.getRisk(any()) } returns null
            coVerify(exactly = 0) { exposureCircuitBreakerApi.submitExposureInfo(any()) }

            val result = testSubject.invoke("approval_token")

            assertEquals(
                Success(InitialCircuitBreakerResult.Skipped),
                result
            )
        }
}
