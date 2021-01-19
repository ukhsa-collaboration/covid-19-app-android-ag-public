package uk.nhs.nhsx.covid19.android.app.status

import android.app.Activity
import android.content.SharedPreferences
import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.play.core.review.ReviewManagerFactory
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedIsolationPaymentsButton
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowEncounterDetection
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowIsolationExpiration
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider.Companion.APP_CONFIGURATION_CHANNEL_ID
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowTestResult
import uk.nhs.nhsx.covid19.android.app.payment.CanClaimIsolationPayment
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Token
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenStateProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.ExposureConsent
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.IsolationExpiration
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.TestResult
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.VenueAlert
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Risk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Unknown
import uk.nhs.nhsx.covid19.android.app.util.DistrictAreaStringProvider
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent

class StatusViewModel @Inject constructor(
    private val postCodeProvider: PostCodeProvider,
    private val postCodeIndicatorProvider: RiskyPostCodeIndicatorProvider,
    private val sharedPreferences: SharedPreferences,
    private val isolationStateMachine: IsolationStateMachine,
    private val userInbox: UserInbox,
    private val notificationProvider: NotificationProvider,
    private val districtAreaStringProvider: DistrictAreaStringProvider,
    private val shouldShowInAppReview: ShouldShowInAppReview,
    private val lastAppRatingStartedDateProvider: LastAppRatingStartedDateProvider,
    private val canClaimIsolationPayment: CanClaimIsolationPayment,
    private val isolationPaymentTokenStateProvider: IsolationPaymentTokenStateProvider,
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    private val clock: Clock
) : ViewModel() {

    private val viewStateLiveData = MutableLiveData<ViewState>()
    val viewState = distinctUntilChanged(viewStateLiveData)

    private val showInformationScreen = SingleLiveEvent<InformationScreen>()
    fun showInformationScreen(): LiveData<InformationScreen> = showInformationScreen

    private val postCodeRiskIndicatorChangedListener = PostCodeRiskIndicatorChangedListener {
        updateViewState()
    }

    private val isolationPaymentTokenStateListener: (IsolationPaymentTokenState) -> Unit = {
        val updatedState =
            viewStateLiveData.value?.copy(showIsolationPaymentButton = mustShowIsolationPaymentButton())
        if (updatedState != null) {
            viewStateLiveData.postValue(updatedState)
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val userInboxListener = {
        checkShouldShowInformationScreen()
    }

    fun onResume() {
        updateViewState()
        checkShouldShowInformationScreen()
        sharedPreferences.registerOnSharedPreferenceChangeListener(
            postCodeRiskIndicatorChangedListener
        )
        isolationPaymentTokenStateProvider.addTokenStateListener(isolationPaymentTokenStateListener)
        userInbox.registerListener(userInboxListener)
    }

    fun onPause() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(
            postCodeRiskIndicatorChangedListener
        )
        isolationPaymentTokenStateProvider.removeTokenStateListener(isolationPaymentTokenStateListener)
        userInbox.unregisterListener(userInboxListener)
    }

    fun onStopExposureNotificationsClicked() {
        val canSendNotification =
            notificationProvider.canSendNotificationToChannel(APP_CONFIGURATION_CHANNEL_ID)
        val updatedState =
            viewStateLiveData.value?.copy(showExposureNotificationReminderDialog = canSendNotification)
        if (updatedState != null) {
            viewStateLiveData.postValue(updatedState)
        }
    }

    fun onExposureNotificationReminderDialogDismissed() {
        val updatedState =
            viewStateLiveData.value?.copy(showExposureNotificationReminderDialog = false)
        if (updatedState != null) {
            viewStateLiveData.postValue(updatedState)
        }
    }

    fun updateViewState(currentDate: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            val showExposureNotificationReminderDialog =
                viewStateLiveData.value?.showExposureNotificationReminderDialog ?: false
            val updatedViewState = ViewState(
                currentDate = currentDate,
                areaRiskState = getAreaRiskViewState(),
                isolationState = isolationStateMachine.readState(),
                latestAdviceUrl = getLatestAdviceUrl(),
                showExposureNotificationReminderDialog = showExposureNotificationReminderDialog,
                showIsolationPaymentButton = mustShowIsolationPaymentButton()
            )
            viewStateLiveData.postValue(updatedViewState)
        }
    }

    private suspend fun getLatestAdviceUrl(): Int {
        val isInDefaultState = isolationStateMachine.readState() is Default
        val url =
            if (isInDefaultState) R.string.url_latest_advice else R.string.url_latest_advice_in_isolation
        return districtAreaStringProvider.provide(url)
    }

    private fun getAreaRiskViewState(): RiskyPostCodeViewState {
        val riskIndicatorWrapper = postCodeIndicatorProvider.riskyPostCodeIndicator
            ?: return Unknown

        return when {
            riskIndicatorWrapper.riskIndicator != null -> {
                Risk(
                    mainPostCode = postCodeProvider.value,
                    riskIndicator = riskIndicatorWrapper.riskIndicator,
                    riskLevelFromLocalAuthority = riskIndicatorWrapper.riskLevelFromLocalAuthority
                )
            }
            else -> {
                Unknown
            }
        }
    }

    private fun mustShowIsolationPaymentButton(): Boolean =
        canClaimIsolationPayment() && isolationPaymentTokenStateProvider.tokenState is Token

    fun attemptToStartAppReviewFlow(activity: Activity) {
        viewModelScope.launch {
            if (shouldShowInAppReview()) {
                startAppReviewFlow(activity)
            }
        }
    }

    private fun startAppReviewFlow(activity: Activity) {
        val reviewManager = ReviewManagerFactory.create(activity)
        val reviewFlowRequest = reviewManager.requestReviewFlow()

        reviewFlowRequest.addOnCompleteListener { request ->
            if (request.isSuccessful) {
                val reviewInfo = request.result
                val reviewFlow = reviewManager.launchReviewFlow(activity, reviewInfo)
                reviewFlow.addOnCompleteListener {
                    lastAppRatingStartedDateProvider.value = Instant.now(clock).toEpochMilli()
                }
            }
        }
    }

    private fun checkShouldShowInformationScreen() {
        viewModelScope.launch {
            when (val item = userInbox.fetchInbox()) {
                is ShowIsolationExpiration -> {
                    showInformationScreen.postValue(IsolationExpiration(item.expirationDate))
                    userInbox.clearItem(item)
                }
                is ShowTestResult -> {
                    notificationProvider.cancelTestResult()
                    showInformationScreen.postValue(TestResult)
                }
                is ShowVenueAlert -> {
                    showInformationScreen.postValue(VenueAlert(item.venueId))
                    userInbox.clearItem(item)
                }
                is ShowEncounterDetection -> {
                    showInformationScreen.postValue(ExposureConsent)
                }
            }
        }
    }

    fun optionIsolationPaymentClicked() {
        viewModelScope.launch {
            analyticsEventProcessor.track(SelectedIsolationPaymentsButton)
        }
    }

    sealed class RiskyPostCodeViewState : Parcelable {
        @Parcelize
        data class Risk(
            val mainPostCode: String?,
            val riskIndicator: RiskIndicator,
            val riskLevelFromLocalAuthority: Boolean
        ) :
            RiskyPostCodeViewState()

        @Parcelize
        object Unknown : RiskyPostCodeViewState()
    }

    data class ViewState(
        val currentDate: LocalDate,
        val areaRiskState: RiskyPostCodeViewState,
        val isolationState: State,
        val latestAdviceUrl: Int,
        val showExposureNotificationReminderDialog: Boolean,
        val showIsolationPaymentButton: Boolean
    )
}

sealed class InformationScreen {
    data class IsolationExpiration(val expiryDate: LocalDate) : InformationScreen()
    object TestResult : InformationScreen()
    object ExposureConsent : InformationScreen()
    data class VenueAlert(val venueId: String) : InformationScreen()
}
