package uk.nhs.nhsx.covid19.android.app.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import javax.inject.Inject

class WelcomeViewModel @Inject constructor() : ViewModel() {

    private val showDialogLiveData: MutableLiveData<Boolean> = MutableLiveData()
    fun getShowDialog(): LiveData<Boolean> = showDialogLiveData

    fun onConfirmOnboardingClicked() {
        showDialogLiveData.postValue(true)
    }

    fun onDialogDismissed() {
        showDialogLiveData.postValue(false)
    }
}
