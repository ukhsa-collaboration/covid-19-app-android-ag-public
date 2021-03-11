package uk.nhs.nhsx.covid19.android.app.about

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeroenmols.featureflag.framework.FeatureFlag.DAILY_CONTACT_TESTING
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.about.UserDataViewModel.DialogType.ConfirmDeleteAllData
import uk.nhs.nhsx.covid19.android.app.about.UserDataViewModel.DialogType.ConfirmDeleteVenueVisit
import uk.nhs.nhsx.covid19.android.app.analytics.SubmittedOnboardingAnalyticsProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodesLoader
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantTestResultProvider
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class UserDataViewModel @Inject constructor(
    private val postCodePrefs: PostCodeProvider,
    private val localAuthorityProvider: LocalAuthorityProvider,
    private val venuesStorage: VisitedVenuesStorage,
    private val stateMachine: IsolationStateMachine,
    private val relevantTestResultProvider: RelevantTestResultProvider,
    private val sharedPreferences: SharedPreferences,
    private val localAuthorityPostCodesLoader: LocalAuthorityPostCodesLoader,
    private val submittedOnboardingAnalyticsProvider: SubmittedOnboardingAnalyticsProvider,
    private val lastVisitedBookTestTypeVenueDateProvider: LastVisitedBookTestTypeVenueDateProvider
) : ViewModel() {

    private val userDataStateLiveData = MutableLiveData<UserDataState>()
    fun userDataState(): LiveData<UserDataState> = userDataStateLiveData

    private val venueVisitsEditModeChangedLiveData: MutableLiveData<Boolean> = SingleLiveEvent()
    fun venueVisitsEditModeChanged(): LiveData<Boolean> = venueVisitsEditModeChangedLiveData

    private val allUserDataDeletedLiveData: MutableLiveData<Unit> = SingleLiveEvent()
    fun getAllUserDataDeleted(): LiveData<Unit> = allUserDataDeletedLiveData

    fun onResume() {
        viewModelScope.launch {
            val updatedViewState = UserDataState(
                localAuthority = getLocalAuthorityText(),
                isolationState = getIsolationState(),
                lastRiskyVenueVisitDate = getLastRiskyVenueVisitDate(),
                venueVisitsUiState = getVenueVisitsUiState(),
                acknowledgedTestResult = relevantTestResultProvider.testResult,
                showDialog = userDataStateLiveData.value?.showDialog
            )
            if (userDataStateLiveData.value != updatedViewState) {
                userDataStateLiveData.postValue(updatedViewState)
            }
        }
    }

    private suspend fun getLocalAuthorityText(): String? =
        localAuthorityProvider.value?.let {
            val localAuthorityName = localAuthorityPostCodesLoader.load()?.localAuthorities?.get(it)?.name
            localAuthorityName?.let { name -> "$name\n${postCodePrefs.value}" }
        } ?: postCodePrefs.value

    private fun getIsolationState(): IsolationState? {
        return when (val isolationState = stateMachine.readState()) {
            is Default -> {
                isolationState.previousIsolation?.let {
                    IsolationState(
                        contactCaseEncounterDate = isolationState.previousIsolation.contactCase?.startDate,
                        contactCaseNotificationDate = isolationState.previousIsolation.contactCase?.notificationDate,
                        dailyContactTestingOptInDate = getDailyContactTestingOptInDateForIsolation(isolationState.previousIsolation)
                    )
                }
            }
            is Isolation -> IsolationState(
                lastDayOfIsolation = isolationState.lastDayOfIsolation,
                contactCaseEncounterDate = isolationState.contactCase?.startDate,
                contactCaseNotificationDate = isolationState.contactCase?.notificationDate,
                indexCaseSymptomOnsetDate = isolationState.indexCase?.symptomsOnsetDate,
                dailyContactTestingOptInDate = getDailyContactTestingOptInDateForIsolation(isolationState)
            )
        }
    }

    private fun getLastRiskyVenueVisitDate(): LocalDate? = lastVisitedBookTestTypeVenueDateProvider.lastVisitedVenue?.latestDate

    private suspend fun getVenueVisitsUiState(): VenueVisitsUiState {
        return VenueVisitsUiState(
            getVenueVisits(),
            isInEditMode = userDataStateLiveData.value?.venueVisitsUiState?.isInEditMode ?: false
        )
    }

    private suspend fun getVenueVisits() = venuesStorage.getVisits()
        .sortedWith(
            compareByDescending<VenueVisit> { it.from }.thenBy { it.venue.organizationPartName }
        )

    private fun getDailyContactTestingOptInDateForIsolation(isolation: Isolation): LocalDate? =
        if (RuntimeBehavior.isFeatureEnabled(DAILY_CONTACT_TESTING)) {
            isolation.contactCase?.dailyContactTestingOptInDate
        } else null

    fun onDeleteAllUserDataClicked() {
        userDataStateLiveData.postValue(userDataStateLiveData.value!!.copy(showDialog = ConfirmDeleteAllData))
    }

    fun deleteAllUserData() {
        val submittedOnboardingAnalytics = submittedOnboardingAnalyticsProvider.value
        sharedPreferences.edit().clear().apply()
        submittedOnboardingAnalyticsProvider.value = submittedOnboardingAnalytics
        stateMachine.reset()
        venuesStorage.removeAllVenueVisits()
        userDataStateLiveData.postValue(userDataStateLiveData.value!!.copy(showDialog = null))
        allUserDataDeletedLiveData.postValue(Unit)
    }

    fun onVenueVisitDataClicked(venueVisit: VenueVisit) {
        userDataStateLiveData.postValue(userDataStateLiveData.value!!.copy(showDialog = ConfirmDeleteVenueVisit(venueVisit)))
    }

    fun deleteVenueVisit(venueVisit: VenueVisit) {
        viewModelScope.launch {
            venuesStorage.removeVenueVisit(venueVisit)
            userDataStateLiveData.postValue(
                userDataStateLiveData.value!!.copy(
                    venueVisitsUiState = VenueVisitsUiState(getVenueVisits(), isInEditMode = true),
                    showDialog = null
                )
            )
        }
    }

    fun onDialogDismissed() {
        userDataStateLiveData.postValue(userDataStateLiveData.value!!.copy(showDialog = null))
    }

    fun onEditVenueVisitClicked() {
        viewModelScope.launch {
            val toggleIsInEditMode = !userDataStateLiveData.value!!.venueVisitsUiState.isInEditMode
            userDataStateLiveData.postValue(
                userDataStateLiveData.value!!.copy(
                    venueVisitsUiState = userDataStateLiveData.value!!.venueVisitsUiState.copy(isInEditMode = toggleIsInEditMode)
                )
            )
            venueVisitsEditModeChangedLiveData.postValue(toggleIsInEditMode)
        }
    }

    data class UserDataState(
        val localAuthority: String?,
        val isolationState: IsolationState?,
        val venueVisitsUiState: VenueVisitsUiState,
        val lastRiskyVenueVisitDate: LocalDate?,
        val acknowledgedTestResult: AcknowledgedTestResult?,
        val showDialog: DialogType? = null
    )

    data class VenueVisitsUiState(val venueVisits: List<VenueVisit>, val isInEditMode: Boolean)

    data class IsolationState(
        val lastDayOfIsolation: LocalDate? = null,
        val contactCaseEncounterDate: Instant? = null,
        val contactCaseNotificationDate: Instant? = null,
        val indexCaseSymptomOnsetDate: LocalDate? = null,
        val dailyContactTestingOptInDate: LocalDate? = null
    )

    sealed class DialogType {
        object ConfirmDeleteAllData : DialogType()
        data class ConfirmDeleteVenueVisit(val venueVisit: VenueVisit) : DialogType()
    }
}
