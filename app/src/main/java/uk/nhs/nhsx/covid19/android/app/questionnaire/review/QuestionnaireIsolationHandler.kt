package uk.nhs.nhsx.covid19.android.app.questionnaire.review

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireAndStartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.IndexCaseThenHasSymptomsDidUpdateIsolation
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.IndexCaseThenHasSymptomsNoEffectOnIsolation
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.IndexCaseThenNoSymptoms
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.NoIndexCaseThenIsolationDueToSelfAssessment
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.NoIndexCaseThenIsolationDueToSelfAssessmentNoTimerWales
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.NoIndexCaseThenSelfAssessmentNoImpactOnIsolation
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.NoIsolationSymptomAdvice.NoSymptoms
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.QuestionnaireIsolationHandler.SymptomsSelectionOutcome.CardinalSymptomsSelected
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.QuestionnaireIsolationHandler.SymptomsSelectionOutcome.NoSymptomsSelected
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.QuestionnaireIsolationHandler.SymptomsSelectionOutcome.OnlyNonCardinalSymptomsSelected
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.CannotRememberDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.ExplicitDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.NotStated
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.Symptom
import uk.nhs.nhsx.covid19.android.app.state.GetLatestConfiguration
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnPositiveSelfAssessment
import java.lang.Long
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import javax.inject.Inject

class QuestionnaireIsolationHandler @Inject constructor(
    private val isolationStateMachine: IsolationStateMachine,
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    private val riskCalculator: RiskCalculator,
    private val clock: Clock,
    private val getLatestConfiguration: GetLatestConfiguration
) {
    fun computeAdvice(
        riskThreshold: Float,
        selectedSymptoms: List<Symptom>,
        onsetDate: SelectedDate,
        isSymptomaticSelfIsolationEnabled: Boolean,
    ): SymptomAdvice {
        val symptomsSelectionOutcome = calculateSymptomsSelectionOutcome(selectedSymptoms, riskThreshold)

        val isolationState = isolationStateMachine.readLogicalState()
        return if (isolationState.hasActivePositiveTestResult(clock)) {
            symptomAdviceWhenIsolatingDueToPositiveTestResult(symptomsSelectionOutcome, onsetDate)
        } else {
            when (isSymptomaticSelfIsolationEnabled) {
                true -> symptomAdviceWhenNotIsolatingDueToPositiveTestResultSymptomaticSelfIsolationEnabled(symptomsSelectionOutcome, onsetDate)
                false -> symptomAdviceWhenNotIsolatingDueToPositiveTestResultSymptomaticSelfIsolationDisabled(symptomsSelectionOutcome, onsetDate)
            }
        }
    }

    private fun symptomAdviceWhenNotIsolatingDueToPositiveTestResultSymptomaticSelfIsolationDisabled(
        symptomsSelectionOutcome: SymptomsSelectionOutcome,
        onsetDate: SelectedDate
    ): SymptomAdvice {
        val hadCardinalSymptoms = (symptomsSelectionOutcome == CardinalSymptomsSelected)

        val isolationState = isolationStateMachine.readLogicalState()

        val isInActiveIsolation = isolationState.isActiveIsolation(clock)

        return when {
            isInActiveIsolation -> {
                NoIndexCaseThenSelfAssessmentNoImpactOnIsolation(isolationStateMachine.remainingDaysInIsolation().toInt())
            }
            hadCardinalSymptoms -> {
                val startDate: LocalDate =
                    when (onsetDate) {
                        CannotRememberDate, NotStated -> LocalDate.now(clock)
                        is ExplicitDate -> onsetDate.date
                    }
                val expiryDate = startDate.plusDays(getLatestConfiguration().indexCaseSinceSelfDiagnosisOnset.toLong())
                val remainingDaysInIsolation = daysUntil(expiryDate).toInt()
                NoIndexCaseThenIsolationDueToSelfAssessmentNoTimerWales(remainingDaysInIsolation)
            }
            else -> NoSymptoms
        }
    }

    private fun calculateSymptomsSelectionOutcome(
        selectedSymptoms: List<Symptom>,
        riskThreshold: Float
    ): SymptomsSelectionOutcome {
        val noSymptomsSelected = selectedSymptoms.isEmpty()
        return when {
            noSymptomsSelected -> NoSymptomsSelected
            riskCalculator.isRiskAboveThreshold(selectedSymptoms, riskThreshold) -> CardinalSymptomsSelected
            else -> OnlyNonCardinalSymptomsSelected
        }
    }

    private fun symptomAdviceWhenNotIsolatingDueToPositiveTestResultSymptomaticSelfIsolationEnabled(
        symptomsSelectionOutcome: SymptomsSelectionOutcome,
        onsetDate: SelectedDate
    ): SymptomAdvice {
        when (symptomsSelectionOutcome) {
            CardinalSymptomsSelected -> {
                isolationStateMachine.processEvent(OnPositiveSelfAssessment(onsetDate))
                analyticsEventProcessor.track(CompletedQuestionnaireAndStartedIsolation)
            }
            NoSymptomsSelected, OnlyNonCardinalSymptomsSelected -> {}
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
        symptomsSelectionOutcome: SymptomsSelectionOutcome,
        onsetDate: SelectedDate
    ): SymptomAdvice {
        return when (symptomsSelectionOutcome) {
            CardinalSymptomsSelected -> {
                isolationStateMachine.processEvent(OnPositiveSelfAssessment(onsetDate))

                val isolationState = isolationStateMachine.readLogicalState()

                val selfAssessmentStored =
                    (isolationState as? PossiblyIsolating)?.remembersIndexCaseWithSelfAssessment() == true

                if (selfAssessmentStored) {
                    IndexCaseThenHasSymptomsDidUpdateIsolation(isolationStateMachine.remainingDaysInIsolation().toInt())
                } else {
                    IndexCaseThenHasSymptomsNoEffectOnIsolation
                }
            }
            NoSymptomsSelected, OnlyNonCardinalSymptomsSelected -> IndexCaseThenNoSymptoms
        }
    }

    private fun daysUntil(date: LocalDate) = Long.max(0, DAYS.between(LocalDate.now(clock), date))

    sealed class SymptomsSelectionOutcome {
        object NoSymptomsSelected : SymptomsSelectionOutcome()
        object CardinalSymptomsSelected : SymptomsSelectionOutcome()
        object OnlyNonCardinalSymptomsSelected : SymptomsSelectionOutcome()
    }
}

interface SymptomAdvice : Parcelable

sealed class IsolationSymptomAdvice : SymptomAdvice {
    @Parcelize
    data class NoIndexCaseThenIsolationDueToSelfAssessment(val remainingDaysInIsolation: Int) : IsolationSymptomAdvice()

    @Parcelize
    data class NoIndexCaseThenIsolationDueToSelfAssessmentNoTimerWales(val remainingDaysInIsolation: Int) : IsolationSymptomAdvice()

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
