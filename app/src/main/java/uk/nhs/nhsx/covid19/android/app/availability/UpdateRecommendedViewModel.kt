package uk.nhs.nhsx.covid19.android.app.availability

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import javax.inject.Inject

class UpdateRecommendedViewModel @Inject constructor(
    private val appAvailabilityProvider: AppAvailabilityProvider
) : ViewModel() {

    private val recommendationInfoLiveData = MutableLiveData<RecommendationInfo>()

    fun observeRecommendationInfo(): LiveData<RecommendationInfo> = recommendationInfoLiveData

    fun fetchRecommendationInfo() {
        appAvailabilityProvider.appAvailability?.let {
            recommendationInfoLiveData.postValue(
                RecommendationInfo(
                    title = it.recommendedAppVersion.title.translate(),
                    description = it.recommendedAppVersion.description.translate()
                )
            )
        }
    }

    data class RecommendationInfo(val title: String, val description: String)
}
