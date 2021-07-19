package uk.nhs.nhsx.covid19.android.app.status

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_status.contactTracingActiveView
import kotlinx.android.synthetic.main.activity_status.contactTracingStoppedView
import kotlinx.android.synthetic.main.activity_status.contactTracingView
import kotlinx.android.synthetic.main.activity_status.isolationView
import kotlinx.android.synthetic.main.activity_status.localMessageBanner
import kotlinx.android.synthetic.main.activity_status.optionAboutTheApp
import kotlinx.android.synthetic.main.activity_status.optionIsolationPayment
import kotlinx.android.synthetic.main.activity_status.optionLinkTestResult
import kotlinx.android.synthetic.main.activity_status.optionReadAdvice
import kotlinx.android.synthetic.main.activity_status.optionReportSymptoms
import kotlinx.android.synthetic.main.activity_status.optionSettings
import kotlinx.android.synthetic.main.activity_status.optionTestingHub
import kotlinx.android.synthetic.main.activity_status.optionToggleContactTracing
import kotlinx.android.synthetic.main.activity_status.optionVenueCheckIn
import kotlinx.android.synthetic.main.activity_status.riskAreaView
import kotlinx.android.synthetic.main.activity_status.statusContainer
import kotlinx.android.synthetic.main.include_stopped.activateContactTracingButton
import kotlinx.android.synthetic.main.view_default_state.contactTracingActiveLabel
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.about.MoreAboutAppActivity
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.assistedViewModel
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EncounterDetectionActivity
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysInformationActivity
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysReminderActivity
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider.ContactTracingHubAction
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.QrScannerActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertBookTestActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertInformActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.QuestionnaireActivity
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.BOOK_TEST
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.INFORM
import uk.nhs.nhsx.covid19.android.app.remote.data.NotificationMessage
import uk.nhs.nhsx.covid19.android.app.settings.SettingsActivity
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.state.IsolationExpirationActivity
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.ContactTracingHub
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.ExposureConsent
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.IsolationExpiration
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.LocalMessage
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.ShareKeys
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.TestResult
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.UnknownTestResult
import uk.nhs.nhsx.covid19.android.app.status.NavigationTarget.VenueAlert
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.IsolationViewState
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.IsolationViewState.Isolating
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.IsolationViewState.NotIsolating
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.PermissionRequestResult.Error
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.PermissionRequestResult.Request
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Risk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Unknown
import uk.nhs.nhsx.covid19.android.app.status.contacttracinghub.ContactTracingHubActivity
import uk.nhs.nhsx.covid19.android.app.status.localmessage.LocalMessageActivity
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultActivity
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultActivity
import uk.nhs.nhsx.covid19.android.app.testordering.unknownresult.UnknownTestResultActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.openUrl
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import uk.nhs.nhsx.covid19.android.app.widgets.IsolationStatusView
import java.time.LocalDate
import javax.inject.Inject

class StatusActivity : StatusBaseActivity(R.layout.activity_status) {

    @Inject
    lateinit var statusViewModelFactory: StatusViewModel.Factory

    private val statusViewModel: StatusViewModel by assistedViewModel {
        statusViewModelFactory.create(
            contactTracingHubAction = intent.getSerializableExtra(CONTACT_TRACING_HUB_ACTION_EXTRA) as? ContactTracingHubAction,
            showLocalMessageScreen = intent.getBooleanExtra(STARTED_FROM_LOCAL_MESSAGE_NOTIFICATION, false),
            startedFromRiskyVenueNotificationWithType = intent.getSerializableExtra(STARTED_FROM_RISKY_VENUE_NOTIFICATION_WITH_TYPE) as? RiskyVenueMessageType
        )
    }

    @Inject
    lateinit var dateChangeReceiver: DateChangeReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        startListeningToExposureNotificationPermissionState()

        startListeningToViewState()

        startListeningForNavigationTarget()

        setClickListeners()

