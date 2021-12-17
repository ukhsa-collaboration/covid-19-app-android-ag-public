package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EvaluateTestingAdviceToShow.TestingAdviceToShow
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EvaluateTestingAdviceToShow.TestingAdviceToShow.UnknownExposureDate
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra.FULLY_VACCINATED
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra.MEDICALLY_EXEMPT
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra.MINOR
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra.NONE
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.NavigationTarget.BookPcrTest
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.NavigationTarget.Home
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.ViewState.AlreadyIsolating
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.ViewState.NewlyIsolating
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.ViewState.NotIsolatingAsFullyVaccinated
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.ViewState.NotIsolatingAsMedicallyExempt
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.ViewState.NotIsolatingAsMinor
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.Clock

class RiskyContactIsolationAdviceViewModel @AssistedInject constructor(
    private val isolationStateMachine: IsolationStateMachine,
    @Assisted private val optOutOfContactIsolation: OptOutOfContactIsolationExtra,
    private val evaluateTestingAdviceToShow: EvaluateTestingAdviceToShow,
    private val localAuthorityPostCodeProvider: LocalAuthorityPostCodeProvider,
    private val clock: Clock,
) : ViewModel() {

    private val viewStateLiveData = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewStateLiveData

    private val navigationTargetLiveData = MutableLiveData<NavigationTarget>()
    fun navigationTarget(): LiveData<NavigationTarget> = navigationTargetLiveData

    init {
        viewModelScope.launch {
            evaluateViewState()
        }
    }

    private suspend fun evaluateViewState() {
        val testingAdviceToShow = evaluateTestingAdviceToShow(clock)
        if (testingAdviceToShow == UnknownExposureDate) {
            navigationTargetLiveData.postValue(Home)
            return
        }
        val state = isolationStateMachine.readLogicalState()
        val isAlreadyIsolating = state.isActiveIndexCase(clock)
        val remainingDaysInIsolation = isolationStateMachine.remainingDaysInIsolation(state).toInt()
        val viewState = if (isAlreadyIsolating) {
            AlreadyIsolating(remainingDaysInIsolation, testingAdviceToShow)
        } else {
            when (optOutOfContactIsolation) {
                MINOR -> NotIsolatingAsMinor(localAuthorityPostCodeProvider.requirePostCodeDistrict().supportedCountry!!, testingAdviceToShow)
                FULLY_VACCINATED -> NotIsolatingAsFullyVaccinated(
                    localAuthorityPostCodeProvider.requirePostCodeDistrict().supportedCountry!!,
                    testingAdviceToShow
                )
                MEDICALLY_EXEMPT -> NotIsolatingAsMedicallyExempt
                NONE -> NewlyIsolating(remainingDaysInIsolation, testingAdviceToShow)
            }
        }
        viewStateLiveData.postValue(viewState)
    }

    fun onBackToHomeClicked() {
        navigationTargetLiveData.postValue(Home)
    }

    fun onBookPcrTestTestClicked() {
        navigationTargetLiveData.postValue(BookPcrTest)
    }

    sealed class ViewState {
        data class NewlyIsolating(val remainingDaysInIsolation: Int, val testingAdviceToShow: TestingAdviceToShow) :
            ViewState()

        data class AlreadyIsolating(val remainingDaysInIsolation: Int, val testingAdviceToShow: TestingAdviceToShow) :
            ViewState()

        data class NotIsolatingAsFullyVaccinated(
            val country: SupportedCountry,
            val testingAdviceToShow: TestingAdviceToShow
        ) : ViewState()

        data class NotIsolatingAsMinor(val country: SupportedCountry, val testingAdviceToShow: TestingAdviceToShow) :
            ViewState()
        object NotIsolatingAsMedicallyExempt : ViewState()
    }

    sealed class NavigationTarget {
        object Home : NavigationTarget()
        object BookPcrTest : NavigationTarget()
    }

    @AssistedFactory
    interface Factory {
        fun create(optOutOfContactIsolationExtra: OptOutOfContactIsolationExtra): RiskyContactIsolationAdviceViewModel
    }
}
