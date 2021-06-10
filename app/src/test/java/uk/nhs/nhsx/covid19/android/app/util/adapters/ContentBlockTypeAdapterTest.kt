package uk.nhs.nhsx.covid19.android.app.util.adapters

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.ContentBlockType
import uk.nhs.nhsx.covid19.android.app.remote.data.ContentBlockType.UNKNOWN
import kotlin.test.assertEquals

class ContentBlockTypeAdapterTest {
    private val testSubject = ContentBlockTypeAdapter()

    private val contentBlockType = ContentBlockType.PARAGRAPH
    private val contentBlockTypeJson = "para"
    private val unsupportedContentBlockTypeJson = "random-string"

    @Test
    fun `toJson converts LocalMessageType to json string`() {
        assertEquals(contentBlockTypeJson, testSubject.toJson(contentBlockType))
    }

    @Test
    fun `fromJson converts json string to LocalMessageType`() {
        assertEquals(contentBlockType, testSubject.fromJson(contentBlockTypeJson))
    }

    @Test
    fun `fromJson converts unsupported json string to UNKNOWN`() {
        assertEquals(UNKNOWN, testSubject.fromJson(unsupportedContentBlockTypeJson))
    }
}
