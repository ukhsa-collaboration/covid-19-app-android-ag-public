package uk.nhs.nhsx.covid19.android.app.remote.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.ContentBlockType.PARAGRAPH

class ContentBlockTest {
    @Test
    fun `when ContentBlock has all fields, isDisplayable is true`() {
        val contentBlock = ContentBlock(PARAGRAPH, text = "test", link = "link", linkText = "linkText")
        assertTrue(contentBlock.isDisplayable())
    }

    @Test
    fun `when ContentBlock has text and no link fields, isDisplayable is true`() {
        val contentBlock = ContentBlock(PARAGRAPH, text = "test")
        assertTrue(contentBlock.isDisplayable())
    }

    @Test
    fun `when ContentBlock no text but link fields, isDisplayable is true`() {
        val contentBlock = ContentBlock(PARAGRAPH, link = "link", linkText = "linkText")
        assertTrue(contentBlock.isDisplayable())
    }

    @Test
    fun `when ContentBlock has link but no linkText, isDisplayable is true`() {
        val contentBlock = ContentBlock(PARAGRAPH, link = "link")
        assertTrue(contentBlock.isDisplayable())
    }

    @Test
    fun `when ContentBlock has no fields, isDisplayable is false`() {
        val contentBlock = ContentBlock(PARAGRAPH)
        assertFalse(contentBlock.isDisplayable())
    }

    @Test
    fun `when ContentBlock has just link text, isDisplayable is false`() {
        val contentBlock = ContentBlock(PARAGRAPH, linkText = "linkText")
        assertFalse(contentBlock.isDisplayable())
    }
}
