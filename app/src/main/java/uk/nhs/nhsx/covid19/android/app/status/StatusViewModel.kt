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
import com.jeroenmols.featureflag.framework.FeatureFlag.LOCAL_COVID_STATS
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.parcelize.Parcelize
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidAccessLocalInfoScreenViaBanner
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidAccessLocalInfoScreenViaNotification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidAccessRiskyVenueM2Notification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationManager
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationPermissionHelper
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
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
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.ENABLED
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityStateProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.NotificationMessage
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.BOOK_TEST
import uk.nhs.nhsx.covid19.android.app.settings.animations.AnimationsProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.ContactTracingHub
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.EnableBluetooth
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.ExposureConsent
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.InAppReview
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.IsolationExpiration
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.IsolationHub
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.LocalMessage
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.ShareKeys
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.TestResult
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.UnknownTestResult
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.VenueAlert
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity.StatusActivityAction
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity.StatusActivityAction.NavigateToContactTracingHub
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity.StatusActivityAction.NavigateToIsolationHub
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity.StatusActivityAction.NavigateToLocalMessage
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity.StatusActivityAction.None
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity.StatusActivityAction.ProcessRiskyVenueAlert
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity.StatusActivityAction.StartInAppReview
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
import javax.inject.Named

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
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    private val animationsProvider: AnimationsProvider,
    private val clock: Clock,
    private val exposureNotificationManager: ExposureNotificationManager,
    private val areSystemLevelAnimationsEnabled: AreSystemLevelAnimationsEnabled,
    private val getLocalMessageFromStorage: GetLocalMessageFromStorage,
    @Named(AppModule.BLUETOOTH_STATE_NAME) private val bluetoothAvailabilityStateProvider: AvailabilityStateProvider,
    exposureNotificationPermissionHelperFactory: ExposureNotificationPermissionHelper.Factory,
    private val shouldShowBluetoothSplashScreen: ShouldShowBluetoothSplashScreen,
    @Assisted val statusActivityAction: StatusActivityAction,
) : ViewModel() {

    var contactTracingSwitchedOn = false
    var contactTracingHubActionHandled = false
    var showLocalMessageScreenHandled = false
    var showIsolationHubReminderHandled = false
    var didTrackRiskyVenueM2NotificationAnalytics = false
    var showInAppReviewHandled = false

    private val viewStateLiveData = MutableLiveData<ViewState>()
    val viewState = distinctUntilChanged(viewStateLiveData)

    private val navigationTarget = SingleLiveEvent<NavigationTarget>()
    fun navigationTarget(): LiveData<NavigationTarget> = navigationTarget

    private val permissionRequestLiveData = SingleLiveEvent<PermissionRequestResult>()
    fun permissionRequest(): LiveData<PermissionRequestResult> = permissionRequestLiveData

    private val areaInfoChangedListener = AreaInfoChangedListener {
        updateViewState()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val userInboxStorageChangeListener = object : UserInboxStorageChangeListener {
        override fun notifyChanged() {
            checkShouldShowInformationScreen(shouldCheckBluetoothState = false)
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

                override fun onPermissionDenied() {
                    Timber.d("Permission to start contact tracing denied")
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
        updateViewStateAndCheckUserInbox(shouldCheckBluetoothState = true)
        sharedPreferences.registerOnSharedPreferenceChangeListener(
            areaInfoChangedListener
        )
        storageBasedUserInbox.setStorageChangeListener(userInboxStorageChangeListener)
    }

    fun onPause() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(
            areaInfoChangedListener
        )
        storageBasedUserInbox.removeStorageChangeListener()
    }

    fun updateViewStateAndCheckUserInbox(shouldCheckBluetoothState: Boolean = false) {
        updateViewState()
        checkShouldShowInformationScreen(shouldCheckBluetoothState)
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
                showReportSymptomsButton = canReportSymptoms(isolationState),
                exposureNotificationsEnabled = exposureNotificationManager.isEnabled(),
                animationsEnabled = animationsProvider.inAppAnimationEnabled && areSystemLevelAnimationsEnabled(),
                localMessage = getLocalMessageFromStorage(),
                bluetoothEnabled = bluetoothAvailabilityStateProvider.getState() == ENABLED,
                showCovidStatsButton = RuntimeBehavior.isFeatureEnabled(LOCAL_COVID_STATS)
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

    private fun checkShouldShowInformationScreen(shouldCheckBluetoothState: Boolean) {
        val navigatedViaActions = navigateViaActions()
        if (!navigatedViaActions) {
            val navigatedViaInbox = navigateViaUserInbox()
            if (!navigatedViaInbox && shouldCheckBluetoothState && shouldShowBluetoothSplashScreen()) {
                navigationTarget.value = EnableBluetooth
            }
        }
    }

    private fun navigateViaActions(): Boolean {
        when (statusActivityAction) {
            is NavigateToContactTracingHub ->
                if (!contactTracingHubActionHandled) {
                    contactTracingHubActionHandled = true
                    navigationTarget.value =
                        ContactTracingHub(shouldTurnOnContactTracing = statusActivityAction.action == NAVIGATE_AND_TURN_ON)
                    return true
                }
            NavigateToIsolationHub -> {
                if (!showIsolationHubReminderHandled) {
                    showIsolationHubReminderHandled = true
                    navigationTarget.value = IsolationHub
                    return true
                }
            }
            NavigateToLocalMessage -> {
                if (!showLocalMessageScreenHandled) {
                    showLocalMessageScreenHandled = true
                    analyticsEventProcessor.track(DidAccessLocalInfoScreenViaNotification)
                    navigationTarget.value = LocalMessage
                    return true
                }
            }
            is ProcessRiskyVenueAlert -> {
                if (statusActivityAction.type == BOOK_TEST && !didTrackRiskyVenueM2NotificationAnalytics) {
                    didTrackRiskyVenueM2NotificationAnalytics = true
                    analyticsEventProcessor.track(DidAccessRiskyVenueM2Notification)
                }
            }
            StartInAppReview -> {
                if (!showInAppReviewHandled) {
                    showInAppReviewHandled = true
                    navigationTarget.value = InAppReview
                    return true
                }
            }
            None -> {
            }
        }
        return false
    }

    private fun navigateViaUserInbox(): Boolean {
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
        return target != null
    }

    private fun canReportSymptoms(isolationState: IsolationLogicalState): Boolean =
        isolationState.canReportSymptoms(clock)

    fun localMessageBannerClicked() {
        analyticsEventProcessor.track(DidAccessLocalInfoScreenViaBanner)
    }

    fun onBluetoothStateChanged() {
        updateViewState()
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
        val showReportSymptomsButton: Boolean,
        val exposureNotificationsEnabled: Boolean,
        val animationsEnabled: Boolean,
        val localMessage: NotificationMessage?,
        val bluetoothEnabled: Boolean,
        val showCovidStatsButton: Boolean
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
        fun create(statusActivityAction: StatusActivityAction): StatusViewModel
    }
}

sealed class NavigationTarget {
    data class IsolationExpiration(val expiryDate: LocalDate) : NavigationTarget()
    object TestResult : NavigationTarget()
    object UnknownTestResult : NavigationTarget()
    object ExposureConsent : NavigationTarget()
    data class ShareKeys(val reminder: Boolean) : NavigationTarget()
    data class VenueAlert(val venueId: String, val messageType: RiskyVenueMessageType) : NavigationTarget()
    data class ContactTracingHub(val shouldTurnOnContactTracing: Boolean) : NavigationTarget()
    object LocalMessage : NavigationTarget()
    object IsolationHub : NavigationTarget()
    object InAppReview : NavigationTarget()
    object EnableBluetooth : NavigationTarget()
}
