package uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker

import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceResult.CONTINUE_NORMAL_ACTIVITIES
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceResult.TRY_TO_STAY_AT_HOME
import javax.inject.Inject

class SymptomCheckerAdviceHandler @Inject constructor() {
    operator fun invoke(questions: SymptomsCheckerQuestions): SymptomCheckerAdviceResult? {
        return if (questions.cardinalSymptom?.isChecked == null || questions.howDoYouFeelSymptom?.isChecked == null || questions.nonCardinalSymptoms?.isChecked == null) {
            null
        } else {
            val hasCheckedCardinalSymptom = questions.cardinalSymptom.isChecked
            val hasCheckedHowDoYouFeelSymptom = questions.howDoYouFeelSymptom.isChecked

            when {
                !hasCheckedCardinalSymptom && hasCheckedHowDoYouFeelSymptom -> CONTINUE_NORMAL_ACTIVITIES
                else -> TRY_TO_STAY_AT_HOME
            }
        }
    }
}
