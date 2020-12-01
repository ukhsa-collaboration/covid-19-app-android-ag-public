package uk.nhs.nhsx.covid19.android.app.common.postcode

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult
import kotlin.test.assertEquals

class LocalAuthorityPostCodeValidatorTest {

    private val localAuthorityPostCodesLoader = mockk<LocalAuthorityPostCodesLoader>(relaxed = true)

    private val testSubject = LocalAuthorityPostCodeValidator(localAuthorityPostCodesLoader)

    @Before
    fun setUp() {
        coEvery { localAuthorityPostCodesLoader.load() } returns LocalAuthorityPostCodes(
            postcodes = mapOf(
                "AB1" to listOf("S0001"),
                "AB2" to listOf("S0002", "S0003"),
                "AB3" to listOf("S0003"),
                "AB4" to listOf("S0004", "S0005")
            ),
            localAuthorities = mapOf(
                "S0001" to LocalAuthority("Aberdeenshire", "England"),
                "S0002" to LocalAuthority("Something", "Wales"),
                "S0003" to LocalAuthority("Something else", "Scotland"),
                "S0004" to LocalAuthority("Yet another place", "Scotland"),
                "S0005" to LocalAuthority("And another one", "Northern Ireland")
            )
        )
    }

    @Test
    fun `return Invalid if post code is empty`() = runBlocking {
        val result = testSubject.validate("")

        assertEquals(LocalAuthorityPostCodeValidationResult.Invalid, result)
    }

    @Test
    fun `return Invalid if post code only contains white spaces`() = runBlocking {
        val result = testSubject.validate("   ")

        assertEquals(LocalAuthorityPostCodeValidationResult.Invalid, result)
    }

    @Test
    fun `return JsonParseError if post code and local authority map could not be loaded`() = runBlocking {
        coEvery { localAuthorityPostCodesLoader.load() } returns null

        val result = testSubject.validate("AB1")

        assertEquals(LocalAuthorityPostCodeValidationResult.ParseJsonError, result)
    }

    @Test
    fun `return Invalid if post code is not in list of post codes`() = runBlocking {
        val result = testSubject.validate("AB9")

        assertEquals(LocalAuthorityPostCodeValidationResult.Invalid, result)
    }

    @Test
    fun `return Unsupported if post code is in list of post codes and country is not supported`() = runBlocking {
        val result = testSubject.validate("AB3")

        assertEquals(LocalAuthorityPostCodeValidationResult.Unsupported, result)
    }

    @Test
    fun `return Unsupported if post code is in list of post codes and none of the countries are supported`() = runBlocking {
        val result = testSubject.validate("AB4")

        assertEquals(LocalAuthorityPostCodeValidationResult.Unsupported, result)
    }

    @Test
    fun `return Valid if post code is in list of post codes and country is supported`() = runBlocking {
        val result = testSubject.validate("AB2")

        assertEquals(
            LocalAuthorityPostCodeValidationResult.Valid(
                "AB2",
                listOf(
                    LocalAuthorityWithId("S0002", LocalAuthority("Something", "Wales")),
                    LocalAuthorityWithId("S0003", LocalAuthority("Something else", "Scotland"))
                )
            ),
            result
        )
    }

    @Test
    fun `return Valid if post code is in list of post codes and country is supported and post code contains extra white spaces`() =
        runBlocking {
            val result = testSubject.validate(" AB1 ")

            assertEquals(
                LocalAuthorityPostCodeValidationResult.Valid(
                    "AB1",
                    listOf(
                        LocalAuthorityWithId("S0001", LocalAuthority("Aberdeenshire", "England"))
                    )
                ),
                result
            )
        }
}
