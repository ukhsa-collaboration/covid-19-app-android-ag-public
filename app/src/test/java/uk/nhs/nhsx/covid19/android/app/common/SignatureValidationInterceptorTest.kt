package uk.nhs.nhsx.covid19.android.app.common

import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor.Chain
import okhttp3.Protocol.HTTP_1_1
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Response.Builder
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.covid19.config.SignatureKey
import uk.nhs.nhsx.covid19.android.app.di.module.SignatureValidationInterceptor
import uk.nhs.nhsx.covid19.android.app.remote.AnalyticsApi
import uk.nhs.nhsx.covid19.android.app.utils.Java8Base64Decoder
import java.io.IOException
import kotlin.test.assertEquals

class SignatureValidationInterceptorTest {

    //region test data

    private val dynamicSignatureKey = SignatureKey(
        id = "b4c27bf3-8a76-4d2b-b91c-2152e7710a57",
        pemRepresentation =
            """
        -----BEGIN PUBLIC KEY-----
        MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEqCkmIgEk4ojU2yIvejV0uzIroJJC
        SNVxLyo9xDFKiHsnqs7kFu418ynYKTZfx8y42ahiUwKN4Er4KAnFW1LtxA==
        -----END PUBLIC KEY-----
            """.trimIndent()
    )

    private val dynamicSampleData =
        """
        ewogICJleHBvc3VyZU5vdGlmaWNhdGlvbiI6IHsKICAgICJtaW5pbXVtUmlza1Njb3JlIjogMTEsCiAgICAiYXR0ZW51YXRpb25EdXJhdGlvblRocmVzaG9sZHMiOiBbCiAgICAgIDU1LAogICAgICA2MwogICAgXSwKICAgICJhdHRlbnVhdGlvbkxldmVsVmFsdWVzIjogWwogICAgICAwLAogICAgICAxLAogICAgICAxLAogICAgICAxLAogICAgICAxLAogICAgICAxLAogICAgICAxLAogICAgICAxCiAgICBdLAogICAgImRheXNTaW5jZUxhc3RFeHBvc3VyZUxldmVsVmFsdWVzIjogWwogICAgICA1LAogICAgICA1LAogICAgICA1LAogICAgICA1LAogICAgICA1LAogICAgICA1LAogICAgICA1LAogICAgICA1CiAgICBdLAogICAgImR1cmF0aW9uTGV2ZWxWYWx1ZXMiOiBbCiAgICAgIDAsCiAgICAgIDAsCiAgICAgIDAsCiAgICAgIDEsCiAgICAgIDEsCiAgICAgIDEsCiAgICAgIDEsCiAgICAgIDEKICAgIF0sCiAgICAidHJhbnNtaXNzaW9uUmlza0xldmVsVmFsdWVzIjogWwogICAgICAxLAogICAgICAyLAogICAgICAzLAogICAgICA0LAogICAgICA1LAogICAgICA2LAogICAgICA3LAogICAgICA4CiAgICBdLAogICAgImF0dGVudWF0aW9uV2VpZ2h0IjogNTAsCiAgICAiZGF5c1NpbmNlTGFzdEV4cG9zdXJlV2VpZ2h0IjogMjAsCiAgICAiZHVyYXRpb25XZWlnaHQiOiA1MCwKICAgICJ0cmFuc21pc3Npb25SaXNrV2VpZ2h0IjogNTAKICB9LAogICJyaXNrQ2FsY3VsYXRpb24iOiB7CiAgICAiZHVyYXRpb25CdWNrZXRXZWlnaHRzIjogWwogICAgICAxLAogICAgICAwLjUsCiAgICAgIDAKICAgIF0sCiAgICAicmlza1RocmVzaG9sZCI6IDkwMAogIH0KfQo=
        """.trimIndent()

    private val dynamicSignatureHeader =
        """
        keyId="b4c27bf3-8a76-4d2b-b91c-2152e7710a57",signature="MEUCIHK7mz1iDZBuMyVOGDniAmx1/UxJPaRcJDxKqkjOQSCiAiEAoaS41mzMtjqe8lz/skejpsvJ9IdgLi7VUr2U8tla5vY="
        """.trimIndent()

    private val dynamicSignatureDateHeader =
        """
        Sun, 02 Aug 2020 18:13:05 UTC
        """.trimIndent()

    private val staticSignatureKey = SignatureKey(
        id = "3ca3a36d-c9c4-421e-b76b-6f755bef244b",
        pemRepresentation =
            """
        -----BEGIN PUBLIC KEY-----
        MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAExArtfd8KY9Y1IJgjRMFHQvo00z3x
        YNa7c/VE0cKVP3iEbaO684i9FPQfXu8U/23UI25N62uMnmsF7orxSZJBVQ==
        -----END PUBLIC KEY-----
            """.trimIndent()
    )

