package uk.nhs.nhsx.covid19.android.app.status

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.provider.Settings
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import com.jeroenmols.featureflag.framework.FeatureFlag.TESTING_FOR_COVID19_HOME_SCREEN_BUTTON
import com.jeroenmols.featureflag.framework.FeatureFlag.VENUE_CHECK_IN_BUTTON
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import kotlinx.android.parcel.Parcelize
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.about.MoreAboutAppActivity
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.LocaleProvider
import uk.nhs.nhsx.covid19.android.app.common.assistedViewModel
import uk.nhs.nhsx.covid19.android.app.common.bluetooth.EnableBluetoothActivity
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityStatusBinding
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationActivity
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysInformationActivity
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysReminderActivity
import uk.nhs.nhsx.covid19.android.app.localstats.FetchLocalDataProgressActivity
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider.ContactTracingHubAction
import uk.nhs.nhsx.covid19.android.app.qrcode.QrScannerActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertBookTestActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertInformActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.QuestionnaireActivity
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityStateProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.NotificationMessage
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.BOOK_TEST
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.INFORM
import uk.nhs.nhsx.covid19.android.app.settings.SettingsActivity
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.state.IsolationExpirationActivity
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
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity.StatusActivityAction.None
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.IsolationViewState
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.IsolationViewState.Isolating
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.IsolationViewState.NotIsolating
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.PermissionRequestResult.Error
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.PermissionRequestResult.Request
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Risk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Unknown
import uk.nhs.nhsx.covid19.android.app.status.contacttracinghub.ContactTracingHubActivity
import uk.nhs.nhsx.covid19.android.app.status.guidancehub.GuidanceHubActivity
import uk.nhs.nhsx.covid19.android.app.status.guidancehub.GuidanceHubWalesActivity
import uk.nhs.nhsx.covid19.android.app.status.isolationhub.IsolationHubActivity
import uk.nhs.nhsx.covid19.android.app.status.localmessage.LocalMessageActivity
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultActivity
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultActivity
import uk.nhs.nhsx.covid19.android.app.testordering.unknownresult.UnknownTestResultActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import uk.nhs.nhsx.covid19.android.app.widgets.IsolationStatusView
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Named

class StatusActivity : StatusBaseActivity() {

    @Inject
    lateinit var statusViewModelFactory: StatusViewModel.Factory

    @Inject
    lateinit var localeProvider: LocaleProvider

    @Inject
    @Named(AppModule.BLUETOOTH_STATE_NAME)
    lateinit var bluetoothStateProvider: AvailabilityStateProvider

    private val statusViewModel: StatusViewModel by assistedViewModel {
        val extras = intent.getParcelableExtra(STATUS_ACTIVITY_ACTION) as? StatusActivityAction ?: None
        statusViewModelFactory.create(extras)
    }

    @Inject
    lateinit var dateChangeReceiver: DateChangeReceiver

    private lateinit var binding: ActivityStatusBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startListeningToExposureNotificationPermissionState()

        startListeningToViewState()

