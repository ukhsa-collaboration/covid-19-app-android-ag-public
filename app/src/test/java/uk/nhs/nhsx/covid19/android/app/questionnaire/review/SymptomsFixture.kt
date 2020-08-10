package uk.nhs.nhsx.covid19.android.app.questionnaire.review

import uk.nhs.nhsx.covid19.android.app.common.TranslatedString
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.Symptom

object SymptomsFixture {
    val symptom1 = Symptom(TranslatedString("Sneeze"), TranslatedString("Sneeze"), 10.0)
    val symptom2 = Symptom(TranslatedString("Cough"), TranslatedString("Cough"), 1.0)
    val symptom3 =
        Symptom(TranslatedString("Temperature"), TranslatedString("Temperature"), 8.0)
    val symptom4 =
        Symptom(TranslatedString("Allergy"), TranslatedString("Allergy"), 0.0)

    val symptoms = listOf(symptom1, symptom2, symptom3, symptom4)
}
