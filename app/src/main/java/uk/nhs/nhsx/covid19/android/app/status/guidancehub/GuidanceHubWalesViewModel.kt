package uk.nhs.nhsx.covid19.android.app.status.guidancehub

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.status.NewFunctionalityLabelProvider
import uk.nhs.nhsx.covid19.android.app.status.guidancehub.GuidanceHubWalesViewModel.NewLabelViewState.Hidden
import uk.nhs.nhsx.covid19.android.app.status.guidancehub.GuidanceHubWalesViewModel.NewLabelViewState.Visible
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class GuidanceHubWalesViewModel @Inject constructor(
    private val newFunctionalityLabelProvider: NewFunctionalityLabelProvider
) : ViewModel() {
    private val navigationTargetLiveData = SingleLiveEvent<NavigationTarget>()
    fun navigationTarget(): LiveData<NavigationTarget> = navigationTargetLiveData

    private val newLabelViewState = MutableLiveData<NewLabelViewState>()
    fun newLabelViewState(): LiveData<NewLabelViewState> = newLabelViewState

    fun onCreate() {
        viewModelScope.launch {
            newLabelViewState.postValue(
                if (newFunctionalityLabelProvider.hasInteractedWithLongCovidWalesNewLabel) Hidden else Visible
            )
        }
    }

    fun itemOneClicked() {
        navigationTargetLiveData.postValue(NavigationTarget.ExternalLink(urlRes = R.string.covid_guidance_hub_wales_button_one_url))
    }

    fun itemTwoClicked() {
        navigationTargetLiveData.postValue(NavigationTarget.ExternalLink(urlRes = R.string.covid_guidance_hub_wales_button_two_url))
    }

    fun itemThreeClicked() {
        navigationTargetLiveData.postValue(NavigationTarget.ExternalLink(urlRes = R.string.covid_guidance_hub_wales_button_three_url))
    }

    fun itemFourClicked() {
        navigationTargetLiveData.postValue(NavigationTarget.ExternalLink(urlRes = R.string.covid_guidance_hub_wales_button_four_url))
    }

    fun itemFiveClicked() {
        navigationTargetLiveData.postValue(NavigationTarget.ExternalLink(urlRes = R.string.covid_guidance_hub_wales_button_five_url))
    }

    fun itemSixClicked() {
        navigationTargetLiveData.postValue(NavigationTarget.ExternalLink(urlRes = R.string.covid_guidance_hub_wales_button_six_url))
        onNewLabelItemInteraction()
    }

    fun itemSevenClicked() {
        navigationTargetLiveData.postValue(NavigationTarget.ExternalLink(urlRes = R.string.covid_guidance_hub_wales_button_seven_url))
    }

    fun itemEightClicked() {
        navigationTargetLiveData.postValue(NavigationTarget.ExternalLink(urlRes = R.string.covid_guidance_hub_wales_button_eight_url))
    }

    private fun onNewLabelItemInteraction() {
        newFunctionalityLabelProvider.hasInteractedWithLongCovidWalesNewLabel = true
        newLabelViewState.postValue(Hidden)
    }

    sealed class NavigationTarget {
        data class ExternalLink(@StringRes val urlRes: Int) : NavigationTarget()
    }

    sealed class NewLabelViewState {
        object Visible : NewLabelViewState()
        object Hidden : NewLabelViewState()
    }
}
