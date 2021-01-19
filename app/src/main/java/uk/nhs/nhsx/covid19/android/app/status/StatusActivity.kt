package uk.nhs.nhsx.covid19.android.app.status

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.isVisible
import androidx.lifecycle.observe
import com.google.android.gms.common.api.Status
import com.google.android.material.snackbar.Snackbar
import javax.inject.Inject
import kotlinx.android.synthetic.main.activity_status.contactTracingActiveView
import kotlinx.android.synthetic.main.activity_status.contactTracingStoppedView
import kotlinx.android.synthetic.main.activity_status.contactTracingView
import kotlinx.android.synthetic.main.activity_status.encounterDetectionSwitch
import kotlinx.android.synthetic.main.activity_status.isolationView
import kotlinx.android.synthetic.main.activity_status.optionAboutTheApp
import kotlinx.android.synthetic.main.activity_status.optionContactTracing
import kotlinx.android.synthetic.main.activity_status.optionIsolationPayment
import kotlinx.android.synthetic.main.activity_status.optionLinkTestResult
import kotlinx.android.synthetic.main.activity_status.optionOrderTest
import kotlinx.android.synthetic.main.activity_status.optionReadAdvice
import kotlinx.android.synthetic.main.activity_status.optionReportSymptoms
import kotlinx.android.synthetic.main.activity_status.optionSettings
import kotlinx.android.synthetic.main.activity_status.optionVenueCheckIn
import kotlinx.android.synthetic.main.activity_status.riskAreaView
import kotlinx.android.synthetic.main.activity_status.statusContainer
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.about.MoreAboutAppActivity
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationActivationResult.Error
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationActivationResult.ResolutionRequired
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationActivationResult.Success
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EncounterDetectionActivity
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.QrScannerActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.QuestionnaireActivity
import uk.nhs.nhsx.covid19.android.app.settings.SettingsActivity
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.state.IsolationExpirationActivity
import uk.nhs.nhsx.covid19.android.app.state.State
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.canOrderTest
import uk.nhs.nhsx.covid19.android.app.state.canReportSymptoms
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.ExposureConsent
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.IsolationExpiration
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.TestResult
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.VenueAlert
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Risk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Unknown
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultActivity
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.openUrl
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible

class StatusActivity : StatusBaseActivity(R.layout.activity_status) {

    @Inject
    lateinit var exposureStatusViewModelFactory: ViewModelFactory<ExposureStatusViewModel>
    private val exposureStatusViewModel: ExposureStatusViewModel by viewModels { exposureStatusViewModelFactory }

    @Inject
    lateinit var statusViewModelFactory: ViewModelFactory<StatusViewModel>
    private val statusViewModel: StatusViewModel by viewModels { statusViewModelFactory }

