package uk.nhs.nhsx.covid19.android.app.about

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.about.UserDataViewModel.DialogType.ConfirmDeleteAllData
import uk.nhs.nhsx.covid19.android.app.about.UserDataViewModel.DialogType.ConfirmDeleteVenueVisit
import uk.nhs.nhsx.covid19.android.app.analytics.SubmittedOnboardingAnalyticsProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodesLoader
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantTestResultProvider
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class UserDataViewModel @Inject constructor(
    private val postCodePrefs: PostCodeProvider,
    private val localAuthorityProvider: LocalAuthorityProvider,
    private val venuesStorage: VisitedVenuesStorage,
    private val stateMachine: IsolationStateMachine,
    private val relevantTestResultProvider: RelevantTestResultProvider,
    private val sharedPreferences: SharedPreferences,
    private val localAuthorityPostCodesLoader: LocalAuthorityPostCodesLoader,
    private val submittedOnboardingAnalyticsProvider: SubmittedOnboardingAnalyticsProvider
) : ViewModel() {

    private val localAuthorityText = MutableLiveData<String>()
    fun localAuthorityText(): LiveData<String> = localAuthorityText

    private val statusMachineLiveData: MutableLiveData<State> = MutableLiveData()
    fun getLastStatusMachineState(): LiveData<State> = statusMachineLiveData

    private val venueVisitsUiStateLiveData: MutableLiveData<VenueVisitsUiState> = MutableLiveData(
        VenueVisitsUiState(listOf(), isInEditMode = false)
    )

    private val venueVisitsEditModeChangedLiveData: MutableLiveData<Boolean> = SingleLiveEvent()
    fun venueVisitsEditModeChanged(): LiveData<Boolean> = venueVisitsEditModeChangedLiveData

    fun getVenueVisitsUiState(): LiveData<VenueVisitsUiState> = venueVisitsUiStateLiveData

    private val acknowledgedTestResultLiveData: MutableLiveData<AcknowledgedTestResult> = MutableLiveData()
    fun getAcknowledgedTestResult(): LiveData<AcknowledgedTestResult> = acknowledgedTestResultLiveData

    private val allUserDataDeletedLiveData: MutableLiveData<Unit> = SingleLiveEvent()
    fun getAllUserDataDeleted(): LiveData<Unit> = allUserDataDeletedLiveData

    private val showDialogLiveData: MutableLiveData<DialogType> = MutableLiveData()
    fun getShowDialog(): LiveData<DialogType> = showDialogLiveData

    fun loadUserData() {
        viewModelScope.launch {
            loadLocalAuthorityText()

            val isEditModeActive = venueVisitsUiStateLiveData.value?.isInEditMode ?: false
            venueVisitsUiStateLiveData.postValue(
                VenueVisitsUiState(venuesStorage.getVisits().map { it.copy(to = it.to.minusSeconds(1L)) }, isInEditMode = isEditModeActive)
            )
            statusMachineLiveData.postValue(stateMachine.readState())

            acknowledgedTestResultLiveData.postValue(relevantTestResultProvider.testResult)
        }
    }

    fun onDeleteAllUserDataClicked() {
        showDialogLiveData.postValue(ConfirmDeleteAllData)
    }

    fun deleteAllUserData() {
        val submittedOnboardingAnalytics = submittedOnboardingAnalyticsProvider.value
        sharedPreferences.edit().clear().apply()
        submittedOnboardingAnalyticsProvider.value = submittedOnboardingAnalytics
        stateMachine.reset()
        venuesStorage.removeAllVenueVisits()
        allUserDataDeletedLiveData.postValue(Unit)
    }

    fun onVenueVisitDataClicked(position: Int) {
        showDialogLiveData.postValue(ConfirmDeleteVenueVisit(position))
    }

    fun deleteVenueVisit(position: Int) {
        viewModelScope.launch {
            venuesStorage.removeVenueVisit(position)
            venueVisitsUiStateLiveData.postValue(
                VenueVisitsUiState(venuesStorage.getVisits(), isInEditMode = true)
            )
        }
    }

    fun onDialogDismissed() {
        showDialogLiveData.postValue(null)
    }

    fun onEditVenueVisitClicked() {
        viewModelScope.launch {
            val previousVenueVisitsState = venueVisitsUiStateLiveData.value ?: return@launch
            val updatedVenueVisitsState =
                previousVenueVisitsState.copy(isInEditMode = !previousVenueVisitsState.isInEditMode)
            venueVisitsUiStateLiveData.postValue(updatedVenueVisitsState)
            venueVisitsEditModeChangedLiveData.postValue(updatedVenueVisitsState.isInEditMode)
        }
    }

    private suspend fun loadLocalAuthorityText() {
        val text = localAuthorityProvider.value?.let {
            val localAuthorityName = localAuthorityPostCodesLoader.load()?.localAuthorities?.get(it)?.name
            localAuthorityName?.let { name -> "$name\n${postCodePrefs.value}" }
        }
        localAuthorityText.postValue(text ?: postCodePrefs.value)
    }

    data class VenueVisitsUiState(val venueVisits: List<VenueVisit>, val isInEditMode: Boolean)

    sealed class DialogType {
        object ConfirmDeleteAllData : DialogType()
        data class ConfirmDeleteVenueVisit(val venueVisitPosition: Int) : DialogType()
    }
}
