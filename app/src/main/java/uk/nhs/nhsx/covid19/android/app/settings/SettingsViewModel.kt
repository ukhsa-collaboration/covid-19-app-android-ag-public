package uk.nhs.nhsx.covid19.android.app.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.covid19.android.app.SupportedLanguage
import uk.nhs.nhsx.covid19.android.app.common.ApplicationLocaleProvider
import javax.inject.Inject

class SettingsViewModel @Inject constructor(
    private val applicationLocaleProvider: ApplicationLocaleProvider
) : ViewModel() {

    private val viewState = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewState

    fun loadSettings() {
        val language = applicationLocaleProvider.getUserSelectedLanguage()
            ?: applicationLocaleProvider.getSystemLanguage()
        viewState.postValue(ViewState(language))
    }

    data class ViewState(val language: SupportedLanguage)
}
