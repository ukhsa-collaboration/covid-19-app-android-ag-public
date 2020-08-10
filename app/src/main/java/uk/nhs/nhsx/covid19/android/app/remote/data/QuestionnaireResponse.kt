package uk.nhs.nhsx.covid19.android.app.remote.data

import com.squareup.moshi.JsonClass
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.Symptom

@JsonClass(generateAdapter = true)
data class QuestionnaireResponse(
    val symptoms: List<Symptom>,
    val riskThreshold: Float,
    val symptomsOnsetWindowDays: Int
)
