package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EvaluateTestingAdviceToShow.TestingAdviceToShow
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EvaluateTestingAdviceToShow.TestingAdviceToShow.UnknownExposureDate
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra.FULLY_VACCINATED
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra.MEDICALLY_EXEMPT
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra.MINOR
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra.NONE
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceViewModel.NavigationTarget.BookLfdTest
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
            val country = localAuthorityPostCodeProvider.requirePostCodeDistrict().supportedCountry!!
            when (optOutOfContactIsolation) {
                MINOR -> NotIsolatingAsMinor(country, testingAdviceToShow)
                FULLY_VACCINATED -> NotIsolatingAsFullyVaccinated(country, testingAdviceToShow)
                MEDICALLY_EXEMPT -> NotIsolatingAsMedicallyExempt
                NONE -> NewlyIsolating(country, remainingDaysInIsolation, testingAdviceToShow)
            }
        }
        viewStateLiveData.postValue(viewState)
    }

    fun onBackToHomeClicked() {
        navigationTargetLiveData.postValue(Home)
    }

    fun onBookPcrTestClicked() {
        navigationTargetLiveData.postValue(BookPcrTest)
    }

    fun onBookLfdTestClicked() {
        navigationTargetLiveData.postValue(BookLfdTest(R.string.contact_case_start_isolation_book_lfd_test_url))
    }

    sealed class ViewState {
        data class NewlyIsolating(
            val country: SupportedCountry,
            val remainingDaysInIsolation: Int,
            val testingAdviceToShow: TestingAdviceToShow
        ) : ViewState()

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
        data class BookLfdTest(@StringRes val url: Int) : NavigationTarget()
    }

    @AssistedFactory
    interface Factory {
        fun create(optOutOfContactIsolationExtra: OptOutOfContactIsolationExtra): RiskyContactIsolationAdviceViewModel
    }
}
