package uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.AcknowledgeRiskyContact
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.GetAgeLimitBeforeEncounter
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.GetLastDoseDateLimit
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.OptOutOfContactIsolation
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.ExposureNotificationReviewViewModel.NavigationTarget.AgeLimit
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.ExposureNotificationReviewViewModel.NavigationTarget.IsolationAdvice
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.ExposureNotificationReviewViewModel.NavigationTarget.VaccinationStatus
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionnaireOutcome.NotExempt
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import java.time.LocalDate

class ExposureNotificationReviewViewModel @AssistedInject constructor(
    private val getAgeLimitBeforeEncounter: GetAgeLimitBeforeEncounter,
    private val getLastDoseDateLimit: GetLastDoseDateLimit,
    private val optOutOfContactIsolation: OptOutOfContactIsolation,
    private val acknowledgeRiskyContact: AcknowledgeRiskyContact,
    @Assisted val reviewData: ReviewData
) : ViewModel() {

    private val navigationLiveData = SingleLiveEvent<NavigationTarget>()
    val navigationTarget: LiveData<NavigationTarget> = navigationLiveData

    val viewState = liveData {
        emit(
            ViewState(
                ageLimitResponse = reviewData.ageResponse,
                vaccinationStatusResponse = reviewData.vaccinationStatusResponses,
                ageLimitDate = getAgeLimitBeforeEncounter()!!,
                lastDoseDateLimit = getLastDoseDateLimit()!!
            )
        )
    }

    fun onSubmitClicked() {
        if (reviewData.questionnaireOutcome != NotExempt) {
            optOutOfContactIsolation()
        }
        acknowledgeRiskyContact()
        navigationLiveData.postValue(IsolationAdvice(questionnaireOutcome = reviewData.questionnaireOutcome))
    }

    fun onChangeAgeLimitResponseClicked() {
        navigationLiveData.postValue(AgeLimit)
    }

    fun onChangeVaccinationStatusResponseClicked() {
        navigationLiveData.postValue(VaccinationStatus)
    }

    @AssistedFactory
    interface Factory {
        fun create(reviewData: ReviewData): ExposureNotificationReviewViewModel
    }

    data class ViewState(
        val ageLimitResponse: OptOutResponseEntry,
        val vaccinationStatusResponse: List<OptOutResponseEntry>,
        val ageLimitDate: LocalDate,
        val lastDoseDateLimit: LocalDate
    )

    sealed class NavigationTarget {
        object AgeLimit : NavigationTarget()
        object VaccinationStatus : NavigationTarget()
        data class IsolationAdvice(val questionnaireOutcome: QuestionnaireOutcome) : NavigationTarget()
    }
}