        startListeningForNavigationTarget()
        startListeningForBluetoothState()
        setClickListeners()
    }

    private fun startListeningToExposureNotificationPermissionState() {
        statusViewModel.permissionRequest().observe(this) { result ->
            when (result) {
                is Request -> result.callback(this)
                is Error -> Snackbar.make(binding.statusContainer, result.message, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun startListeningForNavigationTarget() {
        statusViewModel.navigationTarget().observe(this) {
            when (it) {
                is IsolationExpiration -> IsolationExpirationActivity.start(
                    this,
                    it.expiryDate.toString()
                )
                TestResult -> startActivity<TestResultActivity>()
                UnknownTestResult -> startActivity<UnknownTestResultActivity>()
                is ExposureConsent -> ExposureNotificationActivity.start(this)
                is ShareKeys -> {
                    if (it.reminder) {
                        startActivity<ShareKeysReminderActivity>()
                    } else {
                        startActivity<ShareKeysInformationActivity>()
                    }
                }
                is VenueAlert -> {
                    when (it.messageType) {
                        INFORM -> VenueAlertInformActivity.start(this, it.venueId)
                        BOOK_TEST -> VenueAlertBookTestActivity.start(this, it.venueId)
                    }
                }
                is ContactTracingHub -> ContactTracingHubActivity.start(this, it.shouldTurnOnContactTracing)
                LocalMessage -> startActivity<LocalMessageActivity>()
                IsolationHub -> startActivity<IsolationHubActivity>()
                InAppReview -> statusViewModel.attemptToStartAppReviewFlow(this)
                EnableBluetooth -> startActivity<EnableBluetoothActivity>()
            }
        }
    }

    private fun startListeningForBluetoothState() {
        bluetoothStateProvider.availabilityState.observe(this) {
            statusViewModel.onBluetoothStateChanged()
        }
    }

    private fun startListeningToViewState() {
        statusViewModel.viewState.observe(this) { viewState ->
            handleIsolationState(
                viewState.isolationState,
                viewState.currentDate,
                viewState.exposureNotificationsEnabled,
                viewState.animationsEnabled,
                viewState.bluetoothEnabled,
                viewState.showCovidStatsButton,
                viewState.country,
                viewState.showIsolationHubButton
            )
            handleRiskyPostCodeViewState(viewState.areaRiskState)
            handleReportSymptomsState(viewState.showReportSymptomsButton)
            handleLocalMessageState(viewState.localMessage)
            handleCovidGuidanceHubState(viewState.showCovidGuidanceHubButton)
            setupCovidGuidanceHubListener(viewState.country)
        }
        binding.optionVenueCheckIn.isVisible = RuntimeBehavior.isFeatureEnabled(VENUE_CHECK_IN_BUTTON)
        binding.optionTestingHub.isVisible = RuntimeBehavior.isFeatureEnabled(TESTING_FOR_COVID19_HOME_SCREEN_BUTTON)
    }

    private fun handleLocalMessageState(localMessage: NotificationMessage?) = with(binding) {
        localMessageBanner.title = localMessage?.head
        localMessageBanner.isVisible = localMessage != null
    }

    private fun handleReportSymptomsState(showReportSymptomsButton: Boolean) {
        binding.optionReportSymptoms.isVisible = showReportSymptomsButton
    }

    private fun handleCovidGuidanceHubState(showCovidGuidanceHubButton: Boolean) {
        binding.optionCovidGuidance.isVisible = showCovidGuidanceHubButton
    }

    private fun setClickListeners() = with(binding) {
        optionReportSymptoms.setOnSingleClickListener {
            optionReportSymptoms.isEnabled = false
            startActivity<QuestionnaireActivity>()
        }

        optionTestingHub.setOnSingleClickListener {
            optionTestingHub.isEnabled = false
            startActivity<TestingHubActivity>()
        }

        optionIsolationHub.setOnSingleClickListener {
            optionIsolationHub.isEnabled = false
            startActivity<IsolationHubActivity>()
        }

        optionVenueCheckIn.setOnSingleClickListener {
            optionVenueCheckIn.isEnabled = false
            QrScannerActivity.start(this@StatusActivity)
        }

        optionAboutTheApp.setOnSingleClickListener {
            optionAboutTheApp.isEnabled = false
            MoreAboutAppActivity.start(this@StatusActivity)
        }

        optionLinkTestResult.setOnSingleClickListener {
            optionLinkTestResult.isEnabled = false
            startActivity<LinkTestResultActivity>()
        }

        optionSettings.setOnSingleClickListener {
            optionSettings.isEnabled = false
            startActivity<SettingsActivity>()
        }

        optionToggleContactTracing.setOnSingleClickListener {
            optionToggleContactTracing.isEnabled = false
            startActivity<ContactTracingHubActivity>()
        }

        riskAreaView.setOnSingleClickListener {
            riskAreaView.isEnabled = false
            statusViewModel.viewState.value?.let {
                RiskLevelActivity.start(this@StatusActivity, it.areaRiskState)
            }
        }

        contactTracingStoppedView.activateContactTracingButton.setOnSingleClickListener {
            statusViewModel.onActivateContactTracingButtonClicked()
        }

        localMessageBanner.setOnSingleClickListener {
            statusViewModel.localMessageBannerClicked()
            startActivity<LocalMessageActivity>()
        }

        optionLocalData.setOnSingleClickListener {
            startActivity<FetchLocalDataProgressActivity>()
        }

        bluetoothStoppedView.activateBluetoothButton.setOnSingleClickListener {
            val bluetoothSettingsIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            try {
                startActivity(bluetoothSettingsIntent)
            } catch (e: ActivityNotFoundException) {
                Snackbar.make(binding.root, R.string.enable_bluetooth_error_hint, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun setupCovidGuidanceHubListener(country: PostCodeDistrict) {
        binding.optionCovidGuidance.setOnSingleClickListener {
            binding.optionCovidGuidance.isEnabled = false
            when (country) {
                ENGLAND -> startActivity<GuidanceHubActivity>()
                WALES -> startActivity<GuidanceHubWalesActivity>()
                else -> throw IllegalStateException("The post code district is not England or Wales")
            }
        }
    }

    private fun handleIsolationState(
        isolationState: IsolationViewState,
        currentDate: LocalDate,
        exposureNotificationsEnabled: Boolean,
        animationsEnabled: Boolean,
        bluetoothEnabled: Boolean,
        showCovidStatsButton: Boolean,
        country: PostCodeDistrict,
        showIsolationHubButton: Boolean
    ) {
        if (statusViewModel.contactTracingSwitchedOn) {
            binding.contactTracingActiveView.focusOnActiveLabel()
            statusViewModel.contactTracingSwitchedOn = false
        }

        when (isolationState) {
            NotIsolating -> {
                showDefaultView(exposureNotificationsEnabled, animationsEnabled, bluetoothEnabled, showCovidStatsButton)
            }
            is Isolating -> {
                showIsolationView(
                    isolationState,
                    currentDate,
                    exposureNotificationsEnabled,
                    animationsEnabled,
                    showCovidStatsButton,
                    country,
                    showIsolationHubButton
                )
            }
        }
    }

    private fun handleRiskyPostCodeViewState(riskyPostCodeViewState: RiskyPostCodeViewState) = with(binding) {
        when (riskyPostCodeViewState) {
            is Risk -> {
                riskAreaView.text = riskyPostCodeViewState.riskIndicator.name.translate()
                riskAreaView.areaRisk = riskyPostCodeViewState.riskIndicator
                riskAreaView.visible()
            }
            is Unknown -> riskAreaView.gone()
        }
    }

    private fun showIsolationView(
        isolationState: Isolating,
        currentDate: LocalDate,
        exposureNotificationsEnabled: Boolean,
        animationsEnabled: Boolean,
        showCovidStatsButton: Boolean,
        country: PostCodeDistrict,
        showIsolationHubButton: Boolean
    ) = with(binding) {
        isolationView.initialize(isolationState, currentDate, country)
        val animationState = when {
            animationsEnabled && exposureNotificationsEnabled -> IsolationStatusView.AnimationState.ANIMATION_ENABLED_EN_ENABLED
            !animationsEnabled && exposureNotificationsEnabled -> IsolationStatusView.AnimationState.ANIMATION_DISABLED_EN_ENABLED
            else -> IsolationStatusView.AnimationState.ANIMATION_DISABLED_EN_DISABLED
        }
        isolationView.animationState = animationState

        contactTracingView.gone()
        isolationView.visible()
        optionIsolationHub.isVisible = showIsolationHubButton
        if (showCovidStatsButton) {
            optionLocalData.visible()
            updateLocalDataButtonPositionWhenInIsolation()
        } else {
            optionLocalData.gone()
        }
    }

    private fun updateLocalDataButtonPositionWhenInIsolation() {
        with(binding) {
            mainActionsContainer.removeView(optionLocalData)
            val indexOfSettingsButton = mainActionsContainer.indexOfChild(optionSettings)
            val targetIndex = if (indexOfSettingsButton <= 0) {
                0
            } else {
                indexOfSettingsButton
            }
            mainActionsContainer.addView(optionLocalData, targetIndex)
        }
    }

    private fun updateLocalDataButtonPositionWhenNotInIsolation() {
        with(binding) {
            mainActionsContainer.removeView(optionLocalData)
            val indexOfVenueCheckInButton = mainActionsContainer.indexOfChild(optionVenueCheckIn)
            val targetIndex = if (indexOfVenueCheckInButton < 0) {
                0
            } else {
                indexOfVenueCheckInButton + 1
            }
            mainActionsContainer.addView(optionLocalData, targetIndex)
        }
    }

    private fun showDefaultView(
        exposureNotificationsEnabled: Boolean,
        animationsEnabled: Boolean,
        bluetoothEnabled: Boolean,
        showCovidStatsButton: Boolean
    ) = with(binding) {

        if (bluetoothEnabled) {
            contactTracingActiveView.isVisible = exposureNotificationsEnabled
            contactTracingActiveView.isAnimationEnabled = animationsEnabled
            contactTracingStoppedView.root.isVisible = !exposureNotificationsEnabled
            bluetoothStoppedView.root.isVisible = !bluetoothEnabled
        } else {
            contactTracingView.visible()
            bluetoothStoppedView.root.visible()
            contactTracingStoppedView.root.gone()
            contactTracingActiveView.gone()
        }

        isolationView.gone()
        contactTracingView.visible()
        optionReportSymptoms.visible()
        optionIsolationHub.gone()
        if (showCovidStatsButton) {
            optionLocalData.visible()
            updateLocalDataButtonPositionWhenNotInIsolation()
        } else {
            optionLocalData.gone()
        }
    }

    override fun onResume() {
        super.onResume()
        bluetoothStateProvider.start(this)
        resetButtonEnabling()
        isVisible = true
        statusViewModel.onResume()
        dateChangeReceiver.registerReceiver(this) {
            statusViewModel.updateViewStateAndCheckUserInbox()
        }
    }

    private fun resetButtonEnabling() = with(binding) {
        arrayOf(
            optionCovidGuidance,
            optionReportSymptoms,
            optionTestingHub,
            optionIsolationHub,
            optionVenueCheckIn,
            optionAboutTheApp,
            optionLinkTestResult,
            optionSettings,
            optionToggleContactTracing,
            riskAreaView,
        ).forEach { it.isEnabled = true }
    }

    override fun onPause() {
        super.onPause()
        isVisible = false
        statusViewModel.onPause()
        dateChangeReceiver.unregisterReceiver(this)
        bluetoothStateProvider.stop(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        statusViewModel.onActivityResult(requestCode, resultCode)
    }

    companion object {
        var isVisible = false

        fun start(context: Context, statusActivityAction: StatusActivityAction = None) {
            context.startActivity(getIntent(context, statusActivityAction))
        }

        private fun getIntent(context: Context, statusActivityAction: StatusActivityAction) =
            Intent(context, StatusActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra(STATUS_ACTIVITY_ACTION, statusActivityAction)
                }

        fun getIntentClearTop(context: Context) =
            Intent(context, StatusActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }

        const val STATUS_ACTIVITY_ACTION = "STATUS_ACTIVITY_ACTION"
    }

    sealed class StatusActivityAction : Parcelable {
        @Parcelize
        data class NavigateToContactTracingHub(val action: ContactTracingHubAction) : StatusActivityAction()

        @Parcelize
        object StartInAppReview : StatusActivityAction()

        @Parcelize
        object NavigateToLocalMessage : StatusActivityAction()

        @Parcelize
        data class ProcessRiskyVenueAlert(val type: RiskyVenueMessageType) : StatusActivityAction()

        @Parcelize
        object NavigateToIsolationHub : StatusActivityAction()

        @Parcelize
        object None : StatusActivityAction()
    }
}
