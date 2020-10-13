package uk.nhs.nhsx.covid19.android.app.status

import android.app.Activity
import android.content.SharedPreferences
import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.play.core.review.ReviewManagerFactory
import com.jeroenmols.featureflag.framework.FeatureFlag.TEST_ORDERING
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.string
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowEncounterDetection
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowIsolationExpiration
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.HIGH
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.LOW
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.MEDIUM
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.IsolationExpiration
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.TestResult
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.VenueAlert
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.OldRisk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Risk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Unknown
import uk.nhs.nhsx.covid19.android.app.util.DistrictAreaStringProvider
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
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
    private val lastAppRatingStartedDateProvider: LastAppRatingStartedDateProvider
) : ViewModel() {

    private val viewStateLiveData = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewStateLiveData

    private val showInformationScreen = SingleLiveEvent<InformationScreen>()
    fun showInformationScreen(): LiveData<InformationScreen> = showInformationScreen

    private val canReceiveReminderLiveData = SingleLiveEvent<Boolean>()
    fun onExposureNotificationStopped(): LiveData<Boolean> = canReceiveReminderLiveData

    private val postCodeRiskIndicatorChangedListener = PostCodeRiskIndicatorChangedListener {
        updateViewState()
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
        userInbox.registerListener(userInboxListener)
    }

    fun onPause() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(
            postCodeRiskIndicatorChangedListener
        )
        userInbox.unregisterListener(userInboxListener)
    }

    fun onStopExposureNotificationsClicked() {
        canReceiveReminderLiveData.postValue(getCanReceiveReminder())
    }

    fun updateViewState() {
        viewStateLiveData.postValue(
            ViewState(
                areaRiskState = getAreaRiskViewState(),
                isolationState = isolationStateMachine.readState(),
                latestAdviceUrl = getLatestAdviceUrl()
            )
        )
    }

    private fun getCanReceiveReminder(): Boolean =
        notificationProvider.canSendNotificationToChannel(
            NotificationProvider.APP_CONFIGURATION_CHANNEL_ID
        )

    private fun getLatestAdviceUrl(): Int {
        val isInDefaultState = isolationStateMachine.readState() is Default
        val url =
            if (isInDefaultState) R.string.url_latest_advice else R.string.url_latest_advice_in_isolation
        return districtAreaStringProvider.provide(url)
    }

    private fun getAreaRiskViewState(): RiskyPostCodeViewState {
        val riskIndicatorWrapper =
            postCodeIndicatorProvider.riskyPostCodeIndicator ?: return Unknown

        return when {
            riskIndicatorWrapper.riskIndicator != null -> {
                Risk(
                    mainPostCode = postCodeProvider.value,
                    riskIndicator = riskIndicatorWrapper.riskIndicator
                )
            }
            riskIndicatorWrapper.oldRiskLevel != null -> {
                OldRisk(
                    mainPostCode = postCodeProvider.value,
                    textResId = districtAreaStringProvider.provide(string.status_area_risk_level),
                    areaRiskLevelResId = getAreaRiskLevelResId(riskIndicatorWrapper.oldRiskLevel),
                    areaRisk = riskIndicatorWrapper.oldRiskLevel
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

    private fun getAreaRiskLevelResId(riskLevel: RiskLevel): Int =
        when (riskLevel) {
            LOW -> districtAreaStringProvider.provide(R.string.status_area_risk_level_low)
            MEDIUM -> districtAreaStringProvider.provide(R.string.status_area_risk_level_medium)
            HIGH -> districtAreaStringProvider.provide(R.string.status_area_risk_level_high)
        }

    private fun startAppReviewFlow(activity: Activity) {
        val reviewManager = ReviewManagerFactory.create(activity)
        val reviewFlowRequest = reviewManager.requestReviewFlow()

        reviewFlowRequest.addOnCompleteListener { request ->
            if (request.isSuccessful) {
                val reviewInfo = request.result
                val reviewFlow = reviewManager.launchReviewFlow(activity, reviewInfo)
                reviewFlow.addOnCompleteListener { _ ->
                    lastAppRatingStartedDateProvider.value =
                        LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli()
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
                    if (RuntimeBehavior.isFeatureEnabled(TEST_ORDERING)) {
                        notificationProvider.cancelTestResult()
                        showInformationScreen.postValue(TestResult)
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

    sealed class RiskyPostCodeViewState : Parcelable {
        @Parcelize
        data class Risk(
            val mainPostCode: String?,
            val riskIndicator: RiskIndicator
        ) :
            RiskyPostCodeViewState()

        @Parcelize
        data class OldRisk(
            val mainPostCode: String?,
            val textResId: Int,
            val areaRiskLevelResId: Int,
            val areaRisk: RiskLevel
        ) :
            RiskyPostCodeViewState()

        @Parcelize
        object Unknown : RiskyPostCodeViewState()
    }

    data class ViewState(
        val areaRiskState: RiskyPostCodeViewState,
        val isolationState: State,
        val latestAdviceUrl: Int
    )
}

sealed class InformationScreen {
    data class IsolationExpiration(val expiryDate: LocalDate) : InformationScreen()
    object TestResult : InformationScreen()
    object ExposureConsent : InformationScreen()
    data class VenueAlert(val venueId: String) : InformationScreen()
}
