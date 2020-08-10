package uk.nhs.nhsx.covid19.android.app.remote

import uk.nhs.nhsx.covid19.android.app.common.TranslatedString
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.Symptom
import uk.nhs.nhsx.covid19.android.app.remote.data.QuestionnaireResponse

class MockQuestionnaireApi : QuestionnaireApi {
    override suspend fun fetchQuestionnaire(): QuestionnaireResponse {
        val temperatureTitle = TranslatedString("A high temperature (fever)")
        val temperatureDescription =
            TranslatedString("This means that you feel hot to touch on your chest or back (you do not need to measure your temperature).")

        val coughTitle = TranslatedString("A new continuous cough")
        val coughDescription =
            TranslatedString("This means coughing a lot for more than an hour, or 3 or more coughing episodes in 24 hours (if you usually have a cough, it may be worse than usual).")

        val anosmiaTitle = TranslatedString("A new loss or change to your sense of smell or taste")
        val anosmiaDescription =
            TranslatedString("This means you have noticed you cannot smell or taste anything, or things smell or taste different to normal.")

        val dummyTitle = TranslatedString("Dummy")
        val dummyDescription =
            TranslatedString("Dummy and not related with coronavirus")

        return QuestionnaireResponse(
            symptoms = listOf(
                Symptom(
                    temperatureTitle,
                    temperatureDescription,
                    riskWeight = 1.0
                ),
                Symptom(
                    coughTitle,
                    coughDescription,
                    riskWeight = 1.0
                ),
                Symptom(
                    anosmiaTitle,
                    anosmiaDescription,
                    riskWeight = 1.0
                ),
                Symptom(
                    dummyTitle,
                    dummyDescription,
                    riskWeight = 0.0
                )
            ),
            riskThreshold = 0.5f,
            symptomsOnsetWindowDays = 5
        )
    }
}
