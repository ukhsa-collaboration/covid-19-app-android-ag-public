package uk.nhs.nhsx.covid19.android.app.remote

import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import okhttp3.Interceptor.Chain
import okhttp3.Request
import okhttp3.Response
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.AppInfo
import kotlin.test.assertEquals

class UserAgentInterceptorTest {
    private val appInfo = AppInfo(
        osVersion = 27,
        buildNumber = 88,
        shortVersionName = "3.10"
    )
    private val testSubject = UserAgentInterceptor(appInfo)
    private val mockChain = mockk<Chain>(relaxed = true)
    private val mockRequest = mockk<Request>(relaxed = true)
    private val mockResponse = mockk<Response>(relaxed = true)
    private val mockUpdatedRequest = mockk<Request>(relaxed = true)
    private val mockRequestBuilder = mockk<Request.Builder>(relaxed = true)

    @Test
    fun `adds user agent header`() {
        every { mockChain.request() } returns mockRequest
        every { mockChain.proceed(mockUpdatedRequest) } returns mockResponse
        every { mockRequest.newBuilder() } returns mockRequestBuilder
        every { mockRequestBuilder.addHeader(any(), any()) } returns mockRequestBuilder
        every { mockRequestBuilder.build() } returns mockUpdatedRequest

        val response = testSubject.intercept(mockChain)
        assertEquals(mockResponse, response)

        verifyOrder {
            mockRequestBuilder.addHeader("User-Agent", "p=Android,o=27,v=3.10,b=88")
            mockRequestBuilder.build()
            mockChain.proceed(mockUpdatedRequest)
        }
    }
}
