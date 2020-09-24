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
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import com.google.android.gms.common.api.Status
import com.google.android.material.snackbar.Snackbar
import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.FeatureFlag.HIGH_RISK_POST_DISTRICTS
import com.jeroenmols.featureflag.framework.FeatureFlag.HIGH_RISK_VENUES
import com.jeroenmols.featureflag.framework.FeatureFlag.SELF_DIAGNOSIS
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import kotlinx.android.synthetic.main.activity_status.contactTracingActiveView
import kotlinx.android.synthetic.main.activity_status.contactTracingStoppedView
import kotlinx.android.synthetic.main.activity_status.contactTracingView
import kotlinx.android.synthetic.main.activity_status.encounterDetectionSwitch
import kotlinx.android.synthetic.main.activity_status.isolationView
import kotlinx.android.synthetic.main.activity_status.optionAboutTheApp
import kotlinx.android.synthetic.main.activity_status.optionContactTracing
import kotlinx.android.synthetic.main.activity_status.optionLinkTestResult
import kotlinx.android.synthetic.main.activity_status.optionOrderTest
import kotlinx.android.synthetic.main.activity_status.optionReadAdvice
import kotlinx.android.synthetic.main.activity_status.optionReportSymptoms
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
import uk.nhs.nhsx.covid19.android.app.qrcode.QrScannerActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.QuestionnaireActivity
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.state.IsolationExpirationActivity
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.canOrderTest
import uk.nhs.nhsx.covid19.android.app.state.canReportSymptoms
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.ExposureConsent
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.IsolationExpiration
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.TestResult
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.VenueAlert
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Risk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Unknown
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultActivity
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultActivity
import uk.nhs.nhsx.covid19.android.app.util.gone
import uk.nhs.nhsx.covid19.android.app.util.openUrl
import uk.nhs.nhsx.covid19.android.app.util.visible
import javax.inject.Inject

class StatusActivity : StatusBaseActivity(R.layout.activity_status) {

    @Inject
    lateinit var exposureStatusViewModelFactory: ViewModelFactory<ExposureStatusViewModel>
    private val exposureStatusViewModel: ExposureStatusViewModel by viewModels { exposureStatusViewModelFactory }

    @Inject
    lateinit var statusViewModelFactory: ViewModelFactory<StatusViewModel>
    private val statusViewModel: StatusViewModel by viewModels { statusViewModelFactory }

