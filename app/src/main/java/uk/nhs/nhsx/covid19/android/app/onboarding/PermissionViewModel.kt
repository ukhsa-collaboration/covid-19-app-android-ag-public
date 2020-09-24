package uk.nhs.nhsx.covid19.android.app.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.OnboardingCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class PermissionViewModel @Inject constructor(
    private val onboardingCompletedProvider: OnboardingCompletedProvider,
    private val analyticsEventProcessor: AnalyticsEventProcessor
) : ViewModel() {

    private val onboardingCompletedLiveData = SingleLiveEvent<Unit>()
    fun onboardingCompleted(): LiveData<Unit> = onboardingCompletedLiveData

    fun setOnboardingCompleted() {
        viewModelScope.launch {
            onboardingCompletedProvider.value = true

            analyticsEventProcessor.track(OnboardingCompletion)

            onboardingCompletedLiveData.postCall()
        }
    }
}
