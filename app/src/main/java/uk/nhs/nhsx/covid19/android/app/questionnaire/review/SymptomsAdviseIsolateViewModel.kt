package uk.nhs.nhsx.covid19.android.app.questionnaire.review

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.NoIndexCaseThenIsolationDueToSelfAssessmentAdvice.AdviceForEngland
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.NoIndexCaseThenIsolationDueToSelfAssessmentAdvice.AdviceForWales
import javax.inject.Inject

class SymptomsAdviseIsolateViewModel @Inject constructor(
    private val localAuthorityPostCodeProvider: LocalAuthorityPostCodeProvider
) : ViewModel() {

    private val viewState = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewState

    fun handleLocalAuthorityAdvice() {
        viewModelScope.launch {
            val country =
                when (localAuthorityPostCodeProvider.requirePostCodeDistrict() == ENGLAND) {
                    true -> AdviceForEngland
                    false -> AdviceForWales
                }
            viewState.postValue(
                ViewState(country)
            )
        }
    }

    data class ViewState(
        val country: NoIndexCaseThenIsolationDueToSelfAssessmentAdvice
    )
}

sealed class NoIndexCaseThenIsolationDueToSelfAssessmentAdvice {
    object AdviceForEngland : NoIndexCaseThenIsolationDueToSelfAssessmentAdvice()
    object AdviceForWales : NoIndexCaseThenIsolationDueToSelfAssessmentAdvice()
}
