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
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedIsolationPaymentsButton
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationManager
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationPermissionHelper
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowEncounterDetection
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ContinueInitialKeySharing
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowIsolationExpiration
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowKeySharingReminder
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowTestResult
import uk.nhs.nhsx.covid19.android.app.payment.CanClaimIsolationPayment
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Token
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenStateProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.MessageType
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.ExposureConsent
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.IsolationExpiration
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.ShareKeys
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.TestResult
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.VenueAlert
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.IsolationViewState.Isolating
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.IsolationViewState.NotIsolating
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.PermissionRequestResult.Error
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.PermissionRequestResult.Request
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Risk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Unknown
import uk.nhs.nhsx.covid19.android.app.util.DistrictAreaStringProvider
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

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
    private val lastVisitedBookTestTypeVenueDateProvider: LastVisitedBookTestTypeVenueDateProvider,
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    private val clock: Clock,
    private val exposureNotificationManager: ExposureNotificationManager,
    exposureNotificationPermissionHelperFactory: ExposureNotificationPermissionHelper.Factory,
) : ViewModel() {

    var contactTracingSwitchedOn = false

    private val viewStateLiveData = MutableLiveData<ViewState>()
    val viewState = distinctUntilChanged(viewStateLiveData)

    private val showInformationScreen = SingleLiveEvent<InformationScreen>()
    fun showInformationScreen(): LiveData<InformationScreen> = showInformationScreen

    private val permissionRequestLiveData = SingleLiveEvent<PermissionRequestResult>()
    fun permissionRequest(): LiveData<PermissionRequestResult> = permissionRequestLiveData

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

    private val exposureNotificationPermissionHelper =
        exposureNotificationPermissionHelperFactory.create(
            object : ExposureNotificationPermissionHelper.Callback {
                override fun onExposureNotificationsEnabled() {
                    Timber.d("Exposure notifications successfully started")
                    contactTracingSwitchedOn = true
                    updateViewState()
                }

                override fun onPermissionRequired(permissionRequest: (Activity) -> Unit) {
                    permissionRequestLiveData.postValue(Request(permissionRequest))
                }

                override fun onError(error: Throwable) {
                    Timber.e(error, "Could not start exposure notifications")
                    permissionRequestLiveData.postValue(Error(error.message ?: ""))
                }
            },
            viewModelScope
        )

    fun onActivateContactTracingButtonClicked() {
        exposureNotificationPermissionHelper.startExposureNotifications()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        exposureNotificationPermissionHelper.onActivityResult(requestCode, resultCode)
    }

    fun onResume() {
        updateViewStateAndCheckUserInbox()
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

    fun updateViewStateAndCheckUserInbox() {
        updateViewState()
        checkShouldShowInformationScreen()
    }

    @VisibleForTesting
    fun updateViewState(
        currentDate: LocalDate = LocalDate.now(clock)
    ) {
        viewModelScope.launch {
            val isolationState = isolationStateMachine.readLogicalState()
            val updatedViewState = ViewState(
                currentDate = currentDate,
                areaRiskState = getAreaRiskViewState(),
                isolationState = getIsolationViewState(isolationState),
                latestAdviceUrl = getLatestAdviceUrl(),
                showIsolationPaymentButton = mustShowIsolationPaymentButton(),
                showOrderTestButton = canOrderTest(isolationState),
                showReportSymptomsButton = canReportSymptoms(isolationState),
                exposureNotificationsEnabled = exposureNotificationManager.isEnabled(),
            )
            viewStateLiveData.postValue(updatedViewState)
        }
    }

    private fun getIsolationViewState(isolationState: IsolationLogicalState): IsolationViewState =
        when {
            isolationState is PossiblyIsolating && isolationState.isActiveIsolation(clock) -> Isolating(
                isolationState.startDate,
                isolationState.expiryDate
            )
            else -> NotIsolating
        }

    private suspend fun getLatestAdviceUrl(): Int {
        val isIsolating = isolationStateMachine.readLogicalState().isActiveIsolation(clock)
        val url =
            if (isIsolating) R.string.url_latest_advice_in_isolation
            else R.string.url_latest_advice
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
                }
                is ShowTestResult -> {
                    notificationProvider.cancelTestResult()
                    showInformationScreen.postValue(TestResult)
                }
                is ShowVenueAlert -> showInformationScreen.postValue(VenueAlert(item.venueId, item.messageType))
                is ShowEncounterDetection -> showInformationScreen.postValue(ExposureConsent)
                is ContinueInitialKeySharing -> showInformationScreen.postValue(ShareKeys(reminder = false))
                is ShowKeySharingReminder -> showInformationScreen.postValue(ShareKeys(reminder = true))
            }
        }
    }

    private fun canOrderTest(isolationState: IsolationLogicalState): Boolean =
        lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk() ||
            isolationState.isActiveIsolation(clock)

    private fun canReportSymptoms(isolationState: IsolationLogicalState): Boolean =
        isolationState.canReportSymptoms(clock)

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
        val isolationState: IsolationViewState,
        val latestAdviceUrl: Int,
        val showIsolationPaymentButton: Boolean,
        val showOrderTestButton: Boolean,
        val showReportSymptomsButton: Boolean,
        val exposureNotificationsEnabled: Boolean,
    )

    sealed class PermissionRequestResult {
        data class Request(val callback: (Activity) -> Unit) : PermissionRequestResult()
        data class Error(val message: String) : PermissionRequestResult()
    }

    sealed class IsolationViewState {
        object NotIsolating : IsolationViewState()
        data class Isolating(
            val isolationStart: LocalDate,
            val expiryDate: LocalDate
        ) : IsolationViewState()
    }
}

sealed class InformationScreen {
    data class IsolationExpiration(val expiryDate: LocalDate) : InformationScreen()
    object TestResult : InformationScreen()
    object ExposureConsent : InformationScreen()
    data class ShareKeys(val reminder: Boolean) : InformationScreen()
    data class VenueAlert(val venueId: String, val messageType: MessageType) : InformationScreen()
}
