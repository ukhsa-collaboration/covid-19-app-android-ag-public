package uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

sealed class QuestionnaireOutcome : Parcelable {
    @Parcelize
    object Minor : QuestionnaireOutcome()
    @Parcelize
    object FullyVaccinated : QuestionnaireOutcome()
    @Parcelize
    object MedicallyExempt : QuestionnaireOutcome()
    @Parcelize
    object NotExempt : QuestionnaireOutcome()
}
