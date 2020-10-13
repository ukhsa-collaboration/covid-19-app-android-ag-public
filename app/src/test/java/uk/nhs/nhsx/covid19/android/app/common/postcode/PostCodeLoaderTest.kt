package uk.nhs.nhsx.covid19.android.app.common.postcode

import android.content.Context
import android.content.res.AssetManager
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals

class PostCodeLoaderTest {

    private val context = mockk<Context>()
    private val moshi = mockk<Moshi>()
    private val assetManager = mockk<AssetManager>()
    private val moshiAdapter = mockk<JsonAdapter<Map<String, List<String>>>>(relaxed = true)

    private val testSubject = PostCodeLoader(context, moshi)

    private val postCodeDistricts =
        """{"ENGLAND":"["AB10","AB11"]"}"""

    private val type = Types.newParameterizedType(Map::class.java, String::class.java, List::class.java)

    @Before
    fun setUp() {
        every { context.assets } returns assetManager
        every { assetManager.open(any()) } returns postCodeDistricts.byteInputStream()
        every { moshi.adapter<Map<String, List<String>>>(type) } returns moshiAdapter
        every { moshiAdapter.fromJson(any<String>()) } returns mapOf()
    }

    @Test
    fun `read list form json`() = runBlocking {
        testSubject.loadPostCodes()

        verify { moshiAdapter.fromJson(any<String>()) }
    }

    @Test
    fun `read list form json throws exception`() = runBlocking {
        every { assetManager.open(any()) } throws Exception()

        testSubject.loadPostCodes()

        verify(exactly = 0) { moshiAdapter.fromJson(any<String>()) }
    }

    @Test
    fun `read list from json returns empty if json is null`() = runBlocking {
        every { moshiAdapter.fromJson(any<String>()) } returns null

        val result = testSubject.loadPostCodes()

        assertEquals(mapOf(), result)
    }

    @Test
    fun `read list from json throws exception which returns empty map`() = runBlocking {
        every { assetManager.open("postalDistricts.json") } throws IOException()

        val result = testSubject.loadPostCodes()

        assertEquals(mapOf(), result)
    }
}
