package uk.nhs.nhsx.covid19.android.app.about

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodesLoader
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultsProvider
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class UserDataViewModel @Inject constructor(
    private val postCodePrefs: PostCodeProvider,
    private val localAuthorityProvider: LocalAuthorityProvider,
    private val venuesStorage: VisitedVenuesStorage,
    private val stateMachine: IsolationStateMachine,
    private val testResultsProvider: TestResultsProvider,
    private val sharedPreferences: SharedPreferences,
    private val localAuthorityPostCodesLoader: LocalAuthorityPostCodesLoader
) : ViewModel() {

    private val localAuthorityText = MutableLiveData<String>()
    fun localAuthorityText(): LiveData<String> = localAuthorityText

    private val statusMachineLiveData: MutableLiveData<State> = MutableLiveData()
    fun getLastStatusMachineState(): LiveData<State> = statusMachineLiveData

    private val venueVisitsUiStateLiveData: MutableLiveData<VenueVisitsUiState> = MutableLiveData(
        VenueVisitsUiState(listOf(), isInEditMode = false)
    )

    fun getVenueVisitsUiState(): LiveData<VenueVisitsUiState> = venueVisitsUiStateLiveData

    private val receivedTestResultLiveData: MutableLiveData<ReceivedTestResult> = MutableLiveData()
    fun getReceivedTestResult(): LiveData<ReceivedTestResult> = receivedTestResultLiveData

    private val allUserDataDeletedLiveData: MutableLiveData<Unit> = SingleLiveEvent()
    fun getAllUserDataDeleted(): LiveData<Unit> = allUserDataDeletedLiveData

    fun loadUserData() {
        viewModelScope.launch {
            loadLocalAuthorityText()

            val isEditModeActive = venueVisitsUiStateLiveData.value?.isInEditMode ?: false
            venueVisitsUiStateLiveData.postValue(
                VenueVisitsUiState(venuesStorage.getVisits(), isInEditMode = isEditModeActive)
            )
            statusMachineLiveData.postValue(stateMachine.readState())

            receivedTestResultLiveData.postValue(testResultsProvider.getLastNonVoidTestResult())
        }
    }

    fun deleteAllUserData() {
        sharedPreferences.edit().clear().apply()
        stateMachine.reset()
        venuesStorage.removeAllVenueVisits()
        allUserDataDeletedLiveData.postValue(Unit)
    }

    fun deleteVenueVisit(position: Int) {
        viewModelScope.launch {
            venuesStorage.removeVenueVisit(position)
            venueVisitsUiStateLiveData.postValue(
                VenueVisitsUiState(venuesStorage.getVisits(), isInEditMode = true)
            )
        }
    }

    fun onEditVenueVisitClicked() {
        viewModelScope.launch {
            val previousVenueVisitsState = venueVisitsUiStateLiveData.value ?: return@launch
            val updatedVenueVisitsState =
                previousVenueVisitsState.copy(isInEditMode = !previousVenueVisitsState.isInEditMode)
            venueVisitsUiStateLiveData.postValue(updatedVenueVisitsState)
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
}
