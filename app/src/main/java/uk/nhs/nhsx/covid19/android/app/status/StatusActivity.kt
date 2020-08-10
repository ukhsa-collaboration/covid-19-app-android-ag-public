package uk.nhs.nhsx.covid19.android.app.status

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.viewModels
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
import kotlinx.android.synthetic.main.activity_status.optionOrderTest
import kotlinx.android.synthetic.main.activity_status.optionReadAdvice
import kotlinx.android.synthetic.main.activity_status.optionReadSelfIsolationAdvice
import kotlinx.android.synthetic.main.activity_status.optionReportSymptoms
import kotlinx.android.synthetic.main.activity_status.optionVenueCheckIn
import kotlinx.android.synthetic.main.activity_status.riskAreaView
import kotlinx.android.synthetic.main.activity_status.statusContainer
import kotlinx.android.synthetic.main.banner_home.aboutImageView
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.about.MoreAboutAppActivity
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationActivationResult.Error
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationActivationResult.ResolutionRequired
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationActivationResult.Success
import uk.nhs.nhsx.covid19.android.app.exposure.SubmitTemporaryExposureKeys.SubmitResult
import uk.nhs.nhsx.covid19.android.app.exposure.SubmitTemporaryExposureKeys.SubmitResult.Failure
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EncounterDetectionActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.QrScannerActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.QuestionnaireActivity
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.state.IsolationExpirationActivity
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.canOrderTest
import uk.nhs.nhsx.covid19.android.app.state.canReportSymptoms
import uk.nhs.nhsx.covid19.android.app.status.ExposureStatusViewModel.Companion.REQUEST_CODE_SUBMIT_KEYS_PERMISSION
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.ExposureConsent
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.IsolationExpiration
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.TestResult
import uk.nhs.nhsx.covid19.android.app.status.InformationScreen.VenueAlert
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.HighRisk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.LowRisk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.MediumRisk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Unknown
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultActivity
import uk.nhs.nhsx.covid19.android.app.util.URL_ABOUT_THE_APP
import uk.nhs.nhsx.covid19.android.app.util.URL_ISOLATION_ADVICE
import uk.nhs.nhsx.covid19.android.app.util.URL_LATEST_ADVICE
import uk.nhs.nhsx.covid19.android.app.util.URL_POSTAL_CODE_RISK_MORE_INFO
import uk.nhs.nhsx.covid19.android.app.util.gone
import uk.nhs.nhsx.covid19.android.app.util.openUrl
import uk.nhs.nhsx.covid19.android.app.util.showSnackBarShort
import uk.nhs.nhsx.covid19.android.app.util.visible
import javax.inject.Inject

class StatusActivity : BaseActivity(R.layout.activity_status) {

    @Inject
    lateinit var exposureStatusViewModelFactory: ViewModelFactory<ExposureStatusViewModel>

    internal val exposureStatusViewModel: ExposureStatusViewModel by viewModels {
        exposureStatusViewModelFactory
    }

    @Inject
    lateinit var statusViewModelFactory: ViewModelFactory<StatusViewModel>

    private val statusViewModel: StatusViewModel by viewModels { statusViewModelFactory }

    private val dateChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            statusViewModel.onDateChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        startExposureNotifications()

        startObservingHighRiskPostDistricts()

        startListeningToIsolationStateChanges()

        startListeningForInformationScreen()

