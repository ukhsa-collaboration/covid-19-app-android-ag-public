package uk.nhs.nhsx.covid19.android.app.status.guidancehub

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class GuidanceHubViewModel @Inject constructor() : ViewModel() {
    private val navigationTargetLiveData = SingleLiveEvent<NavigationTarget>()
    fun navigationTarget(): LiveData<NavigationTarget> = navigationTargetLiveData

    fun itemOneClicked() {
        navigationTargetLiveData.postValue(NavigationTarget.ExternalLink(urlRes = R.string.covid_guidance_hub_england_button_one_url))
    }

    fun itemTwoClicked() {
        navigationTargetLiveData.postValue(NavigationTarget.ExternalLink(urlRes = R.string.covid_guidance_hub_england_button_two_url))
    }

    fun itemThreeClicked() {
        navigationTargetLiveData.postValue(NavigationTarget.ExternalLink(urlRes = R.string.covid_guidance_hub_england_button_three_url))
    }

    fun itemFourClicked() {
        navigationTargetLiveData.postValue(NavigationTarget.ExternalLink(urlRes = R.string.covid_guidance_hub_england_button_four_url))
    }

    fun itemFiveClicked() {
        navigationTargetLiveData.postValue(NavigationTarget.ExternalLink(urlRes = R.string.covid_guidance_hub_england_button_five_url))
    }

    fun itemSixClicked() {
        navigationTargetLiveData.postValue(NavigationTarget.ExternalLink(urlRes = R.string.covid_guidance_hub_england_button_six_url))
    }

    fun itemSevenClicked() {
        navigationTargetLiveData.postValue(NavigationTarget.ExternalLink(urlRes = R.string.covid_guidance_hub_england_button_seven_url))
    }

    fun itemEightClicked() {
        navigationTargetLiveData.postValue(NavigationTarget.ExternalLink(urlRes = R.string.covid_guidance_hub_england_button_eight_url))
    }

    sealed class NavigationTarget {
        data class ExternalLink(@StringRes val urlRes: Int) : NavigationTarget()
    }
}