    private val dateChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            statusViewModel.updateViewState()
        }
    }

    private lateinit var readAdviceUrl: String

    /**
     * Exposure notification dialog currently displayed, or null if none are displayed
     */
    private var currentExposureNotificationReminderDialog: ExposureNotificationReminderDialog? =
        null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        startListeningToViewState()

        startListeningForInformationScreen()

        startExposureNotifications()

        startObservingExposureNotificationEnabled()

        setClickListeners()

        NotificationManagerCompat.from(this)
            .cancel(NotificationProvider.EXPOSURE_REMINDER_NOTIFICATION_ID)
        val notificationFlag =
            intent.getStringExtra(NotificationProvider.TAP_EXPOSURE_NOTIFICATION_REMINDER_FLAG)
        if (!notificationFlag.isNullOrEmpty()) {
            exposureStatusViewModel.startExposureNotifications()
        }

        checkIfInAppReviewShouldBeDisplayed()
    }

    private fun startListeningForInformationScreen() {
        statusViewModel.showInformationScreen().observe(this) {
            when (it) {
                is IsolationExpiration -> IsolationExpirationActivity.start(
                    this,
                    it.expiryDate.toString()
                )
                TestResult -> startActivity<TestResultActivity>()
                ExposureConsent -> EncounterDetectionActivity.start(this)
                is VenueAlert -> VenueAlertActivity.start(this, it.venueId)
            }
        }
    }

    private fun startListeningToViewState() {
        statusViewModel.viewState.observe(this) { viewState ->
            readAdviceUrl = getString(viewState.latestAdviceUrl)
            handleIsolationState(viewState.isolationState)
            handleRiskyPostCodeViewState(viewState.areaRiskState)
            handleReminderDialogState(viewState.showExposureNotificationReminderDialog)
            handleIsolationPaymentState(viewState.showIsolationPaymentButton)
        }
    }

    private fun handleReminderDialogState(showExposureNotificationReminderDialog: Boolean) {
        if (showExposureNotificationReminderDialog) {
            showExposureNotificationReminderDialog()
        } else {
            dismissExposureNotificationReminderDialog()
        }
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
        optionReadAdvice.setOnSingleClickListener {
            optionReadAdvice.isEnabled = false
            openUrl(readAdviceUrl, useInternalBrowser = true)
        }

        optionReportSymptoms.setOnSingleClickListener {
            optionReportSymptoms.isEnabled = false
            startActivity<QuestionnaireActivity>()
        }

        optionOrderTest.setOnSingleClickListener {
            optionOrderTest.isEnabled = false
            startActivity<TestOrderingActivity>()
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

        optionContactTracing.setOnSingleClickListener {
            optionContactTracing.isEnabled = false
            encounterDetectionSwitch.isChecked = !encounterDetectionSwitch.isChecked
            if (encounterDetectionSwitch.isChecked) {
                exposureStatusViewModel.startExposureNotifications()
            } else {
                exposureStatusViewModel.stopExposureNotifications()
                statusViewModel.onStopExposureNotificationsClicked()
                optionContactTracing.isEnabled = true
            }
        }

        optionSettings.setOnSingleClickListener {
            optionSettings.isEnabled = false
            startActivity<SettingsActivity>()
        }

        riskAreaView.setOnSingleClickListener {
            riskAreaView.isEnabled = false
            statusViewModel.viewState.value?.let {
                RiskLevelActivity.start(this, it.areaRiskState)
            }
        }
    }

    private fun handleIsolationState(isolationState: State) {
        when (isolationState) {
            is Default -> {
                showDefaultView()
            }
            is Isolation -> {
                isolationView.initialize(isolationState)
                optionOrderTest.isVisible = isolationState.canOrderTest
                optionReportSymptoms.isVisible = isolationState.canReportSymptoms
                showIsolationView()
            }
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

    private fun showIsolationView() {
        optionReadAdvice.text = getString(R.string.status_option_read_self_isolation_advice)
        contactTracingView.gone()
        isolationView.visible()
    }

    private fun showDefaultView() {
        optionReadAdvice.text = getString(R.string.status_option_read_latest_advice)
        isolationView.gone()
        contactTracingView.visible()
        optionReportSymptoms.visible()
        optionOrderTest.gone()
    }

    private fun startExposureNotifications() {
        exposureStatusViewModel.exposureNotificationsChanged().observe(this) { isEnabled ->
            encounterDetectionSwitch.isChecked = isEnabled
            contactTracingActiveView.isVisible = isEnabled
            contactTracingStoppedView.isVisible = !isEnabled
            setAnimationsEnabled(isEnabled)
        }

        exposureStatusViewModel.exposureNotificationActivationResult().observe(this) { viewState ->
            when (viewState) {
                Success -> {
                    Timber.d("Exposure notifications successfully started")
                    optionContactTracing.isEnabled = true
                }
                is Error -> {
                    handleExposureNotificationActivationError(viewState.exception.message)
                    optionContactTracing.isEnabled = true
                }
                is ResolutionRequired -> handleResolution(
                    viewState.status,
                    REQUEST_CODE_START_EXPOSURE_NOTIFICATION
                )
            }
        }
    }

    private fun startObservingExposureNotificationEnabled() {
        exposureStatusViewModel.exposureNotificationsEnabled().observe(
            this
        ) { isEnabled ->
            setAnimationsEnabled(isEnabled)
        }
    }

    private fun setAnimationsEnabled(animationsEnabled: Boolean) {
        isolationView.isAnimationEnabled = animationsEnabled
        contactTracingActiveView.isAnimationEnabled = animationsEnabled
    }

    override fun onResume() {
        super.onResume()
        resetButtonEnabling()
        isVisible = true
        statusViewModel.onResume()
        exposureStatusViewModel.checkExposureNotificationsEnabled()
        exposureStatusViewModel.checkExposureNotificationsChanged()

        registerReceiver(dateChangedReceiver, IntentFilter(Intent.ACTION_DATE_CHANGED))
    }

    private fun resetButtonEnabling() {
        optionReadAdvice.isEnabled = true
        optionReportSymptoms.isEnabled = true
        optionOrderTest.isEnabled = true
        optionVenueCheckIn.isEnabled = true
        optionAboutTheApp.isEnabled = true
        optionIsolationPayment.isEnabled = true
        optionLinkTestResult.isEnabled = true
        optionContactTracing.isEnabled = true
        optionSettings.isEnabled = true
        riskAreaView.isEnabled = true
    }

    override fun onPause() {
        super.onPause()
        isVisible = false
        statusViewModel.onPause()

        unregisterReceiver(dateChangedReceiver)
    }

    override fun onDestroy() {
        // To avoid leaking the window
        currentExposureNotificationReminderDialog?.setOnDismissListener { }
        dismissExposureNotificationReminderDialog()

        super.onDestroy()
    }

    private fun handleExposureNotificationActivationError(message: String?) {
        encounterDetectionSwitch.isChecked = false
        Snackbar.make(statusContainer, message.toString(), Snackbar.LENGTH_SHORT).show()
    }

    private fun handleResolution(status: Status, code: Int) {
        status.startResolutionForResult(this, code)
    }

    private fun showExposureNotificationReminderDialog() {
        val dialog = ExposureNotificationReminderDialog(this) { duration ->
            exposureStatusViewModel.scheduleExposureNotificationReminder(duration)
        }
        dialog.setOnDismissListener {
            statusViewModel.onExposureNotificationReminderDialogDismissed()
            currentExposureNotificationReminderDialog = null
        }
        currentExposureNotificationReminderDialog = dialog
        dialog.show()
    }

    private fun dismissExposureNotificationReminderDialog() {
        currentExposureNotificationReminderDialog?.dismiss()
        currentExposureNotificationReminderDialog = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_START_EXPOSURE_NOTIFICATION) {
            if (resultCode == Activity.RESULT_OK) {
                exposureStatusViewModel.startExposureNotifications()
            } else {
                encounterDetectionSwitch.isChecked = false
            }
            optionContactTracing.isEnabled = true
        }
    }

    companion object {
        var isVisible = false

        fun start(context: Context, startedFromVenueCheckInSuccess: Boolean = false) {
            context.startActivity(getIntent(context, startedFromVenueCheckInSuccess))
        }

        private fun getIntent(context: Context, startedFromVenueCheckInSuccess: Boolean) =
            Intent(context, StatusActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra(STARTED_FROM_VENUE_CHECK_IN_SUCCESS, startedFromVenueCheckInSuccess)
                }

        const val STARTED_FROM_VENUE_CHECK_IN_SUCCESS = "STARTED_FROM_VENUE_CHECK_IN_SUCCESS"
        const val REQUEST_CODE_START_EXPOSURE_NOTIFICATION = 1337
    }
}