    private val dateChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            statusViewModel.onDateChanged()
        }
    }

    private lateinit var readAdviceUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        startExposureNotifications()

        startObservingExposureNotificationEnabled()

        startObservingHighRiskPostDistricts()

        startListeningToIsolationStateChanges()

        startListeningForInformationScreen()

        startListeningToExposureNotificationStopped()

        setClickListeners()

        NotificationManagerCompat.from(this)
            .cancel(NotificationProvider.EXPOSURE_REMINDER_NOTIFICATION_ID)
        val notificationFlag =
            intent.getStringExtra(NotificationProvider.TAP_EXPOSURE_NOTIFICATION_REMINDER_FLAG)
        if (!notificationFlag.isNullOrEmpty()) {
            exposureStatusViewModel.startExposureNotifications()
        }

        statusViewModel.latestAdviceUrl().observe(this) { districtAreaAwareUrl ->
            readAdviceUrl = getString(districtAreaAwareUrl)
        }

        if (RuntimeBehavior.isFeatureEnabled(FeatureFlag.IN_APP_REVIEW)) {
            checkIfInAppReviewShouldBeDisplayed()
        }
    }

    private fun checkIfInAppReviewShouldBeDisplayed() {
        val startedFromVenueCheckInSuccess =
            intent.getBooleanExtra(STARTED_FROM_VENUE_CHECK_IN_SUCCESS, false)

        if (startedFromVenueCheckInSuccess) {
            statusViewModel.attemptToStartAppReviewFlow(this)
        }
    }

    private fun setClickListeners() {
        optionReadAdvice.setOnClickListener {
            openUrl(readAdviceUrl, useInternalBrowser = true)
        }

        optionReportSymptoms.setOnClickListener {
            startActivity<QuestionnaireActivity>()
        }
        optionReportSymptoms.isVisible = RuntimeBehavior.isFeatureEnabled(SELF_DIAGNOSIS)

        optionOrderTest.setOnClickListener {
            startActivity<TestOrderingActivity>()
        }

        optionVenueCheckIn.setOnClickListener {
            QrScannerActivity.start(this)
        }
        optionVenueCheckIn.isVisible = RuntimeBehavior.isFeatureEnabled(HIGH_RISK_VENUES)

        optionAboutTheApp.setOnClickListener {
            MoreAboutAppActivity.start(this)
        }

        optionLinkTestResult.setOnClickListener {
            startActivity<LinkTestResultActivity>()
        }

        optionContactTracing.setOnClickListener {
            encounterDetectionSwitch.isChecked = !encounterDetectionSwitch.isChecked
            if (encounterDetectionSwitch.isChecked) {
                exposureStatusViewModel.startExposureNotifications()
            } else {
                exposureStatusViewModel.stopExposureNotifications()
                statusViewModel.onStopExposureNotificationsClicked()
            }
        }

        riskAreaView.setOnClickListener {
            statusViewModel.onAreaRiskLevelChanged().value?.let {
                RiskLevelActivity.start(this, it)
            }
        }
    }

    private fun startListeningForInformationScreen() {
        statusViewModel.showInformationScreen().observe(
            this,
            Observer {
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
        )
    }

    private fun startListeningToIsolationStateChanges() {
        statusViewModel.userState().observe(
            this,
            Observer { state ->
                when (state) {
                    is Default -> {
                        showDefaultView()
                    }
                    is Isolation -> {
                        isolationView.initialize(state.isolationStart, state.expiryDate)
                        optionOrderTest.isVisible =
                            state.canOrderTest && RuntimeBehavior.isFeatureEnabled(FeatureFlag.TEST_ORDERING)
                        optionReportSymptoms.isVisible =
                            state.canReportSymptoms && RuntimeBehavior.isFeatureEnabled(
                            SELF_DIAGNOSIS
                        )
                        showIsolationView()
                    }
                }
            }
        )
    }

    private fun startListeningToExposureNotificationStopped() {
        statusViewModel.onExposureNotificationStopped().observe(this) { canReceiveReminder ->
            if (canReceiveReminder) {
                showExposureNotificationReminderDialog()
            }
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
        optionOrderTest.gone()
        optionReportSymptoms.isVisible = RuntimeBehavior.isFeatureEnabled(SELF_DIAGNOSIS)
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
                Success -> Timber.d("Exposure notifications successfully started")
                is Error -> handleError(viewState.exception.message)
                is ResolutionRequired -> handleResolution(
                    viewState.status,
                    REQUEST_CODE_START_EXPOSURE_NOTIFICATION
                )
            }
        }
    }

    private fun startObservingHighRiskPostDistricts() {
        if (!RuntimeBehavior.isFeatureEnabled(HIGH_RISK_POST_DISTRICTS)) {
            return
        }
        statusViewModel.onAreaRiskLevelChanged().observe(this) { areaRiskState ->
            when (areaRiskState) {
                is Risk -> {
                    riskAreaView.text = getString(
                        areaRiskState.textResId,
                        areaRiskState.mainPostCode,
                        getString(areaRiskState.areaRiskLevelResId)
                    )
                    riskAreaView.areaRisk = areaRiskState.areaRisk.name
                    riskAreaView.visible()
                }
                is Unknown -> riskAreaView.gone()
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
        isVisible = true
        statusViewModel.onResume()
        exposureStatusViewModel.checkExposureNotificationsEnabled()
        exposureStatusViewModel.checkExposureNotificationsChanged()

        registerReceiver(dateChangedReceiver, IntentFilter(Intent.ACTION_DATE_CHANGED))
    }

    override fun onPause() {
        super.onPause()
        isVisible = false
        statusViewModel.onPause()

        unregisterReceiver(dateChangedReceiver)
    }

    private fun handleError(message: String?) {
        Snackbar.make(statusContainer, message.toString(), Snackbar.LENGTH_SHORT).show()
    }

    private fun handleResolution(status: Status, code: Int) {
        status.startResolutionForResult(this, code)
    }

    private fun showExposureNotificationReminderDialog() {
        ExposureNotificationReminderDialog(this) { duration ->
            exposureStatusViewModel.scheduleExposureNotificationReminder(duration)
        }.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_START_EXPOSURE_NOTIFICATION) {
            exposureStatusViewModel.startExposureNotifications()
        } else {
            encounterDetectionSwitch.isChecked = false
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
