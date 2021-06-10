package uk.nhs.nhsx.covid19.android.app.questionnaire.review

import uk.nhs.nhsx.covid19.android.app.common.TranslatableString
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.Symptom

object SymptomsFixture {

    val symptom1 = Symptom(
        title = TranslatableString(mapOf("en-GB" to "Sneeze")),
        description = TranslatableString(mapOf("en-GB" to "Sneeze")),
        riskWeight = 10.0
    )
    val symptom2 = Symptom(
        title = TranslatableString(mapOf("en-GB" to "Cough")),
        description = TranslatableString(mapOf("en-GB" to "Cough")),
        riskWeight = 1.0
    )
    val symptom3 = Symptom(
        title = TranslatableString(mapOf("en-GB" to "Temperature")),
        description = TranslatableString(mapOf("en-GB" to "Temperature")),
        riskWeight = 8.0
    )
    val symptom4 = Symptom(
        title = TranslatableString(mapOf("en-GB" to "Allergy")),
        description = TranslatableString(mapOf("en-GB" to "Allergy")),
        riskWeight = 0.0
    )

    val symptoms = listOf(symptom1, symptom2, symptom3, symptom4)
}
