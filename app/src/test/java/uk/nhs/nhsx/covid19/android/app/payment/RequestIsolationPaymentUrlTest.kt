package uk.nhs.nhsx.covid19.android.app.payment

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.remote.IsolationPaymentApi
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationPaymentUrlRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationPaymentUrlResponse
import java.time.Instant

class RequestIsolationPaymentUrlTest {

    private val isolationPaymentApi = mockk<IsolationPaymentApi>(relaxed = true)
    private val testSubject = RequestIsolationPaymentUrl(isolationPaymentApi)
    private val request = IsolationPaymentUrlRequest(
        ipcToken = "abc",
        riskyEncounterDate = Instant.parse("2014-12-14T10:14:59Z"),
        isolationPeriodEndDate = Instant.parse("2014-12-21T10:14:59Z")
    )

    @Test
    fun `should invoke api call`() = runBlocking {
        val response = IsolationPaymentUrlResponse("http://web")

        coEvery { isolationPaymentApi.requestUrl(request) } returns response

        val result = testSubject.invoke(request)

        assertEquals(Result.Success(response), result)

        coVerify { isolationPaymentApi.requestUrl(request) }
    }

    @Test
    fun `when api call throws exception return error result`() = runBlocking {
        val response = Exception()

        coEvery { isolationPaymentApi.requestUrl(request) } throws response

        val result = testSubject.invoke(request)

        assertEquals(Result.Failure(response), result)

        coVerify { isolationPaymentApi.requestUrl(request) }
    }
}
