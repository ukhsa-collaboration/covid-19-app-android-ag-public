package uk.nhs.nhsx.covid19.android.app.status

import android.content.SharedPreferences
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeroenmols.featureflag.framework.FeatureFlag.TEST_ORDERING
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTasks
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowEncounterDetection
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowIsolationExpiration
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowTestResult
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.onboarding.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.HIGH
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.LOW
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.MEDIUM
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.IsolationExpiration
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.TestResult
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.VenueAlert
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.HighRisk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.LowRisk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.MediumRisk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Unknown
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import java.time.LocalDate
import javax.inject.Inject

class StatusViewModel @Inject constructor(
    private val postCodeProvider: PostCodeProvider,
    private val riskyPostCodeDetectedDetectedPrefs: RiskyPostCodeDetectedProvider,
    private val sharedPreferences: SharedPreferences,
    private val isolationStateMachine: IsolationStateMachine,
    private val userInbox: UserInbox,
    private val periodicTasks: PeriodicTasks,
    private val notificationProvider: NotificationProvider
) : ViewModel() {

    private val areaRiskStateLiveData = SingleLiveEvent<RiskyPostCodeViewState>()
    private val showInformationScreen = SingleLiveEvent<InformationScreen>()
    private val userStateLiveDate = MutableLiveData<State>()

    fun areaRiskState(): LiveData<RiskyPostCodeViewState> = areaRiskStateLiveData
    fun userState(): LiveData<State> = userStateLiveDate
    fun showInformationScreen(): LiveData<InformationScreen> = showInformationScreen

    private val areaRiskPrefChangedListener = AreaRiskPreferenceChangedListener {
        updateAreaRisk()
    }

    private val userInboxListener = {
        checkShouldShowInformationScreen()
    }

    fun onResume() {
        startPeriodicTasks()
        checkIsolationState()
        checkShouldShowInformationScreen()
        updateAreaRisk()

        sharedPreferences.registerOnSharedPreferenceChangeListener(areaRiskPrefChangedListener)
        userInbox.registerListener(userInboxListener)
    }

    fun onPause() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(areaRiskPrefChangedListener)
        userInbox.unregisterListener(userInboxListener)
    }

    fun updateAreaRisk() {
        areaRiskStateLiveData.postValue(determineRiskLevel())
    }

    private fun checkIsolationState() {
        userStateLiveDate.postValue(isolationStateMachine.readState())
    }

    private fun checkShouldShowInformationScreen() {
        viewModelScope.launch {
            when (val item = userInbox.fetchInbox()) {
                is ShowIsolationExpiration -> {
                    showInformationScreen.postValue(IsolationExpiration(item.expirationDate))
                    userInbox.clearItem(item)
                }
                is ShowTestResult -> {
                    if (RuntimeBehavior.isFeatureEnabled(TEST_ORDERING)) {
                        notificationProvider.cancelTestResult()
                        showInformationScreen.postValue(TestResult)
                        userInbox.clearItem(item)
                    }
                }
                is ShowVenueAlert -> {
                    showInformationScreen.postValue(VenueAlert(item.venueId))
                    userInbox.clearItem(item)
                }
                is ShowEncounterDetection -> {
                    showInformationScreen.postValue(InformationScreen.ExposureConsent)
                }
            }
        }
    }

    private fun determineRiskLevel(): RiskyPostCodeViewState =
        when (riskyPostCodeDetectedDetectedPrefs.toRiskLevel()) {
            HIGH -> HighRisk(postCodeProvider.value)
            MEDIUM -> MediumRisk(postCodeProvider.value)
            LOW -> LowRisk(postCodeProvider.value)
            null -> Unknown
        }

    private fun startPeriodicTasks() {
        periodicTasks.schedule(keepPrevious = true)
    }

    fun onDateChanged() {
        checkIsolationState()
    }

    sealed class RiskyPostCodeViewState : Parcelable {
        @Parcelize
        data class LowRisk(val mainPostCode: String?, val name: String = LOW.name) :
            RiskyPostCodeViewState()

        @Parcelize
        data class MediumRisk(val mainPostCode: String?, val name: String = MEDIUM.name) :
            RiskyPostCodeViewState()

        @Parcelize

        data class HighRisk(val mainPostCode: String?, val name: String = HIGH.name) :
            RiskyPostCodeViewState()

        @Parcelize
        object Unknown : RiskyPostCodeViewState()
    }
}

sealed class InformationScreen {
    data class IsolationExpiration(val expiryDate: LocalDate) : InformationScreen()
    object TestResult : InformationScreen()
    object ExposureConsent : InformationScreen()
    data class VenueAlert(val venueId: String) : InformationScreen()
}
