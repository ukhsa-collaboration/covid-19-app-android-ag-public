package uk.nhs.nhsx.covid19.android.app.about

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.onboarding.authentication.AuthenticationProvider
import uk.nhs.nhsx.covid19.android.app.onboarding.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State
import uk.nhs.nhsx.covid19.android.app.testordering.LatestTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.LatestTestResultProvider
import javax.inject.Inject

class UserDataViewModel @Inject constructor(
    private val postCodePrefs: PostCodeProvider,
    private val venuesStorage: VisitedVenuesStorage,
    private val stateMachine: IsolationStateMachine,
    private val latestResultProvider: LatestTestResultProvider,
    private val sharedPreferences: SharedPreferences,
    private val authenticationProvider: AuthenticationProvider
) : ViewModel() {
    private val postCode = MutableLiveData<String>()
    fun getPostCode(): LiveData<String> = postCode

    private val statusMachineLiveData: MutableLiveData<State> = MutableLiveData()
    fun getLastStatusMachineState(): LiveData<State> = statusMachineLiveData

    private val venueVisitsLiveData: MutableLiveData<List<VenueVisit>> = MutableLiveData()
    fun getVenueVisits(): LiveData<List<VenueVisit>> = venueVisitsLiveData

    private val latestTestResultLiveData: MutableLiveData<LatestTestResult> = MutableLiveData()
    fun getLatestTestResult(): LiveData<LatestTestResult> = latestTestResultLiveData

    fun loadUserData() {
        viewModelScope.launch {
            postCode.postValue(postCodePrefs.value)
            venueVisitsLiveData.postValue(venuesStorage.getVisits())
            statusMachineLiveData.postValue(stateMachine.readState())
            latestTestResultLiveData.postValue(latestResultProvider.latestTestResult)
        }
    }

    fun deleteAllUserData() {
        val authenticationCode = authenticationProvider.value
        sharedPreferences.edit().clear().apply()
        venuesStorage.removeAllVenueVisits()
        authenticationProvider.value = authenticationCode
    }
}