        setClickListeners()
    }

    private fun setClickListeners() {
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
            openUrl(URL_ABOUT_THE_APP)
        }

        optionContactTracing.setOnClickListener {
            encounterDetectionSwitch.isChecked = !encounterDetectionSwitch.isChecked
            if (encounterDetectionSwitch.isChecked) {
                exposureStatusViewModel.startExposureNotifications()
            } else {
                exposureStatusViewModel.stopExposureNotifications()
            }
        }

        aboutImageView.setOnClickListener {
            MoreAboutAppActivity.start(this)
        }

        riskAreaView.setOnViewMoreClickListener {
            openUrl(URL_POSTAL_CODE_RISK_MORE_INFO)
        }

        optionReadAdvice.setOnClickListener {
            openUrl(URL_LATEST_ADVICE, useInternalBrowser = true)
        }

        optionReadSelfIsolationAdvice.setOnClickListener {
            openUrl(URL_ISOLATION_ADVICE, useInternalBrowser = true)
        }
    }

    private fun startListeningForInformationScreen() {
        statusViewModel.showInformationScreen().observe(
            this,
            Observer {
                when (it) {
                    is IsolationExpiration -> startActivity<IsolationExpirationActivity> {
                        putExtra(
                            IsolationExpirationActivity.EXTRA_EXPIRY_DATE,
                            it.expiryDate.toString()
                        )
                    }
                    TestResult -> startActivity<TestResultActivity>()
                    ExposureConsent -> EncounterDetectionActivity.start(this)
                    is VenueAlert -> startActivity<VenueAlertActivity>() {
                        putExtra(
                            VenueAlertActivity.EXTRA_VENUE_ID,
                            it.venueId
                        )
                    }
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
                        optionOrderTest.isVisible = state.canOrderTest && RuntimeBehavior.isFeatureEnabled(FeatureFlag.TEST_ORDERING)
                        optionReportSymptoms.isVisible = state.canReportSymptoms && RuntimeBehavior.isFeatureEnabled(SELF_DIAGNOSIS)
                        optionReadAdvice.gone()
                        optionReadSelfIsolationAdvice.visible()
                        showIsolationView()
                    }
                }
            }
        )
    }

    private fun showIsolationView() {
        contactTracingView.gone()
        isolationView.visible()
    }

    private fun showDefaultView() {
        isolationView.gone()
        contactTracingView.visible()
        optionOrderTest.gone()
        optionReportSymptoms.isVisible = RuntimeBehavior.isFeatureEnabled(SELF_DIAGNOSIS)
        optionReadAdvice.visible()
        optionReadSelfIsolationAdvice.gone()
    }

    private fun startExposureNotifications() {
        exposureStatusViewModel.exposureNotificationsEnabled().observe(this) { isEnabled ->
            encounterDetectionSwitch.isChecked = isEnabled
            isolationView.isAnimationEnabled = isEnabled
            contactTracingActiveView.isVisible = isEnabled
            contactTracingActiveView.isAnimationEnabled = isEnabled
            contactTracingStoppedView.isVisible = !isEnabled
        }

        exposureStatusViewModel.submitKeyLiveData.observe(this) { result ->
            when (result) {
                SubmitResult.Success -> showSnackBarShort(
                    statusContainer,
                    "Keys successfully uploaded"
                )
                is Failure -> handleError(result.throwable.toString())
                is SubmitResult.ResolutionRequired -> handleResolution(
                    result.status,
                    REQUEST_CODE_SUBMIT_KEYS_PERMISSION
                )
            }
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
        statusViewModel.areaRiskState().observe(this) { areaRiskState ->
            when (areaRiskState) {
                is LowRisk -> {
                    riskAreaView.text = getString(
                        R.string.status_area_risk_level,
                        areaRiskState.mainPostCode,
                        getString(R.string.status_area_risk_level_low)
                    )
                    riskAreaView.colorResId = R.color.nhs_button_green
                    riskAreaView.visible()
                }
                is MediumRisk -> {
                    riskAreaView.text = getString(
                        R.string.status_area_risk_level,
                        areaRiskState.mainPostCode,
                        getString(R.string.status_area_risk_level_medium)
                    )
                    riskAreaView.colorResId = R.color.amber
                    riskAreaView.visible()
                }
                is HighRisk -> {
                    riskAreaView.text = getString(
                        R.string.status_area_risk_level,
                        areaRiskState.mainPostCode,
                        getString(R.string.status_area_risk_level_high)
                    )
                    riskAreaView.colorResId = R.color.error_red
                    riskAreaView.visible()
                }
                is Unknown -> riskAreaView.gone()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isVisible = true
        statusViewModel.onResume()
        exposureStatusViewModel.checkExposureNotificationsEnabled()

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_START_EXPOSURE_NOTIFICATION -> exposureStatusViewModel.startExposureNotifications()
                REQUEST_CODE_SUBMIT_KEYS_PERMISSION -> exposureStatusViewModel.submitKeys()
            }
        } else if (resultCode == Activity.RESULT_CANCELED && requestCode == REQUEST_CODE_START_EXPOSURE_NOTIFICATION) {
            encounterDetectionSwitch.isChecked = false
        }
    }

    companion object {
        var isVisible = false

        fun start(context: Context) {
            context.startActivity(getIntent(context))
        }

        private fun getIntent(context: Context) =
            Intent(context, StatusActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

        const val REQUEST_CODE_START_EXPOSURE_NOTIFICATION = 1337
    }
}
