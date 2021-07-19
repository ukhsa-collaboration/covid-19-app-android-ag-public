package uk.nhs.nhsx.covid19.android.app.util.adapters

import com.squareup.moshi.Moshi
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.ContentBlock
import uk.nhs.nhsx.covid19.android.app.remote.data.ContentBlockType.PARAGRAPH
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalInformation
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalInformation.Notification
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalInformation.Unknown
import uk.nhs.nhsx.covid19.android.app.remote.data.NotificationMessage
import uk.nhs.nhsx.covid19.android.app.remote.data.TranslatableNotificationMessage
import java.time.Instant
import kotlin.test.assertEquals

class LocalInformationAdapterTest {
    private val moshi = Moshi.Builder()
        .add(LocalInformationAdapter())
        .add(InstantAdapter())
        .add(ContentBlockTypeAdapter())
        .add(LocalMessageTypeAdapter())
        .build()

    private val testSubject = moshi.adapter(LocalInformation::class.java)

    private val notification = Notification(
        updated = Instant.parse("2021-05-19T14:59:13Z"),
        contentVersion = 1,
        translations = TranslatableNotificationMessage(
            translations = mapOf(
                "en" to NotificationMessage(
                    head = "",
                    body = "",
                    content = listOf(
                        ContentBlock(
                            type = PARAGRAPH,
                            text = "",
                            link = "",
                            linkText = ""
                        )
                    )
                )
            )
        )
    )

    private val notificationMessageJson =
        """{"head":"","body":"","content":[{"type":"para","text":"","link":"","linkText":""}]}"""

    private val notificationJson = localInformationJson("notification")

    private fun localInformationJson(
        type: String,
        updated: String = "\"2021-05-19T14:59:13Z\"",
        contentVersion: String = "1",
        translation: String = notificationMessageJson
    ) =
        """{"type":"$type","updated":$updated,"contentVersion":$contentVersion,"translations":{"en":$translation}}""".trimMargin()

    @Test
    fun `toJson adds enum type notification when is notification object`() {
        val result = testSubject.toJson(notification)

        assertEquals(notificationJson, result)
    }

    @Test
    fun `toJson on unknown returns json string of empty local message object`() {
        val result = testSubject.toJson(Unknown)

        val expected =
            """{"type":"unknown"}"""
        assertEquals(expected, result)
    }

    @Test
    fun `fromJson converts string to LocalInformation`() {
        val result = testSubject.fromJson(notificationJson)

        assertEquals(notification, result)
    }

    @Test
    fun `fromJson converts to Unknown if has unrecognised type`() {
        val unknownString = localInformationJson("something-unknown")
        val result = testSubject.fromJson(unknownString)

        assertEquals(Unknown, result)
    }

    @Test
    fun `fromJson drops any translations in a notification message which don't have all required fields`() {
        val translation =
            """{"body":"something"}"""
        val json = localInformationJson(type = "notification", translation = translation)
        val result = testSubject.fromJson(json)

        assertEquals(notification.copy(translations = TranslatableNotificationMessage(mapOf())), result)
    }

    @Test
    fun `fromJson returns unknown if type is notification but doesn't have updated and contentVersion fields`() {
        val json = localInformationJson(type = "notification", updated = "null", contentVersion = "null")
        val result = testSubject.fromJson(json)

        assertEquals(Unknown, result)
    }
}
