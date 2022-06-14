package uk.nhs.nhsx.covid19.android.app.remote.data

import com.squareup.moshi.JsonClass
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.Cardinal
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.NonCardinal
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.Symptom

@JsonClass(generateAdapter = true)
data class QuestionnaireResponse(
    val symptoms: List<Symptom>,
    val cardinal: Cardinal,
    val noncardinal: NonCardinal,
    val riskThreshold: Float,
    val symptomsOnsetWindowDays: Int,
    val isSymptomaticSelfIsolationForWalesEnabled: Boolean
)
