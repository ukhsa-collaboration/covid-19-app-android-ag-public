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
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidAccessLocalInfoScreenViaBanner
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidAccessLocalInfoScreenViaNotification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedIsolationPaymentsButton
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationManager
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationPermissionHelper
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider.ContactTracingHubAction
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider.ContactTracingHubAction.NAVIGATE_AND_TURN_ON
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.StorageBasedUserInbox
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInbox
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ContinueInitialKeySharing
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowEncounterDetection
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowIsolationExpiration
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowKeySharingReminder
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowTestResult
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowUnknownTestResult
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxStorageChangeListener
import uk.nhs.nhsx.covid19.android.app.payment.CanClaimIsolationPayment
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Token
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenStateProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessageTranslation
import uk.nhs.nhsx.covid19.android.app.remote.data.MessageType
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
import uk.nhs.nhsx.covid19.android.app.settings.animations.AnimationsProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.ContactTracingHub
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.ExposureConsent
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.IsolationExpiration
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.LocalMessage
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.ShareKeys
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.TestResult
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.UnknownTestResult
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.VenueAlert
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.IsolationViewState.Isolating
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.IsolationViewState.NotIsolating
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.PermissionRequestResult.Error
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.PermissionRequestResult.Request
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Risk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Unknown
import uk.nhs.nhsx.covid19.android.app.status.localmessage.GetLocalMessageFromStorage
import uk.nhs.nhsx.covid19.android.app.util.DistrictAreaStringProvider
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import uk.nhs.nhsx.covid19.android.app.util.viewutils.AreSystemLevelAnimationsEnabled
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

