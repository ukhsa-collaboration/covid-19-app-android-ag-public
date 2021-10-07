package uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ReviewData(
    val questionnaireOutcome: QuestionnaireOutcome,
    val ageResponse: OptOutResponseEntry,
    val vaccinationStatusResponses: List<OptOutResponseEntry>
) : Parcelable
