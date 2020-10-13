package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandlePollingExposureNotification.PollingCircuitBreakerResult
import uk.nhs.nhsx.covid19.android.app.remote.ExposureCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureCircuitBreakerPollingResponse
import kotlin.test.assertEquals

class HandlePollingExposureNotificationTest {

    private val exposureCircuitBreakerApi = mockk<ExposureCircuitBreakerApi>()

    private val testSubject = HandlePollingExposureNotification(
        exposureCircuitBreakerApi
    )

    @Test
    fun `on approval response yes will return yes`() = runBlocking {

        coEvery { exposureCircuitBreakerApi.getExposureCircuitBreakerResolution(approvalToken = "token") } returns ExposureCircuitBreakerPollingResponse(
            "yes"
        )

        val result = testSubject.invoke("token")

        assertEquals(
            Success(PollingCircuitBreakerResult.Yes),
            result
        )
    }

    @Test
    fun `on approval response pending  will return pending`() = runBlocking {

        coEvery { exposureCircuitBreakerApi.getExposureCircuitBreakerResolution(approvalToken = "token") } returns ExposureCircuitBreakerPollingResponse(
            "pending"
        )

        val result = testSubject.invoke("token")

        assertEquals(
            Success(PollingCircuitBreakerResult.Pending),
            result
        )
    }

    @Test
    fun `on approval response no will return no`() = runBlocking {

        coEvery { exposureCircuitBreakerApi.getExposureCircuitBreakerResolution(approvalToken = "token") } returns ExposureCircuitBreakerPollingResponse(
            "no"
        )

        val result = testSubject.invoke("token")

        assertEquals(
            Success(PollingCircuitBreakerResult.No),
            result
        )
    }

    @Test
    fun `on exception will return failure`() = runBlocking {

        val testException = Exception()
        coEvery { exposureCircuitBreakerApi.getExposureCircuitBreakerResolution(approvalToken = "token") } throws testException

        val result = testSubject.invoke("token")

        assertEquals(
            Failure(testException),
            result
        )
    }
}
