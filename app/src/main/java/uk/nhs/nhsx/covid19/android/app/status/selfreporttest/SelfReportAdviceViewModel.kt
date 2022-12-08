package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportAdviceViewModel.ResultAdvice.HasNotReportedIsolate
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportAdviceViewModel.ResultAdvice.HasNotReportedNoNeedToIsolate
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportAdviceViewModel.ResultAdvice.HasReportedIsolate
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportAdviceViewModel.ResultAdvice.HasReportedNoNeedToIsolate
import java.time.Clock
import java.time.LocalDate

class SelfReportAdviceViewModel @AssistedInject constructor(
    private val isolationStateMachine: IsolationStateMachine,
    private val clock: Clock,
    private val localAuthorityPostCodeProvider: LocalAuthorityPostCodeProvider,
    @Assisted private val reportedTest: Boolean
) : ViewModel() {

    private val viewStateLiveData = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewStateLiveData

    init {
        viewModelScope.launch {
            val isolationState = isolationStateMachine.readLogicalState()
            val isolationEndDate = if (isolationState is PossiblyIsolating && isolationState.isActiveIsolation(clock)) {
                isolationState.expiryDate
            } else {
                null
            }

            val resultAdvice = when (isolationEndDate != null) {
                true -> getIsolationViewState(isolationEndDate)
                else -> getNonIsolationViewState()
            }
            viewStateLiveData.postValue(ViewState(resultAdvice = resultAdvice,
                country = localAuthorityPostCodeProvider.requirePostCodeDistrict()))
        }
    }

    private fun getIsolationViewState(isolationEndDate: LocalDate): ResultAdvice {
        val currentDate = LocalDate.now(clock)
        return if (reportedTest) {
            HasReportedIsolate(currentDate = currentDate, isolationEndDate = isolationEndDate)
        } else {
            HasNotReportedIsolate(currentDate = currentDate, isolationEndDate = isolationEndDate)
        }
    }

    private fun getNonIsolationViewState(): ResultAdvice {
        return if (reportedTest) {
            HasReportedNoNeedToIsolate
        } else {
            HasNotReportedNoNeedToIsolate
        }
    }

    data class ViewState(val resultAdvice: ResultAdvice, val country: PostCodeDistrict)

    sealed class ResultAdvice {
        data class HasReportedIsolate(val currentDate: LocalDate, val isolationEndDate: LocalDate) : ResultAdvice()
        data class HasNotReportedIsolate(val currentDate: LocalDate, val isolationEndDate: LocalDate) : ResultAdvice()
        object HasReportedNoNeedToIsolate : ResultAdvice()
        object HasNotReportedNoNeedToIsolate : ResultAdvice()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            reportedTest: Boolean,
        ): SelfReportAdviceViewModel
    }
}
