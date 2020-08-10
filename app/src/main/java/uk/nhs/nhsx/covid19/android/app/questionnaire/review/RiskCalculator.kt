package uk.nhs.nhsx.covid19.android.app.questionnaire.review

import androidx.annotation.VisibleForTesting
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.Symptom
import javax.inject.Inject

class RiskCalculator @Inject constructor() {

    fun isRiskAboveThreshold(checkedSymptoms: List<Symptom>, riskThreshold: Float): Boolean {
        return calculateRisk(checkedSymptoms) >= riskThreshold
    }

    @VisibleForTesting
    internal fun calculateRisk(checkedSymptoms: List<Symptom>): Double {
        return checkedSymptoms.sumByDouble { it.riskWeight }
    }
}
