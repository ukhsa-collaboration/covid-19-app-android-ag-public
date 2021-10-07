package uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review

import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.android.parcel.Parcelize

@Parcelize
data class OptOutResponseEntry(
    val questionType: QuestionType,
    val response: Boolean
) : Parcelable {

    @get:StringRes
    val responseText: Int
        get() = if (response) questionType.yesResponseText else questionType.noResponseText

    @get:StringRes
    val contentDescription: Int
        get() = if (response) questionType.yesContentDescription else questionType.noContentDescription
}
