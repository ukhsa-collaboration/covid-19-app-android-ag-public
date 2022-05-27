package uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import uk.nhs.nhsx.covid19.android.app.common.TranslatableString

@Parcelize
data class SymptomsCheckerQuestions(
    val nonCardinalSymptoms: NonCardinalSymptoms?,
    val cardinalSymptom: CardinalSymptom?,
    val howDoYouFeelSymptom: HowDoYouFeelSymptom?
) : Parcelable

@Parcelize
data class NonCardinalSymptoms(
    val title: TranslatableString,
    val nonCardinalSymptomsText: TranslatableString,
    val isChecked: Boolean?
) : Parcelable

@Parcelize
data class CardinalSymptom(val title: TranslatableString, val isChecked: Boolean?) : Parcelable

@Parcelize
data class HowDoYouFeelSymptom(val isChecked: Boolean?) : Parcelable

enum class SymptomCheckerAdviceResult {
    TRY_TO_STAY_AT_HOME,
    CONTINUE_NORMAL_ACTIVITIES
}
