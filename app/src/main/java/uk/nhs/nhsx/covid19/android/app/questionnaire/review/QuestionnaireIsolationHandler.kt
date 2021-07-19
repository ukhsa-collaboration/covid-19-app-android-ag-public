package uk.nhs.nhsx.covid19.android.app.questionnaire.review

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireAndStartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireButDidNotStartIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.IndexCaseThenHasSymptomsDidUpdateIsolation
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.IndexCaseThenHasSymptomsNoEffectOnIsolation
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.IndexCaseThenNoSymptoms
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.NoIndexCaseThenIsolationDueToSelfAssessment
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.NoIndexCaseThenSelfAssessmentNoImpactOnIsolation
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.NoIsolationSymptomAdvice.NoSymptoms
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.Symptom
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnPositiveSelfAssessment
import java.time.Clock
import javax.inject.Inject

class QuestionnaireIsolationHandler @Inject constructor(
    private val isolationStateMachine: IsolationStateMachine,
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    private val riskCalculator: RiskCalculator,
    private val clock: Clock
) {
    fun computeAdvice(
        riskThreshold: Float,
        selectedSymptoms: List<Symptom>,
        onsetDate: SelectedDate
    ): SymptomAdvice {
        val isolationState = isolationStateMachine.readLogicalState()
        val doesUserHaveCoronaVirusSymptoms = riskCalculator.isRiskAboveThreshold(selectedSymptoms, riskThreshold)

        return if (isolationState.hasActivePositiveTestResult(clock)) {
            symptomAdviceWhenIsolatingDueToPositiveTestResult(doesUserHaveCoronaVirusSymptoms, onsetDate)
        } else {
            symptomAdviceWhenNotIsolatingDueToPositiveTestResult(doesUserHaveCoronaVirusSymptoms, onsetDate)
        }
    }

    private fun symptomAdviceWhenNotIsolatingDueToPositiveTestResult(
        hasSymptoms: Boolean,
        onsetDate: SelectedDate
    ): SymptomAdvice {
        if (hasSymptoms) {
            isolationStateMachine.processEvent(OnPositiveSelfAssessment(onsetDate))
            analyticsEventProcessor.track(CompletedQuestionnaireAndStartedIsolation)
        } else {
            analyticsEventProcessor.track(CompletedQuestionnaireButDidNotStartIsolation)
        }

        val isolationState = isolationStateMachine.readLogicalState()

        val isInActiveIsolation = isolationState.isActiveIsolation(clock)
        val isolatingDueToSelfAssessment =
            (isolationState as? PossiblyIsolating)?.getActiveIndexCase(clock)?.isSelfAssessment() == true

        return if (isInActiveIsolation) {
            val remainingDaysInIsolation = isolationStateMachine.remainingDaysInIsolation().toInt()
            if (isolatingDueToSelfAssessment) {
                NoIndexCaseThenIsolationDueToSelfAssessment(remainingDaysInIsolation)
            } else {
                NoIndexCaseThenSelfAssessmentNoImpactOnIsolation(remainingDaysInIsolation)
            }
        } else NoSymptoms
    }

    private fun symptomAdviceWhenIsolatingDueToPositiveTestResult(
        hasSymptoms: Boolean,
        onsetDate: SelectedDate
    ): SymptomAdvice {
        return if (hasSymptoms) {
            isolationStateMachine.processEvent(OnPositiveSelfAssessment(onsetDate))

            val isolationState = isolationStateMachine.readLogicalState()

            val selfAssessmentStored =
                (isolationState as? PossiblyIsolating)?.remembersIndexCaseWithSelfAssessment() == true

            if (selfAssessmentStored) {
                IndexCaseThenHasSymptomsDidUpdateIsolation(isolationStateMachine.remainingDaysInIsolation().toInt())
            } else {
                IndexCaseThenHasSymptomsNoEffectOnIsolation
            }
        } else IndexCaseThenNoSymptoms
    }
}

interface SymptomAdvice : Parcelable

sealed class IsolationSymptomAdvice : SymptomAdvice {
    @Parcelize
    data class NoIndexCaseThenIsolationDueToSelfAssessment(val remainingDaysInIsolation: Int) : IsolationSymptomAdvice()

    @Parcelize
    data class NoIndexCaseThenSelfAssessmentNoImpactOnIsolation(val remainingDaysInIsolation: Int) :
        IsolationSymptomAdvice()

    @Parcelize
    data class IndexCaseThenHasSymptomsDidUpdateIsolation(val remainingDaysInIsolation: Int) : IsolationSymptomAdvice()

    @Parcelize
    object IndexCaseThenHasSymptomsNoEffectOnIsolation : IsolationSymptomAdvice()

    @Parcelize
    object IndexCaseThenNoSymptoms : IsolationSymptomAdvice()
}

sealed class NoIsolationSymptomAdvice : SymptomAdvice {
    @Parcelize
    object NoSymptoms : NoIsolationSymptomAdvice()
}
