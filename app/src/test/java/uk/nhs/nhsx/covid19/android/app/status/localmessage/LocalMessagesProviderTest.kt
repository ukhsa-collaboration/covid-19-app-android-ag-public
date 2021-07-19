package uk.nhs.nhsx.covid19.android.app.status.localmessage

import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.remote.data.ContentBlock
import uk.nhs.nhsx.covid19.android.app.remote.data.ContentBlockType.PARAGRAPH
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalInformation.Notification
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessagesResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.NotificationMessage
import uk.nhs.nhsx.covid19.android.app.remote.data.TranslatableNotificationMessage
import uk.nhs.nhsx.covid19.android.app.status.localmessage.LocalMessagesProvider.Companion.VALUE_KEY
import uk.nhs.nhsx.covid19.android.app.util.ProviderTest
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectation
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectationDirection.OBJECT_TO_JSON
import uk.nhs.nhsx.covid19.android.app.util.adapters.ContentBlockTypeAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.LocalInformationAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.LocalMessageTypeAdapter
import java.time.Instant

class LocalMessagesProviderTest : ProviderTest<LocalMessagesProvider, LocalMessagesResponse?>() {
    private val moshi = Moshi.Builder()
        .add(LocalMessageTypeAdapter())
        .add(ContentBlockTypeAdapter())
        .add(InstantAdapter())
        .add(LocalInformationAdapter())
        .build()

    private val localMessagesJson =
        """{"las":{"ABCD1234":["message1"]},"messages":{"message1":{"type":"notification","updated":"2021-05-19T14:59:13Z","contentVersion":1,"translations":{"en":{"head":"A new variant of concern is in your area","body":"This is the body of the notification","content":[{"type":"para","text":"There have been reported cases of a new variant in [postcode]. Here are some key pieces of information to help you stay safe"},{"type":"para","text":"There have been reported cases of a new variant in [postcode]. Here are some key pieces of information to help you stay safe","link":"http://example.com","linkText":"Click me"}]}}}}}"""

    private val localMessagesResponse = LocalMessagesResponse(
        localAuthorities = mapOf(
            "ABCD1234" to listOf("message1")
        ),
        messages = mapOf(
            "message1" to Notification(
                updated = Instant.parse("2021-05-19T14:59:13Z"),
                contentVersion = 1,
                translations = TranslatableNotificationMessage(
                    mapOf(
                        "en" to NotificationMessage(
                            head = "A new variant of concern is in your area",
                            body = "This is the body of the notification",
                            content = listOf(
                                ContentBlock(
                                    type = PARAGRAPH,
                                    text = "There have been reported cases of a new variant in [postcode]. Here are some key pieces of information to help you stay safe"
                                ),
                                ContentBlock(
                                    type = PARAGRAPH,
                                    text = "There have been reported cases of a new variant in [postcode]. Here are some key pieces of information to help you stay safe",
                                    link = "http://example.com",
                                    linkText = "Click me"
                                )
                            )
                        )
                    )
                )
            )
        )
    )

    override val getTestSubject: (SharedPreferences) -> LocalMessagesProvider = { sharedPreferences -> LocalMessagesProvider(moshi, sharedPreferences) }
    override val property = LocalMessagesProvider::localMessages
    override val key = VALUE_KEY
    override val defaultValue: LocalMessagesResponse? = null
    override val expectations: List<ProviderTestExpectation<LocalMessagesResponse?>> = listOf(
        ProviderTestExpectation(json = localMessagesJson, objectValue = localMessagesResponse),
        ProviderTestExpectation(json = null, objectValue = null, direction = OBJECT_TO_JSON)
    )
}
