package uk.nhs.nhsx.covid19.android.app.remote.data

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import uk.nhs.nhsx.covid19.android.app.common.Translatable
import java.time.Instant

@JsonClass(generateAdapter = true)
data class LocalMessagesResponse(
    @Json(name = "las")
    val localAuthorities: Map<String, List<String>>,
    val messages: Map<String, LocalMessage>
)

@JsonClass(generateAdapter = true)
data class LocalMessage(
    val type: LocalMessageType,
    val updated: Instant,
    val contentVersion: Int,
    val translations: TranslatableLocalMessage
)

enum class LocalMessageType(val jsonName: String) {
    NOTIFICATION(jsonName = "notification"),
    UNKNOWN(jsonName = "unknown");
}

@Parcelize
data class TranslatableLocalMessage(
    override val translations: Map<String, LocalMessageTranslation>
) : Translatable<LocalMessageTranslation> {
    fun replacePlaceholders(postCode: String, localAuthority: String): TranslatableLocalMessage =
        TranslatableLocalMessage(
            translations.mapValues { mapItem ->
                val message = mapItem.value
                message.copy(
                    head = message.head?.replacePostCodeAndLocalAuthority(postCode, localAuthority),
                    body = message.body?.replacePostCodeAndLocalAuthority(postCode, localAuthority),
                    content = message.content?.map { contentBlock ->
                        contentBlock.copy(
                            text = contentBlock.text?.replacePostCodeAndLocalAuthority(
                                postCode,
                                localAuthority
                            )
                        )
                    }
                )
            }
        )

    private fun String.replacePostCodeAndLocalAuthority(postCode: String, localAuthority: String): String {
        return replace(postCodePlaceholder, postCode).replace(localAuthorityPlaceholder, localAuthority)
    }

    companion object {
        private const val postCodePlaceholder = "[postcode]"
        private const val localAuthorityPlaceholder = "[local authority]"
    }
}

@Parcelize
@JsonClass(generateAdapter = true)
data class LocalMessageTranslation(
    val head: String?,
    val body: String?,
    val content: List<ContentBlock>?
) : Parcelable

@Parcelize
@JsonClass(generateAdapter = true)
data class ContentBlock(
    val type: ContentBlockType,
    val text: String? = null,
    val link: String? = null,
    val linkText: String? = null
) : Parcelable {
    fun isDisplayable(): Boolean {
        return text != null || link != null
    }
}

@Parcelize
enum class ContentBlockType(val jsonName: String) : Parcelable {
    PARAGRAPH(jsonName = "para"),
    UNKNOWN(jsonName = "unknown")
}
