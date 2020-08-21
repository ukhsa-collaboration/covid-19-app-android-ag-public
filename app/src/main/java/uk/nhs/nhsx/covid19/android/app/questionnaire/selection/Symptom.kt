package uk.nhs.nhsx.covid19.android.app.questionnaire.selection

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import uk.nhs.nhsx.covid19.android.app.common.Translatable

@Parcelize
@JsonClass(generateAdapter = true)
data class Symptom(
    val title: Translatable,
    val description: Translatable,
    val riskWeight: Double = 0.0
) : Parcelable
