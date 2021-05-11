package uk.nhs.nhsx.covid19.android.app.status

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_status.contactTracingActiveView
import kotlinx.android.synthetic.main.activity_status.contactTracingStoppedView
import kotlinx.android.synthetic.main.activity_status.contactTracingView
import kotlinx.android.synthetic.main.activity_status.isolationView
import kotlinx.android.synthetic.main.activity_status.optionAboutTheApp
import kotlinx.android.synthetic.main.activity_status.optionIsolationPayment
import kotlinx.android.synthetic.main.activity_status.optionLinkTestResult
import kotlinx.android.synthetic.main.activity_status.optionOrderTest
import kotlinx.android.synthetic.main.activity_status.optionReadAdvice
import kotlinx.android.synthetic.main.activity_status.optionReportSymptoms
import kotlinx.android.synthetic.main.activity_status.optionSettings
import kotlinx.android.synthetic.main.activity_status.optionToggleContactTracing
import kotlinx.android.synthetic.main.activity_status.optionVenueCheckIn
import kotlinx.android.synthetic.main.activity_status.riskAreaView
import kotlinx.android.synthetic.main.activity_status.statusContainer
import kotlinx.android.synthetic.main.include_stopped.activateContactTracingButton
import kotlinx.android.synthetic.main.view_default_state.contactTracingActiveLabel
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.about.MoreAboutAppActivity
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EncounterDetectionActivity
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysInformationActivity
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysReminderActivity
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider.ContactTracingHubAction
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider.ContactTracingHubAction.NAVIGATE_AND_TURN_ON
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.QrScannerActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertBookTestActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertInformActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.QuestionnaireActivity
import uk.nhs.nhsx.covid19.android.app.remote.data.MessageType.BOOK_TEST
import uk.nhs.nhsx.covid19.android.app.remote.data.MessageType.INFORM
import uk.nhs.nhsx.covid19.android.app.settings.SettingsActivity
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.state.IsolationExpirationActivity
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.ExposureConsent
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.IsolationExpiration
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.ShareKeys
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.TestResult
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.VenueAlert
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.IsolationViewState
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.IsolationViewState.Isolating
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.IsolationViewState.NotIsolating
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.PermissionRequestResult.Error
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.PermissionRequestResult.Request
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Risk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Unknown
import uk.nhs.nhsx.covid19.android.app.status.contacttracinghub.ContactTracingHubActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultActivity
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.openUrl
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import java.time.LocalDate
import javax.inject.Inject

class StatusActivity : StatusBaseActivity(R.layout.activity_status) {

    @Inject
    lateinit var statusViewModelFactory: ViewModelFactory<StatusViewModel>
    private val statusViewModel: StatusViewModel by viewModels { statusViewModelFactory }

    @Inject
    lateinit var dateChangeReceiver: DateChangeReceiver

    private lateinit var readAdviceUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        startListeningToExposureNotificationPermissionState()

        startListeningToViewState()

        startListeningForInformationScreen()

        setClickListeners()

        val contactTracingHubAction =
            intent.getSerializableExtra(CONTACT_TRACING_HUB_ACTION_EXTRA) as? ContactTracingHubAction
        if (contactTracingHubAction != null) {
            val shouldTurnOnContactTracing = contactTracingHubAction == NAVIGATE_AND_TURN_ON
            ContactTracingHubActivity.start(this, shouldTurnOnContactTracing)
        }

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

    private fun startListeningForInformationScreen() {
        statusViewModel.showInformationScreen().observe(this) {
            when (it) {
                is IsolationExpiration -> IsolationExpirationActivity.start(
                    this,
                    it.expiryDate.toString()
                )
                TestResult -> startActivity<TestResultActivity>()
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
            }
        }
    }

    private fun startListeningToViewState() {
        statusViewModel.viewState.observe(this) { viewState ->
            readAdviceUrl = getString(viewState.latestAdviceUrl)
            handleIsolationState(viewState.isolationState, viewState.currentDate)
            handleRiskyPostCodeViewState(viewState.areaRiskState)
            handleExposureNotificationState(viewState.exposureNotificationsEnabled)
            handleIsolationPaymentState(viewState.showIsolationPaymentButton)
            handleOrderTestState(viewState.showOrderTestButton)
            handleReportSymptomsState(viewState.showReportSymptomsButton)
        }
    }

    private fun handleExposureNotificationState(exposureNotificationsEnabled: Boolean) {
        contactTracingActiveView.isVisible = exposureNotificationsEnabled
        contactTracingStoppedView.isVisible = !exposureNotificationsEnabled
        if (statusViewModel.contactTracingSwitchedOn) {
            contactTracingActiveLabel.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null)
            statusViewModel.contactTracingSwitchedOn = false
        }
        setAnimationsEnabled(exposureNotificationsEnabled)
    }

    private fun handleOrderTestState(showOrderTestButton: Boolean) {
        optionOrderTest.isVisible = showOrderTestButton
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
    }

    private fun handleIsolationState(isolationState: IsolationViewState, currentDate: LocalDate) {
        when (isolationState) {
            NotIsolating -> {
                showDefaultView()
            }
            is Isolating -> {
                isolationView.initialize(isolationState, currentDate)
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

    private fun setAnimationsEnabled(animationsEnabled: Boolean) {
        isolationView.isAnimationEnabled = animationsEnabled
        contactTracingActiveView.isAnimationEnabled = animationsEnabled
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
        optionOrderTest.isEnabled = true
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
            contactTracingHubAction: ContactTracingHubAction? = null
        ) {
            context.startActivity(getIntent(context, startedFromVenueCheckInSuccess, contactTracingHubAction))
        }

        private fun getIntent(
            context: Context,
            startedFromVenueCheckInSuccess: Boolean,
            contactTracingHubAction: ContactTracingHubAction?
        ) =
            Intent(context, StatusActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra(STARTED_FROM_VENUE_CHECK_IN_SUCCESS, startedFromVenueCheckInSuccess)
                    putExtra(CONTACT_TRACING_HUB_ACTION_EXTRA, contactTracingHubAction)
                }

        const val STARTED_FROM_VENUE_CHECK_IN_SUCCESS = "STARTED_FROM_VENUE_CHECK_IN_SUCCESS"
        const val CONTACT_TRACING_HUB_ACTION_EXTRA = "CONTACT_TRACING_HUB_ACTION_EXTRA"
    }
}
