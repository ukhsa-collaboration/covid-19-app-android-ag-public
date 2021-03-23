package uk.nhs.nhsx.covid19.android.app.settings

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.covid19.android.app.SupportedLanguage
import uk.nhs.nhsx.covid19.android.app.analytics.SubmittedOnboardingAnalyticsProvider
import uk.nhs.nhsx.covid19.android.app.common.ApplicationLocaleProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class SettingsViewModel @Inject constructor(
    private val applicationLocaleProvider: ApplicationLocaleProvider,
    private val venuesStorage: VisitedVenuesStorage,
    private val stateMachine: IsolationStateMachine,
    private val sharedPreferences: SharedPreferences,
    private val submittedOnboardingAnalyticsProvider: SubmittedOnboardingAnalyticsProvider
) : ViewModel() {

    private val viewState = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewState

    private val allUserDataDeletedLiveData: MutableLiveData<Unit> = SingleLiveEvent()
    fun getAllUserDataDeleted(): LiveData<Unit> = allUserDataDeletedLiveData

    fun loadSettings() {
        val language = applicationLocaleProvider.getUserSelectedLanguage()
            ?: applicationLocaleProvider.getSystemLanguage()
        viewState.postValue(ViewState(language))
    }

    data class ViewState(
        val language: SupportedLanguage,
        val showDeleteAllDataDialog: Boolean = false
    )

    fun onDeleteAllUserDataClicked() {
        viewState.postValue(viewState.value!!.copy(showDeleteAllDataDialog = true))
    }

    fun deleteAllUserData() {
        val submittedOnboardingAnalytics = submittedOnboardingAnalyticsProvider.value
        sharedPreferences.edit().clear().apply()
        submittedOnboardingAnalyticsProvider.value = submittedOnboardingAnalytics
        stateMachine.reset()
        venuesStorage.removeAllVenueVisits()
        viewState.postValue(viewState.value!!.copy(showDeleteAllDataDialog = false))
        allUserDataDeletedLiveData.postValue(Unit)
    }

    fun onDialogDismissed() {
        viewState.postValue(viewState.value!!.copy(showDeleteAllDataDialog = false))
    }
}