    private val staticSampleData =
        """
        ewogICJzeW1wdG9tcyI6IFsKICAgIHsKICAgICAgInRpdGxlIjogewogICAgICAgICJlbi1HQiI6ICJBIGhpZ2ggdGVtcGVyYXR1cmUgKGZldmVyKSIKICAgICAgfSwKICAgICAgImRlc2NyaXB0aW9uIjogewogICAgICAgICJlbi1HQiI6ICJUaGlzIG1lYW5zIHRoYXQgeW91IGZlZWwgaG90IHRvIHRvdWNoIG9uIHlvdXIgY2hlc3Qgb3IgYmFjayAoeW91IGRvIG5vdCBuZWVkIHRvIG1lYXN1cmUgeW91ciB0ZW1wZXJhdHVyZSkuIgogICAgICB9LAogICAgICAicmlza1dlaWdodCI6IDEKICAgIH0sCiAgICB7CiAgICAgICJ0aXRsZSI6IHsKICAgICAgICAiZW4tR0IiOiAiQSBuZXcgY29udGludW91cyBjb3VnaCIKICAgICAgfSwKICAgICAgImRlc2NyaXB0aW9uIjogewogICAgICAgICJlbi1HQiI6ICJUaGlzIG1lYW5zIGNvdWdoaW5nIGEgbG90IGZvciBtb3JlIHRoYW4gYW4gaG91ciwgb3IgMyBvciBtb3JlIGNvdWdoaW5nIGVwaXNvZGVzIGluIDI0IGhvdXJzIChpZiB5b3UgdXN1YWxseSBoYXZlIGEgY291Z2gsIGl0IG1heSBiZSB3b3JzZSB0aGFuIHVzdWFsKS4iCiAgICAgIH0sCiAgICAgICJyaXNrV2VpZ2h0IjogMQogICAgfSwKICAgIHsKICAgICAgInRpdGxlIjogewogICAgICAgICJlbi1HQiI6ICJBIG5ldyBsb3NzIG9yIGNoYW5nZSB0byB5b3VyIHNlbnNlIG9mIHNtZWxsIG9yIHRhc3RlIgogICAgICB9LAogICAgICAiZGVzY3JpcHRpb24iOiB7CiAgICAgICAgImVuLUdCIjogIlRoaXMgbWVhbnMgeW91IGhhdmUgbm90aWNlZCB5b3UgY2Fubm90IHNtZWxsIG9yIHRhc3RlIGFueXRoaW5nLCBvciB0aGluZ3Mgc21lbGwgb3IgdGFzdGUgZGlmZmVyZW50IHRvIG5vcm1hbC4iCiAgICAgIH0sCiAgICAgICJyaXNrV2VpZ2h0IjogMQogICAgfQogIF0sCiAgInJpc2tUaHJlc2hvbGQiOiAwLjUsCiAgInN5bXB0b21zT25zZXRXaW5kb3dEYXlzIjogOQp9Cg==
        """.trimIndent()

    private val staticSignatureHeader =
        """
        keyId="3ca3a36d-c9c4-421e-b76b-6f755bef244b",signature="MEUCIQC8IJ86gourmANndtuy7O37nTBYyBnRYIQFAgPEMeJ89gIgNseHMCCV3ssgEXK1OomS6Sz37A8kwo7v7wp+G/7Dk/g="
        """.trimIndent()

    private val staticSignatureDateHeader =
        """
        Mon, 03 Aug 2020 23:20:48 UTC
        """.trimIndent()

    private val headerWithIncorrectKeyId =
        """
        keyId="not-my-key",signature="MEUCIARjKRl1BuRJcepNnKls41ox9Or70ZCfRWVkigmaki/SAiEAzemveYvOiK/qe+jKrvNmBWN1+rlk46kDmF3VFCZWHRE="
        """.trimIndent()

    private val misNamedHeaderParts =
        """
        notKeyId="b4c27bf3-8a76-4d2b-b91c-2152e7710a57",signature="MEUCIARjKRl1BuRJcepNnKls41ox9Or70ZCfRWVkigmaki/SAiEAzemveYvOiK/qe+jKrvNmBWN1+rlk46kDmF3VFCZWHRE="
        """.trimIndent()

    private val headerWithMalformedSignature =
        """
        keyId="b4c27bf3-8a76-4d2b-b91c-2152e7710a57",signature="not-a-sig"
        """.trimIndent()

    private val signatureKeys = listOf<SignatureKey>(dynamicSignatureKey, staticSignatureKey)

    //endregion

    private lateinit var sut: SignatureValidationInterceptor

