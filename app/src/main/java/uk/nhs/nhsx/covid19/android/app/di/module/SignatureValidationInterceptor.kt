package uk.nhs.nhsx.covid19.android.app.di.module

import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import uk.nhs.covid19.config.SignatureKey
import uk.nhs.nhsx.covid19.android.app.remote.AnalyticsApi
import uk.nhs.nhsx.covid19.android.app.util.Base64Decoder
import java.io.IOException
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

class SignatureValidationInterceptor(
    private val base64Decoder: Base64Decoder,
    private val signatureKeys: List<SignatureKey>,
    private val dynamicContent: Boolean
) : Interceptor {

    override fun intercept(chain: Chain): Response {
        val request = chain.request()

        if (request.url.pathSegments == AnalyticsApi.PATH.split('/')) {
            return chain.proceed(request)
        }

        val response = chain.proceed(request)

        if (RuntimeBehavior.isFeatureEnabled(FeatureFlag.SIGNATURE_VALIDATION)) {
            val signatureHeader = response.header("x-amz-meta-signature")
                ?: throw IOException("Did not receive required signature header")
            val signatureHeaderParts =
                Regex("keyId=\"(.*)\",signature=\"(.*)\"")
                    .find(signatureHeader)
                    ?.groupValues ?: throw IOException("Could not parse signature header")
            val keyId = signatureHeaderParts[1]
            val signature = base64Decoder.decodeToBytes(signatureHeaderParts[2])

            val publicKeyString = signatureKeys.firstOrNull { it.id == keyId }
                ?.pemRepresentation ?: throw IOException("Unknown keyId received")

            val undecoratedString = publicKeyString
                .split("\n")
                .filter { !(it.isEmpty() || it.startsWith("-----")) }
                .joinToString("")
            val publicKeyBytes: ByteArray = base64Decoder.decodeToBytes(undecoratedString)

            val keySpec = X509EncodedKeySpec(publicKeyBytes)
            val keyFactory = KeyFactory.getInstance("EC")
            val key = keyFactory.generatePublic(keySpec)
            val responseBody = response.body
            val message = responseBody?.bytes()

            val dateHeader = response.header("x-amz-meta-signature-date")
                ?: throw IOException("Did not receive required date header: ${request.url}")

            val s = Signature.getInstance("SHA256withECDSA")
                .apply {
                    initVerify(key)
                    when (dynamicContent) {
                        true -> update(
                            getDynamicContentSignatureData(
                                request = request,
                                dateHeader = dateHeader,
                                message = message
                            )
                        )
                        false -> update(
                            getStaticContentSignatureData(
                                dateHeader = dateHeader,
                                message = message
                            )
                        )
                    }
                }

            val valid: Boolean = s.verify(signature)

            if (!valid) {
                throw IOException("Signature validation failed for request: ${request.url.encodedPath}")
            }

            return response.newBuilder()
                .body(message?.toResponseBody()) // Needed because the responseBody can only be read once
                .build()
        }

        return response
    }

    private fun getDynamicContentSignatureData(
        request: Request,
        dateHeader: String,
        message: ByteArray?
    ): ByteArray {
        val requestIdHeader = request.header(HEADER_REQUEST_ID)
            ?: throw IOException("Did not provide required request id header")
        val method = request.method
        val path = request.url.encodedPath

        var data = "$requestIdHeader:$method:$path:$dateHeader:".toByteArray()
        message?.let {
            data = data.plus(it)
        }

        return data
    }

    private fun getStaticContentSignatureData(
        dateHeader: String,
        message: ByteArray?
    ): ByteArray {
        var data = "$dateHeader:".toByteArray()
        message?.let {
            data = data.plus(it)
        }

        return data
    }

    companion object {
        const val HEADER_REQUEST_ID = "request-id"
    }
}
