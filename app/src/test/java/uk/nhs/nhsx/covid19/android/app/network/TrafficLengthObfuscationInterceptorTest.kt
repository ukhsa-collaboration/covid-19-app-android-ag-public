package uk.nhs.nhsx.covid19.android.app.network

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.util.Base64Encoder
import java.security.SecureRandom
import java.util.Base64
import kotlin.test.assertEquals

class TrafficLengthObfuscationInterceptorTest {
    private val mockRandom = mockk<SecureRandom>()
    private val mockBase64Encoder = mockk<Base64Encoder>()
    private val testSubject = TrafficLengthObfuscationInterceptor(mockRandom, mockBase64Encoder)
    private val mockChain = mockk<Interceptor.Chain>(relaxed = true)
    private val mockRequest = mockk<Request>(relaxed = true)
    private val mockResponse = mockk<Response>(relaxed = true)
    private val mockUpdatedRequest = mockk<Request>(relaxed = true)
    private val mockRequestBuilder = mockk<Request.Builder>(relaxed = true)
    private val mockBody = mockk<RequestBody>()

    private val headerOneSlot = slot<String>()
    private val headerTwoSlot = slot<String>()

    @Before
    fun setUp() {
        every { mockChain.request() } returns mockRequest
        every { mockChain.proceed(mockUpdatedRequest) } returns mockResponse
        every { mockRandom.nextInt(any()) } returns 500
        every { mockRequest.newBuilder() } returns mockRequestBuilder
        every { mockRequest.body } returns mockBody
        every { mockRequestBuilder.addHeader(any(), any()) } returns mockRequestBuilder
        every { mockRequestBuilder.build() } returns mockUpdatedRequest

        val byteSlot = slot<ByteArray>()
        every {
            mockRandom.nextBytes(capture(byteSlot))
        } answers {
            byteSlot.captured.fill('a'.toByte())
        }

        val encoderSlot = slot<ByteArray>()
        every {
            mockBase64Encoder.encodeUrl(capture(encoderSlot))
        } answers {
            Base64.getEncoder().encodeToString(encoderSlot.captured)
        }
    }

    @Test
    fun `generates multiple headers`() {
        every { mockBody.contentLength() } returns 600

        val response = testSubject.intercept(mockChain)
        assertEquals(mockResponse, response)

        verifyOrder {
            mockRequestBuilder.addHeader("X-Randomised-1", capture(headerOneSlot))
            mockRequestBuilder.addHeader("X-Randomised-2", capture(headerTwoSlot))
            mockRequestBuilder.build()
            mockChain.proceed(mockUpdatedRequest)
        }
        assertEquals(2000, headerOneSlot.captured.length)
        assertEquals(1900, headerTwoSlot.captured.length)
    }

    @Test
    fun `generates one header`() {
        every { mockBody.contentLength() } returns 2500

        val response = testSubject.intercept(mockChain)
        assertEquals(mockResponse, response)

        verifyOrder {
            mockRequestBuilder.addHeader("X-Randomised-1", capture(headerOneSlot))
            mockRequestBuilder.build()
            mockChain.proceed(mockUpdatedRequest)
        }
        assertEquals(2000, headerOneSlot.captured.length)
    }

    @Test
    fun `generates no header when body is larger than target`() {
        every { mockBody.contentLength() } returns 5500

        val response = testSubject.intercept(mockChain)
        assertEquals(mockResponse, response)

        verify(exactly = 0) { mockRequestBuilder.addHeader(any(), any()) }
    }

    @Test
    fun `generates two headers when body is empty`() {
        every { mockBody.contentLength() } returns 0

        val response = testSubject.intercept(mockChain)
        assertEquals(mockResponse, response)

        verifyOrder {
            mockRequestBuilder.addHeader("X-Randomised-1", capture(headerOneSlot))
            mockRequestBuilder.addHeader("X-Randomised-2", capture(headerTwoSlot))
            mockRequestBuilder.build()
            mockChain.proceed(mockUpdatedRequest)
        }
        assertEquals(2000, headerOneSlot.captured.length)
        assertEquals(2000, headerTwoSlot.captured.length)
    }
}
