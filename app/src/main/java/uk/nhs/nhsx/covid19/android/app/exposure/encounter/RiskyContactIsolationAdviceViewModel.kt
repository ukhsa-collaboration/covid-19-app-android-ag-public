package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
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
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.Clock

class RiskyContactIsolationAdviceViewModel @AssistedInject constructor(
    private val isolationStateMachine: IsolationStateMachine,
    @Assisted private val optOutOfContactIsolation: OptOutOfContactIsolationExtra,
    private val clock: Clock
) : ViewModel() {

    private val viewStateLiveData = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewStateLiveData

    private val navigationTargetLiveData = MutableLiveData<NavigationTarget>()
    fun navigationTarget(): LiveData<NavigationTarget> = navigationTargetLiveData

    init {
        val viewState = evaluateViewState()
        viewStateLiveData.postValue(viewState)
    }

    private fun evaluateViewState(): ViewState {
        val state = isolationStateMachine.readLogicalState()
        val isAlreadyIsolating = state.isActiveIndexCase(clock)
        val remainingDaysInIsolation = isolationStateMachine.remainingDaysInIsolation(state).toInt()
        return if (isAlreadyIsolating) {
            AlreadyIsolating(remainingDaysInIsolation)
        } else {
            when (optOutOfContactIsolation) {
                MINOR -> NotIsolatingAsMinor
                FULLY_VACCINATED -> NotIsolatingAsFullyVaccinated
                MEDICALLY_EXEMPT -> NotIsolatingAsMedicallyExempt
                NONE -> NewlyIsolating(remainingDaysInIsolation)
            }
        }
    }

    fun onBackToHomeClicked() {
        navigationTargetLiveData.postValue(Home)
    }

    fun onBookPcrTestTestClicked() {
        navigationTargetLiveData.postValue(BookPcrTest)
    }

    sealed class ViewState {
        data class NewlyIsolating(val remainingDaysInIsolation: Int) : ViewState()
        data class AlreadyIsolating(val remainingDaysInIsolation: Int) : ViewState()
        object NotIsolatingAsFullyVaccinated : ViewState()
        object NotIsolatingAsMinor : ViewState()
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
