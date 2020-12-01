package uk.nhs.nhsx.covid19.android.app.network

import okhttp3.Interceptor
import okhttp3.Response
import uk.nhs.nhsx.covid19.android.app.util.AndroidBase64Encoder
import uk.nhs.nhsx.covid19.android.app.util.Base64Encoder
import java.security.SecureRandom
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

class TrafficLengthObfuscationInterceptor(
    private val random: SecureRandom,
    private val base64Encoder: Base64Encoder
) : Interceptor {

    @Inject
    constructor() : this (random = SecureRandom(), base64Encoder = AndroidBase64Encoder())

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val bodyContentSize = request.body?.contentLength() ?: 0
        val targetMessageSize = MIN_MESSAGE_SIZE + random.nextInt(MAX_PADDING_SIZE)
        var headerPaddingSize = max(0, targetMessageSize - bodyContentSize)
        val newRequest = request.newBuilder()
        var counter = 1

        while (headerPaddingSize > 0) {
            val headerSize = min(headerPaddingSize, MAX_HEADER_SIZE.toLong())
            val headerContent = ByteArray(headerSize.toInt())
            random.nextBytes(headerContent)
            val encodedBytes = base64Encoder.encodeUrl(headerContent)

            newRequest.addHeader(HEADER_NAME_PREFIX + counter, encodedBytes.substring(0, headerSize.toInt()))

            counter++
            headerPaddingSize -= headerSize
        }
        return chain.proceed(newRequest.build())
    }

    companion object {
        private const val MAX_PADDING_SIZE = 4000
        private const val MIN_MESSAGE_SIZE = 4000
        private const val MAX_HEADER_SIZE = 2000
        private const val HEADER_NAME_PREFIX = "X-Randomised-"
    }
}
