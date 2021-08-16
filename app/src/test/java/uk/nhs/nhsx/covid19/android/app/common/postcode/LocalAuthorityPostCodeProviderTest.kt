package uk.nhs.nhsx.covid19.android.app.common.postcode

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import kotlin.test.assertEquals

class LocalAuthorityPostCodeProviderTest {
    private val localAuthorityProvider: LocalAuthorityProvider = mockk(relaxUnitFun = true)
    private val localAuthorityPostCodesLoader: LocalAuthorityPostCodesLoader = mockk(relaxUnitFun = true)

    private val localAuthorityPostCodes =
        LocalAuthorityPostCodes(
            postcodes = mapOf(
                "AB1" to listOf("S0001"),
                "AL1" to listOf("S0002", "S0003")
            ),
            localAuthorities = mapOf(
                "S0001" to LocalAuthority("Aberdeenshire", "Scotland"),
                "S0002" to LocalAuthority("Something", "England"),
                "S0003" to LocalAuthority("Something else", "England")
            )
        )

    private val testSubject = LocalAuthorityPostCodeProvider(localAuthorityProvider, localAuthorityPostCodesLoader)

    @Test
    fun `when local authority is null, a null value is returned`() = runBlocking {
        every { localAuthorityProvider.value } returns null

        val result = testSubject.getPostCodeDistrict()

        assertEquals(null, result)
        verify { localAuthorityProvider.value }
        coVerify(exactly = 0) { localAuthorityPostCodesLoader.load() }
    }

    @Test
    fun `when local authority is not null and localAuthorityPostCodesLoader load fails, a null value is returned`() =
        runBlocking {
            every { localAuthorityProvider.value } returns "AC1"
            coEvery { localAuthorityPostCodesLoader.load() } returns null

            val result = testSubject.getPostCodeDistrict()

            assertEquals(null, result)
            verify { localAuthorityProvider.value }
            coVerify { localAuthorityPostCodesLoader.load() }
        }

    @Test
    fun `when local authority is not null and is not in postcodes, a null value is returned`() =
        runBlocking {
            every { localAuthorityProvider.value } returns "AC1"
            coEvery { localAuthorityPostCodesLoader.load() } returns localAuthorityPostCodes

            val result = testSubject.getPostCodeDistrict()

            assertEquals(null, result)
            verify { localAuthorityProvider.value }
            coVerify { localAuthorityPostCodesLoader.load() }
        }

    @Test
    fun `when local authority is not null and is in postcodes, the country value is returned`() =
        runBlocking {
            every { localAuthorityProvider.value } returns "S0002"
            coEvery { localAuthorityPostCodesLoader.load() } returns localAuthorityPostCodes

            val result = testSubject.getPostCodeDistrict()

            assertEquals(ENGLAND, result)
            verify { localAuthorityProvider.value }
            coVerify { localAuthorityPostCodesLoader.load() }
        }
}