class StatusViewModel @AssistedInject constructor(
    private val postCodeProvider: PostCodeProvider,
    private val postCodeIndicatorProvider: RiskyPostCodeIndicatorProvider,
    private val sharedPreferences: SharedPreferences,
    private val isolationStateMachine: IsolationStateMachine,
    private val userInbox: UserInbox,
    private val storageBasedUserInbox: StorageBasedUserInbox,
    private val notificationProvider: NotificationProvider,
    private val districtAreaStringProvider: DistrictAreaStringProvider,
    private val shouldShowInAppReview: ShouldShowInAppReview,
    private val lastAppRatingStartedDateProvider: LastAppRatingStartedDateProvider,
    private val canClaimIsolationPayment: CanClaimIsolationPayment,
    private val isolationPaymentTokenStateProvider: IsolationPaymentTokenStateProvider,
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    private val animationsProvider: AnimationsProvider,
    private val clock: Clock,
    private val exposureNotificationManager: ExposureNotificationManager,
    private val areSystemLevelAnimationsEnabled: AreSystemLevelAnimationsEnabled,
    private val getLocalMessageFromStorage: GetLocalMessageFromStorage,
    exposureNotificationPermissionHelperFactory: ExposureNotificationPermissionHelper.Factory,
    @Assisted val contactTracingHubAction: ContactTracingHubAction?,
    @Assisted val showLocalMessageScreen: Boolean
) : ViewModel() {

    var contactTracingSwitchedOn = false
    var contactTracingHubActionHandled = false
    var showLocalMessageScreenHandled = false

    private val viewStateLiveData = MutableLiveData<ViewState>()
    val viewState = distinctUntilChanged(viewStateLiveData)

    private val navigationTarget = SingleLiveEvent<NavigationTarget>()
    fun navigationTarget(): LiveData<NavigationTarget> = navigationTarget

    private val permissionRequestLiveData = SingleLiveEvent<PermissionRequestResult>()
    fun permissionRequest(): LiveData<PermissionRequestResult> = permissionRequestLiveData

    private val areaInfoChangedListener = AreaInfoChangedListener {
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
    val userInboxStorageChangeListener = object : UserInboxStorageChangeListener {
        override fun notifyChanged() {
            checkShouldShowInformationScreen()
        }
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
            areaInfoChangedListener
        )
        isolationPaymentTokenStateProvider.addTokenStateListener(isolationPaymentTokenStateListener)
        storageBasedUserInbox.setStorageChangeListener(userInboxStorageChangeListener)
    }

    fun onPause() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(
            areaInfoChangedListener
        )
        isolationPaymentTokenStateProvider.removeTokenStateListener(isolationPaymentTokenStateListener)
        storageBasedUserInbox.removeStorageChangeListener()
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
                showIsolationPaymentButton = mustShowIsolationPaymentButton(),
                showReportSymptomsButton = canReportSymptoms(isolationState),
                exposureNotificationsEnabled = exposureNotificationManager.isEnabled(),
                animationsEnabled = animationsProvider.inAppAnimationEnabled && areSystemLevelAnimationsEnabled(),
                localMessage = getLocalMessageFromStorage()
            )
            viewStateLiveData.postValue(updatedViewState)
        }
    }

    private suspend fun getIsolationViewState(isolationState: IsolationLogicalState): IsolationViewState =
        when {
            isolationState is PossiblyIsolating && isolationState.isActiveIsolation(clock) -> Isolating(
                isolationState.startDate,
                isolationState.expiryDate,
                districtAreaStringProvider.provide(R.string.url_latest_advice_in_isolation)
            )
            else -> NotIsolating
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
        if (contactTracingHubAction != null && !contactTracingHubActionHandled) {
            contactTracingHubActionHandled = true
            navigationTarget.postValue(ContactTracingHub(shouldTurnOnContactTracing = contactTracingHubAction == NAVIGATE_AND_TURN_ON))
            return
        }

        if (showLocalMessageScreen && !showLocalMessageScreenHandled) {
            showLocalMessageScreenHandled = true
            trackAnalyticsEvent(DidAccessLocalInfoScreenViaNotification)
            navigationTarget.postValue(LocalMessage)
            return
        }

        viewModelScope.launch {
            val target = when (val item = userInbox.fetchInbox()) {
                is ShowIsolationExpiration -> IsolationExpiration(item.expirationDate)
                is ShowTestResult -> {
                    notificationProvider.cancelTestResult()
                    TestResult
                }
                is ShowUnknownTestResult -> UnknownTestResult
                is ShowVenueAlert -> VenueAlert(item.venueId, item.messageType)
                is ShowEncounterDetection -> ExposureConsent
                is ContinueInitialKeySharing -> ShareKeys(reminder = false)
                is ShowKeySharingReminder -> ShareKeys(reminder = true)
                null -> null
            }

            if (target != null) {
                navigationTarget.postValue(target)
            }
        }
    }

    private fun canReportSymptoms(isolationState: IsolationLogicalState): Boolean =
        isolationState.canReportSymptoms(clock)

    fun optionIsolationPaymentClicked() {
        trackAnalyticsEvent(SelectedIsolationPaymentsButton)
    }

    fun localMessageBannerClicked() {
        trackAnalyticsEvent(DidAccessLocalInfoScreenViaBanner)
    }

    private fun trackAnalyticsEvent(event: AnalyticsEvent) {
        viewModelScope.launch {
            analyticsEventProcessor.track(event)
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
        val showIsolationPaymentButton: Boolean,
        val showReportSymptomsButton: Boolean,
        val exposureNotificationsEnabled: Boolean,
        val animationsEnabled: Boolean,
        val localMessage: LocalMessageTranslation?
    )

    sealed class PermissionRequestResult {
        data class Request(val callback: (Activity) -> Unit) : PermissionRequestResult()
        data class Error(val message: String) : PermissionRequestResult()
    }

    sealed class IsolationViewState {
        object NotIsolating : IsolationViewState()
        data class Isolating(
            val isolationStart: LocalDate,
            val expiryDate: LocalDate,
            val isolationAdvice: Int
        ) : IsolationViewState()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            contactTracingHubAction: ContactTracingHubAction?,
            showLocalMessageScreen: Boolean,
        ): StatusViewModel
    }
}

sealed class NavigationTarget {
    data class IsolationExpiration(val expiryDate: LocalDate) : NavigationTarget()
    object TestResult : NavigationTarget()
    object UnknownTestResult : NavigationTarget()
    object ExposureConsent : NavigationTarget()
    data class ShareKeys(val reminder: Boolean) : NavigationTarget()
    data class VenueAlert(val venueId: String, val messageType: MessageType) : NavigationTarget()
    data class ContactTracingHub(val shouldTurnOnContactTracing: Boolean) : NavigationTarget()
    object LocalMessage : NavigationTarget()
}
