package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.NO
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.PENDING
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.YES
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification.InitialCircuitBreakerResult.No
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification.InitialCircuitBreakerResult.Pending
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification.InitialCircuitBreakerResult.Yes
import uk.nhs.nhsx.covid19.android.app.remote.ExposureCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureCircuitBreakerRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureCircuitBreakerResponse
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals

class HandleInitialExposureNotificationTest {

    private val exposureCircuitBreakerApi = mockk<ExposureCircuitBreakerApi>()

    private val testSubject = HandleInitialExposureNotification(exposureCircuitBreakerApi, fixedClock)

    @Test
    fun `circuit breaker responses yes will return yes`() = runBlocking {
        coEvery { exposureCircuitBreakerApi.submitExposureInfo(expectedExposureCircuitBreakerRequest) } returns
            ExposureCircuitBreakerResponse(approvalToken = "token", approval = YES)

        val result = testSubject(exposureCircuitBreakerInfo)

        assertEquals(Success(Yes), result)
    }

    @Test
    fun `circuit breaker responses no will return no`() = runBlocking {
        coEvery { exposureCircuitBreakerApi.submitExposureInfo(expectedExposureCircuitBreakerRequest) } returns
            ExposureCircuitBreakerResponse(approvalToken = "token", approval = NO)

        val result = testSubject(exposureCircuitBreakerInfo)

        assertEquals(Success(No), result)
    }

    @Test
    fun `circuit breaker responses pending will return pending state with approval token`() = runBlocking {
        coEvery { exposureCircuitBreakerApi.submitExposureInfo(expectedExposureCircuitBreakerRequest) } returns
            ExposureCircuitBreakerResponse(approvalToken = "token", approval = PENDING)

        val result = testSubject(exposureCircuitBreakerInfo)

        assertEquals(Success(Pending("token")), result)
    }

    @Test
    fun `on network error will return failure`() = runBlocking {
        val testException = Exception()

        coEvery { exposureCircuitBreakerApi.submitExposureInfo(any()) } throws testException

        val result = testSubject(exposureCircuitBreakerInfo)

        assertEquals(Failure(testException), result)
    }

    companion object {
        private val fixedClock = Clock.fixed(Instant.parse("2020-12-24T20:00:00Z"), ZoneOffset.UTC)

        private val exposureCircuitBreakerInfo = ExposureCircuitBreakerInfo(
            maximumRiskScore = 10.0,
            startOfDayMillis = fixedClock.instant().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS)
                .toEpochMilli(),
            matchedKeyCount = 1,
            riskCalculationVersion = 2,
            exposureNotificationDate = 123L,
            approvalToken = null
        )

        private val expectedExposureCircuitBreakerRequest = ExposureCircuitBreakerRequest(
            maximumRiskScore = 10.0,
            matchedKeyCount = 1,
            riskCalculationVersion = 2,
            daysSinceLastExposure = 1
        )
    }
}
