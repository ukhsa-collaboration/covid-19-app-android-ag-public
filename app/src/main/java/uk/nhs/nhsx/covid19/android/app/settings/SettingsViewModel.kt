package uk.nhs.nhsx.covid19.android.app.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.covid19.android.app.SupportedLanguage
import uk.nhs.nhsx.covid19.android.app.common.ApplicationLocaleProvider
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class SettingsViewModel @Inject constructor(
    private val applicationLocaleProvider: ApplicationLocaleProvider,
    private val deleteAllUserData: DeleteAllUserData
) : ViewModel() {

    private val viewState = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewState

    private val allUserDataDeletedLiveData = SingleLiveEvent<Unit>()
    fun getAllUserDataDeleted(): LiveData<Unit> = allUserDataDeletedLiveData

    fun loadSettings() {
        val language = applicationLocaleProvider.getUserSelectedLanguage()
            ?: applicationLocaleProvider.getDefaultSystemLanguage()
        viewState.postValue(ViewState(language))
    }

    data class ViewState(
        val language: SupportedLanguage,
        val showDeleteAllDataDialog: Boolean = false
    )

    fun onDeleteAllUserDataClicked() {
        viewState.postValue(viewState.value!!.copy(showDeleteAllDataDialog = true))
    }

    fun dataDeletionConfirmed() {
        deleteAllUserData()
        viewState.postValue(viewState.value!!.copy(showDeleteAllDataDialog = false))
        allUserDataDeletedLiveData.call()
    }

    fun onDialogDismissed() {
        viewState.postValue(viewState.value!!.copy(showDeleteAllDataDialog = false))
    }
}
