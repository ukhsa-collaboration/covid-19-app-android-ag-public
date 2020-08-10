package uk.nhs.nhsx.covid19.android.app.onboarding.postcode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.OnboardingCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import java.util.Locale
import javax.inject.Inject

class PostCodeViewModel @Inject constructor(
    private val postCodeValidator: PostCodeValidator,
    private val postCodePrefs: PostCodeProvider,
    private val analyticsEventProcessor: AnalyticsEventProcessor
) : ViewModel() {

    private val postCodeLiveData = MutableLiveData<PostCodeViewState>()

    fun viewState(): LiveData<PostCodeViewState> = postCodeLiveData

    fun validate(postCode: String) {
        viewModelScope.launch {
            val postCodeUpperCased = postCode.toUpperCase(Locale.UK)
            if (postCodeValidator.validate(postCodeUpperCased)) {
                postCodePrefs.value = postCodeUpperCased
                postCodeLiveData.postValue(PostCodeViewState.Valid)
                analyticsEventProcessor.track(OnboardingCompletion)
            } else {
                postCodeLiveData.postValue(PostCodeViewState.Invalid)
            }
        }
    }

    sealed class PostCodeViewState {
        object Valid : PostCodeViewState()
        object Invalid : PostCodeViewState()
    }
}
