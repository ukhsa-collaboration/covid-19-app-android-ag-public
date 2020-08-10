package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.work.ListenableWorker.Result
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.ExposureCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureCircuitBreakerPollingResponse
import uk.nhs.nhsx.covid19.android.app.state.OnExposedNotification
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.Instant
import kotlin.test.assertEquals

class ExposureCircuitBreakerPollingTest {

    private val exposureCircuitBreakerApi = mockk<ExposureCircuitBreakerApi>()
    private val stateMachine = mockk<IsolationStateMachine>(relaxed = true)

    private val testSubject = ExposureCircuitBreakerPolling(
        exposureCircuitBreakerApi,
        stateMachine
    )

    private val onSetTimeSinceEpoch = Instant.now().toEpochMilli()

    @Test
    fun `on approval response yes will update the state`() = runBlocking {

        coEvery { exposureCircuitBreakerApi.getExposureCircuitBreakerResolution(approvalToken = "token") } returns ExposureCircuitBreakerPollingResponse(
            "yes"
        )

        val result = testSubject.doWork("token", onSetTimeSinceEpoch)

        verify {
            stateMachine.processEvent(
                OnExposedNotification(
                    Instant.ofEpochMilli(
                        onSetTimeSinceEpoch
                    )
                )
            )
        }

        assertEquals(Result.success(), result)
    }

    @Test
    fun `on approval response pending  will retry`() = runBlocking {

        coEvery { exposureCircuitBreakerApi.getExposureCircuitBreakerResolution(approvalToken = "token") } returns ExposureCircuitBreakerPollingResponse(
            "pending"
        )

        val result = testSubject.doWork("token", onSetTimeSinceEpoch)

        assertEquals(Result.retry(), result)
    }

    @Test
    fun `on approval response no will return success`() = runBlocking {

        coEvery { exposureCircuitBreakerApi.getExposureCircuitBreakerResolution(approvalToken = "token") } returns ExposureCircuitBreakerPollingResponse(
            "no"
        )

        val result = testSubject.doWork("token", onSetTimeSinceEpoch)

        assertEquals(Result.success(), result)
    }

    @Test
    fun `on exception will retry`() = runBlocking {

        coEvery { exposureCircuitBreakerApi.getExposureCircuitBreakerResolution(approvalToken = "token") } throws Exception()

        val result = testSubject.doWork("token", onSetTimeSinceEpoch)

        assertEquals(Result.retry(), result)
    }
}