    @Before
    fun setup() {
        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.SIGNATURE_VALIDATION)
        sut = SignatureValidationInterceptor(Java8Base64Decoder(), listOf(dynamicSignatureKey), false)
    }

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun analyticsSubmissionEndpointSignatureMustNotBeChecked() {
        val sut = SignatureValidationInterceptor(
            Java8Base64Decoder(),
            signatureKeys,
            true
        )

        val request = Request.Builder()
            .url("https://example.com/${AnalyticsApi.PATH}")
            .method("POST", "".toRequestBody())
            .addHeader("Request-Id", "D96CB6DC-6DE2-4ABF-8FDE-DF1A3D643D94")
            .build()

        val response = createResponse(
            request,
            listOf(),
            null
        )

        val chain = createChainMock(request, response)

        val receivedResponse = sut.intercept(chain)

        assertEquals(response, receivedResponse)
    }

    @Test
    fun verificationPassesForCorrectHeaderForDynamicContent() {
        val sut = SignatureValidationInterceptor(
            Java8Base64Decoder(),
            signatureKeys,
            true
        )

        val request = createRequest()

        val response = createResponse(
            request,
            listOf(
                Pair("x-amz-meta-signature", dynamicSignatureHeader),
                Pair("x-amz-meta-signature-date", dynamicSignatureDateHeader)
            ),
            null
        )

        val chain = createChainMock(request, response)

        sut.intercept(chain)
    }

    @Test
    fun verificationPassesForCorrectHeaderForStaticContent() {
        val sut = SignatureValidationInterceptor(
            Java8Base64Decoder(),
            signatureKeys,
            false
        )

        val request = createRequest()

        val response = createResponse(
            request,
            listOf(
                Pair("x-amz-meta-signature", staticSignatureHeader),
                Pair("x-amz-meta-signature-date", staticSignatureDateHeader)
            ),
            Java8Base64Decoder().decodeToBytes(staticSampleData)
        )

        val chain = createChainMock(request, response)

        sut.intercept(chain)
    }

    @Test(expected = IOException::class)
    fun verificationFailsForWrongMessage() {
        val sut = SignatureValidationInterceptor(
            Java8Base64Decoder(),
            signatureKeys,
            false
        )

        val request = createRequest()

        val response = createResponse(
            request,
            listOf(
                Pair("x-amz-meta-signature", staticSignatureHeader),
                Pair("x-amz-meta-signature-date", staticSignatureDateHeader)
            ),
            Java8Base64Decoder().decodeToBytes(staticSampleData).plus("oops".toByteArray())
        )

        val chain = createChainMock(request, response)

        sut.intercept(chain)
    }

    @Test(expected = IOException::class)
    fun verificationFailsIfNoHeaderIsSet() {
        val request = createRequest()

        val response = createResponse(
            request,
            emptyList(),
            Java8Base64Decoder().decodeToBytes(dynamicSampleData)
        )

        val chain = createChainMock(request, response)

        sut.intercept(chain)
    }

    @Test(expected = IOException::class)
    fun verificationFailsIfHeaderIsMalformed() {
        val request = createRequest()

        val response = createResponse(
            request,
            listOf(
                Pair("x-amz-meta-signature", "asdfagfskfng"),
                Pair("x-amz-meta-signature-date", "dfghdshgyyyds") // TODO more data needed
            ),
            Java8Base64Decoder().decodeToBytes(dynamicSampleData)
        )

        val chain = createChainMock(request, response)

        sut.intercept(chain)
    }

    @Test(expected = IOException::class)
    fun verificationFailsForHeaderWithWrongKeyId() {
        val request = createRequest()

        val response = createResponse(
            request,
            listOf(
                Pair("x-amz-meta-signature", headerWithIncorrectKeyId),
                Pair("x-amz-meta-signature-date", dynamicSignatureDateHeader)
            ),
            null
        )

        val chain = createChainMock(request, response)

        sut.intercept(chain)
    }

    @Test(expected = IOException::class)
    fun verificationFailsIfHeaderNamesWrong() {
        val request = createRequest()

        val response = createResponse(
            request,
            listOf(
                Pair("x-amz-meta-signature", misNamedHeaderParts),
                Pair("x-amz-meta-signature-date", dynamicSignatureDateHeader)
            ),
            null
        )

        val chain = createChainMock(request, response)

        sut.intercept(chain)
    }

    @Test(expected = IllegalArgumentException::class)
    fun verificationFailsIfHeaderPartSignatureWrong() {
        val request = createRequest()

        val response = createResponse(
            request,
            listOf(
                Pair("x-amz-meta-signature", headerWithMalformedSignature),
                Pair("x-amz-meta-signature-date", dynamicSignatureDateHeader)
            ),
            null
        )

        val chain = createChainMock(request, response)

        sut.intercept(chain)
    }

    private fun createRequest(): Request {
        return Request.Builder()
            .url("https://example.com/activation/request")
            .method("POST", "".toRequestBody())
            .addHeader("Request-Id", "D96CB6DC-6DE2-4ABF-8FDE-DF1A3D643D94")
            .build()
    }

    private fun createResponse(
        request: Request,
        headers: List<Pair<String, String>>,
        body: ByteArray?
    ): Response {
        val response = Builder()
            .request(request)
            .protocol(HTTP_1_1)
            .code(200)
            .message("OK")
            .body((body ?: ByteArray(0)).toResponseBody())
            .build()

        val builder = response.newBuilder()
        headers.forEach {
            builder.addHeader(it.first, it.second)
        }

        return builder.build()
    }

    private fun createChainMock(request: Request, response: Response): Chain {
        val chain = mockk<Chain>(relaxed = true)
        every { chain.request() } returns request
        every { chain.proceed(any()) } returns response
        return chain
    }
}
