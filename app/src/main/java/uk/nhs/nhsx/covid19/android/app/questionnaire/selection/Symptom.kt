package uk.nhs.nhsx.covid19.android.app.questionnaire.selection

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import uk.nhs.nhsx.covid19.android.app.common.TranslatableString

@Parcelize
@JsonClass(generateAdapter = true)
data class Symptom(
    val title: TranslatableString,
    val description: TranslatableString,
    val riskWeight: Double = 0.0
) : Parcelable