        checkIfInAppReviewShouldBeDisplayed()
    }

    private fun startListeningToExposureNotificationPermissionState() {
        statusViewModel.permissionRequest().observe(this) { result ->
            when (result) {
                is Request -> result.callback(this)
                is Error -> Snackbar.make(statusContainer, result.message, Snackbar.LENGTH_SHORT).show()
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
                ExposureConsent -> EncounterDetectionActivity.start(this)
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
            }
        }
    }

    private fun startListeningToViewState() {
        statusViewModel.viewState.observe(this) { viewState ->
            handleIsolationState(
                viewState.isolationState,
                viewState.currentDate,
                viewState.exposureNotificationsEnabled,
                viewState.animationsEnabled
            )
            handleRiskyPostCodeViewState(viewState.areaRiskState)
            handleIsolationPaymentState(viewState.showIsolationPaymentButton)
            handleReportSymptomsState(viewState.showReportSymptomsButton)
            handleLocalMessageState(viewState.localMessage)
        }
    }

    private fun handleLocalMessageState(localMessage: NotificationMessage?) {
        localMessageBanner.title = localMessage?.head
        localMessageBanner.isVisible = localMessage != null
    }

    private fun handleReportSymptomsState(showReportSymptomsButton: Boolean) {
        optionReportSymptoms.isVisible = showReportSymptomsButton
    }

    private fun handleIsolationPaymentState(showIsolationPaymentButton: Boolean) {
        optionIsolationPayment.isVisible = showIsolationPaymentButton
    }

    private fun checkIfInAppReviewShouldBeDisplayed() {
        val startedFromVenueCheckInSuccess =
            intent.getBooleanExtra(STARTED_FROM_VENUE_CHECK_IN_SUCCESS, false)

        if (startedFromVenueCheckInSuccess) {
            statusViewModel.attemptToStartAppReviewFlow(this)
        }
    }

    private fun setClickListeners() {
        optionReportSymptoms.setOnSingleClickListener {
            optionReportSymptoms.isEnabled = false
            startActivity<QuestionnaireActivity>()
        }

        optionTestingHub.setOnSingleClickListener {
            optionTestingHub.isEnabled = false
            startActivity<TestingHubActivity>()
        }

        optionVenueCheckIn.setOnSingleClickListener {
            optionVenueCheckIn.isEnabled = false
            QrScannerActivity.start(this)
        }

        optionAboutTheApp.setOnSingleClickListener {
            optionAboutTheApp.isEnabled = false
            MoreAboutAppActivity.start(this)
        }

        optionIsolationPayment.setOnSingleClickListener {
            optionIsolationPayment.isEnabled = false
            statusViewModel.optionIsolationPaymentClicked()
            startActivity<IsolationPaymentActivity>()
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
                RiskLevelActivity.start(this, it.areaRiskState)
            }
        }

        activateContactTracingButton.setOnSingleClickListener {
            statusViewModel.onActivateContactTracingButtonClicked()
        }

        localMessageBanner.setOnSingleClickListener {
            statusViewModel.localMessageBannerClicked()
            startActivity<LocalMessageActivity>()
        }
    }

    private fun handleIsolationState(
        isolationState: IsolationViewState,
        currentDate: LocalDate,
        exposureNotificationsEnabled: Boolean,
        animationsEnabled: Boolean
    ) {
        if (statusViewModel.contactTracingSwitchedOn) {
            contactTracingActiveLabel.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null)
            statusViewModel.contactTracingSwitchedOn = false
        }

        when (isolationState) {
            NotIsolating -> {
                showDefaultView(exposureNotificationsEnabled, animationsEnabled)
            }
            is Isolating -> {
                showIsolationView(isolationState, currentDate, exposureNotificationsEnabled, animationsEnabled)
            }
        }
        setUpReadSelfIsolationAdviceOption(isolationState)
    }

    private fun setUpReadSelfIsolationAdviceOption(isolationState: IsolationViewState) {
        when (isolationState) {
            is Isolating -> {
                optionReadAdvice.setOnSingleClickListener {
                    optionReadAdvice.isEnabled = false
                    openUrl(isolationState.isolationAdvice, useInternalBrowser = true)
                }
                optionReadAdvice.visible()
            }
            NotIsolating -> optionReadAdvice.gone()
        }
    }

    private fun handleRiskyPostCodeViewState(riskyPostCodeViewState: RiskyPostCodeViewState) {
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
        animationsEnabled: Boolean
    ) {
        isolationView.initialize(isolationState, currentDate)
        val animationState = when {
            animationsEnabled && exposureNotificationsEnabled -> IsolationStatusView.AnimationState.ANIMATION_ENABLED_EN_ENABLED
            !animationsEnabled && exposureNotificationsEnabled -> IsolationStatusView.AnimationState.ANIMATION_DISABLED_EN_ENABLED
            else -> IsolationStatusView.AnimationState.ANIMATION_DISABLED_EN_DISABLED
        }
        isolationView.animationState = animationState

        contactTracingView.gone()
        isolationView.visible()
    }

    private fun showDefaultView(exposureNotificationsEnabled: Boolean, animationsEnabled: Boolean) {
        contactTracingActiveView.isVisible = exposureNotificationsEnabled
        contactTracingActiveView.isAnimationEnabled = animationsEnabled

        contactTracingStoppedView.isVisible = !exposureNotificationsEnabled

        isolationView.gone()
        contactTracingView.visible()
        optionReportSymptoms.visible()
    }

    override fun onResume() {
        super.onResume()
        resetButtonEnabling()
        isVisible = true
        statusViewModel.onResume()
        dateChangeReceiver.registerReceiver(this) {
            statusViewModel.updateViewStateAndCheckUserInbox()
        }
    }

    private fun resetButtonEnabling() {
        optionReadAdvice.isEnabled = true
        optionReportSymptoms.isEnabled = true
        optionTestingHub.isEnabled = true
        optionVenueCheckIn.isEnabled = true
        optionAboutTheApp.isEnabled = true
        optionIsolationPayment.isEnabled = true
        optionLinkTestResult.isEnabled = true
        optionSettings.isEnabled = true
        optionToggleContactTracing.isEnabled = true
        riskAreaView.isEnabled = true
    }

    override fun onPause() {
        super.onPause()
        isVisible = false
        statusViewModel.onPause()
        dateChangeReceiver.unregisterReceiver(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        statusViewModel.onActivityResult(requestCode, resultCode)
    }

    companion object {
        var isVisible = false

        fun start(
            context: Context,
            startedFromVenueCheckInSuccess: Boolean = false,
            contactTracingHubAction: ContactTracingHubAction? = null,
            startedFromLocalMessageNotification: Boolean = false,
            startedFromRiskyVenueNotificationWithType: RiskyVenueMessageType? = null
        ) {
            context.startActivity(
                getIntent(
                    context,
                    startedFromVenueCheckInSuccess,
                    contactTracingHubAction,
                    startedFromLocalMessageNotification,
                    startedFromRiskyVenueNotificationWithType
                )
            )
        }

        private fun getIntent(
            context: Context,
            startedFromVenueCheckInSuccess: Boolean,
            contactTracingHubAction: ContactTracingHubAction?,
            startedFromLocalMessageNotification: Boolean,
            startedFromRiskyVenueNotificationWithType: RiskyVenueMessageType?
        ) =
            Intent(context, StatusActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra(STARTED_FROM_VENUE_CHECK_IN_SUCCESS, startedFromVenueCheckInSuccess)
                    putExtra(CONTACT_TRACING_HUB_ACTION_EXTRA, contactTracingHubAction)
                    putExtra(STARTED_FROM_LOCAL_MESSAGE_NOTIFICATION, startedFromLocalMessageNotification)
                    putExtra(STARTED_FROM_RISKY_VENUE_NOTIFICATION_WITH_TYPE, startedFromRiskyVenueNotificationWithType)
                }

        const val STARTED_FROM_VENUE_CHECK_IN_SUCCESS = "STARTED_FROM_VENUE_CHECK_IN_SUCCESS"
        const val CONTACT_TRACING_HUB_ACTION_EXTRA = "CONTACT_TRACING_HUB_ACTION_EXTRA"
        const val STARTED_FROM_LOCAL_MESSAGE_NOTIFICATION = "STARTED_FROM_LOCAL_MESSAGE_NOTIFICATION"
        const val STARTED_FROM_RISKY_VENUE_NOTIFICATION_WITH_TYPE = "STARTED_FROM_RISKY_VENUE_NOTIFICATION_WITH_TYPE"
    }
}
