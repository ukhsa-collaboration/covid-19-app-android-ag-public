package uk.nhs.nhsx.covid19.android.app.common.postcode

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetLocalAuthorityNameTest {

    private val localAuthorityProvider = mockk<LocalAuthorityProvider>()
    private val localAuthorityPostCodesLoader = mockk<LocalAuthorityPostCodesLoader>()

    private val getLocalAuthorityName = GetLocalAuthorityName(localAuthorityProvider, localAuthorityPostCodesLoader)

    @Test
    fun `when local authority with id from LocalAuthorityProvider is known to the app then return its name`() =
        runBlocking {
            every { localAuthorityProvider.value } returns "S0001"
            coEvery { localAuthorityPostCodesLoader.load() } returns localAuthorityPostCodes

            assertEquals(expected = "Aberdeenshire", getLocalAuthorityName())
        }

    @Test
    fun `when local authority with id from LocalAuthorityProvider is not known to the app then return null`() =
        runBlocking {
            every { localAuthorityProvider.value } returns "unknown_local_authority_id"
            coEvery { localAuthorityPostCodesLoader.load() } returns localAuthorityPostCodes

            assertNull(getLocalAuthorityName())
        }

    @Test
    fun `when no local authority id is known to the app then return null`() = runBlocking {
        every { localAuthorityProvider.value } returns null
        coEvery { localAuthorityPostCodesLoader.load() } returns localAuthorityPostCodes

        assertNull(getLocalAuthorityName())
    }

    @Test
    fun `when LocalAuthorityPostCodesLoader returns null then return null`() = runBlocking {
        every { localAuthorityProvider.value } returns "S0001"
        coEvery { localAuthorityPostCodesLoader.load() } returns null

        assertNull(getLocalAuthorityName())
    }

    private val localAuthorityPostCodes = LocalAuthorityPostCodes(
        postcodes = mapOf(
            "AB1" to listOf("S0001"),
            "AB2" to listOf("S0002", "S0003"),
        ),
        localAuthorities = mapOf(
            "S0001" to LocalAuthority("Aberdeenshire", "England"),
            "S0002" to LocalAuthority("Something", "Wales"),
            "S0003" to LocalAuthority("Something else", "Scotland"),
        )
    )
}
