package uk.nhs.nhsx.covid19.android.app.common

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class TranslatedString(@Json(name = "en-GB") val enGB: String) : Parcelable {
    fun provideTranslation() = enGB
}
