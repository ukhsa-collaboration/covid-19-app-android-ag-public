package uk.nhs.nhsx.covid19.android.app.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class WelcomeViewModel @Inject constructor() : ViewModel() {

    private val showDialogLiveData: MutableLiveData<Boolean> = MutableLiveData()
    fun getShowDialog(): LiveData<Boolean> = showDialogLiveData

    private val showHowAppWorksScreen = SingleLiveEvent<Unit>()
    fun showHowAppWorksScreen(): LiveData<Unit> = showHowAppWorksScreen

    fun onConfirmOnboardingClicked() {
        showDialogLiveData.postValue(true)
    }

    fun onDialogDismissed() {
        showDialogLiveData.postValue(false)
    }

    fun onPositiveButtonClicked() {
        showHowAppWorksScreen.call()
    }
}
