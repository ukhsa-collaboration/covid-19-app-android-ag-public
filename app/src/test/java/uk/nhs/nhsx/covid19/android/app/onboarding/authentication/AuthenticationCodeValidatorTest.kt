package uk.nhs.nhsx.covid19.android.app.onboarding.authentication

import io.mockk.Called
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verifyAll
import kotlinx.coroutines.runBlocking
import org.junit.Test
import retrofit2.Response
import uk.nhs.nhsx.covid19.android.app.remote.ActivationApi
import kotlin.test.assertEquals

class AuthenticationCodeValidatorTest {
    private val activationApi = mockk<ActivationApi>()

    private val testSubject = AuthenticationCodeValidator(activationApi)

    @Test
    fun invalidAuthCode_returnsFalse() = runBlocking {
        testSubject.validate("")

        verifyAll {
            activationApi wasNot Called
        }
    }

    @Test
    fun validAuthCode_returnsTrue() = runBlocking {
        coEvery { activationApi.activate(any()) } returns Response.success(Unit)

        val response = testSubject.validate("12345678")

        assertEquals(true, response)
    }
}
