package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import uk.nhs.nhsx.covid19.android.app.remote.VirologyTestingApi
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyCtaExchangeRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyCtaExchangeResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.CtaTokenValidator.CtaTokenValidationResult.Failure
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.CtaTokenValidator.CtaTokenValidationResult.Success
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultErrorType.INVALID
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultErrorType.NO_CONNECTION
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultErrorType.UNEXPECTED
import java.io.IOException
import java.time.Instant
import kotlin.test.assertEquals

class CtaTokenValidatorTest {

    private val virologyTestingApi = mockk<VirologyTestingApi>()
    private val crockfordDammValidator = mockk<CrockfordDammValidator>()

    private val testSubject = CtaTokenValidator(virologyTestingApi, crockfordDammValidator)

    @Before
    fun setUp() {
        every { crockfordDammValidator.validate(any()) } returns true
    }

    @Test
    fun `cta token length wrong`() = runBlocking {
        val ctaToken = ""

        val result = testSubject.validate(ctaToken)

        assertEquals(Failure(INVALID), result)
    }

    @Test
    fun `crockford damm validator returns false`() = runBlocking {
        every { crockfordDammValidator.validate(any()) } returns false

        val invalidCtaToken = "12345678"

        val result = testSubject.validate(invalidCtaToken)

        assertEquals(Failure(INVALID), result)
    }

    @Test
    fun `cta token valid returns success`() = runBlocking {
        val ctaToken = "12345678"

        val response = Response.success(
            VirologyCtaExchangeResponse(
                "submissionToken",
                Instant.now(),
                NEGATIVE
            )
        )
        coEvery { virologyTestingApi.getTestResultForCtaToken(VirologyCtaExchangeRequest(ctaToken)) } returns response

        val result = testSubject.validate(ctaToken)

        assertEquals(Success(response.body()!!), result)
    }

    @Test
    fun `cta token validation returns 400 results in invalid code error state`() = runBlocking {
        val ctaToken = "12345678"

        val response = Response.error<VirologyCtaExchangeResponse>(
            400,
            "{}".toResponseBody("application/json".toMediaType())
        )

        coEvery {
            virologyTestingApi.getTestResultForCtaToken(
                VirologyCtaExchangeRequest(
                    ctaToken
                )
            )
        } returns response

        val result = testSubject.validate(ctaToken)

        assertEquals(Failure(INVALID), result)
    }

    @Test
    fun `cta token validation returns 404 results in invalid code error state`() = runBlocking {
        val ctaToken = "12345678"

        val response = Response.error<VirologyCtaExchangeResponse>(
            404,
            "{}".toResponseBody("application/json".toMediaType())
        )

        coEvery {
            virologyTestingApi.getTestResultForCtaToken(
                VirologyCtaExchangeRequest(
                    ctaToken
                )
            )
        } returns response

        val result = testSubject.validate(ctaToken)

        assertEquals(Failure(INVALID), result)
    }

    @Test
    fun `cta token validation returns 500 results in unexpected error state`() = runBlocking {
        val ctaToken = "12345678"

        val response = Response.error<VirologyCtaExchangeResponse>(
            500,
            "{}".toResponseBody("application/json".toMediaType())
        )

        coEvery {
            virologyTestingApi.getTestResultForCtaToken(
                VirologyCtaExchangeRequest(
                    ctaToken
                )
            )
        } returns response

        val result = testSubject.validate(ctaToken)

        assertEquals(Failure(UNEXPECTED), result)
    }

    @Test
    fun `cta token validation throws IOException results in no connection error state`() =
        runBlocking {
            val ctaToken = "12345678"

            coEvery {
                virologyTestingApi.getTestResultForCtaToken(VirologyCtaExchangeRequest(ctaToken))
            } throws IOException()

            val result = testSubject.validate(ctaToken)

            assertEquals(Failure(NO_CONNECTION), result)
        }

    @Test
    fun `cta token validation throws exception other than IOException results in unexpected error state`() =
        runBlocking {
            val ctaToken = "12345678"

            coEvery {
                virologyTestingApi.getTestResultForCtaToken(VirologyCtaExchangeRequest(ctaToken))
            } throws Exception()

            val result = testSubject.validate(ctaToken)

            assertEquals(Failure(UNEXPECTED), result)
        }
}
