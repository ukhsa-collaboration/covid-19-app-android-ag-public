package uk.nhs.nhsx.covid19.android.app.common.postcode

import android.content.Context
import android.content.res.AssetManager
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class LocalAuthorityPostCodesLoaderTest {

    private val context = mockk<Context>()
    private val moshi = mockk<Moshi>()
    private val assetManager = mockk<AssetManager>()
    private val moshiAdapter = mockk<JsonAdapter<LocalAuthorityPostCodes>>(relaxed = true)

    private val testSubject = LocalAuthorityPostCodesLoader(context, moshi)

    private val localAuthorityPostCodes =
        """{"postCodes":{"AB1": ["S0001"],"AL1": ["S0002","S0003"]},"localAuthorities": {"S0001": {"name": "Aberdeenshire","country": "Scotland"},"S0002": {"name": "Something","country": "England"},"S0003": {"name": "Something else","country": "England"}}}"""

    @Before
    fun setUp() {
        every { context.assets } returns assetManager
        every { assetManager.open(any()) } returns localAuthorityPostCodes.byteInputStream()
        every { moshi.adapter<LocalAuthorityPostCodes>(LocalAuthorityPostCodes::class.java) } returns moshiAdapter
        every { moshiAdapter.fromJson(any<String>()) } returns null
    }

    @Test
    fun `read list from json`() = runBlocking {
        testSubject.load()

        verify { moshiAdapter.fromJson(any<String>()) }
    }

    @Test
    fun `read list from json throws exception`() = runBlocking {
        every { assetManager.open(any()) } throws Exception()

        testSubject.load()

        verify(exactly = 0) { moshiAdapter.fromJson(any<String>()) }
    }

    @Test
    fun `read list from json returns Unit if json is null`() = runBlocking {
        every { moshiAdapter.fromJson(any<String>()) } returns null

        val result = testSubject.load()

        assertEquals(null, result)
    }

    @Test
    fun `read list from json throws exception which returns null`() = runBlocking {
        every { assetManager.open("localAuthorities.json") } throws IOException()

        val result = testSubject.load()

        assertEquals(null, result)
    }

    @Test
    fun `country that doesn't exist is not supported`() {
        val localAuthorityInNonExistingCountry = LocalAuthority(
            name = "Something",
            country = "Non-existing"
        )

        assertFalse { localAuthorityInNonExistingCountry.supported() }
    }
}
