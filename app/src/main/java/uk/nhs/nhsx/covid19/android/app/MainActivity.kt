package uk.nhs.nhsx.covid19.android.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import uk.nhs.nhsx.covid19.android.app.MainViewModel.MainViewState.BatteryOptimizationNotAcknowledged
import uk.nhs.nhsx.covid19.android.app.MainViewModel.MainViewState.Completed
import uk.nhs.nhsx.covid19.android.app.MainViewModel.MainViewState.ExposureNotificationsNotAvailable
import uk.nhs.nhsx.covid19.android.app.MainViewModel.MainViewState.LocalAuthorityMissing
import uk.nhs.nhsx.covid19.android.app.MainViewModel.MainViewState.OnboardingStarted
import uk.nhs.nhsx.covid19.android.app.MainViewModel.MainViewState.PolicyUpdated
import uk.nhs.nhsx.covid19.android.app.MainViewModel.MainViewState.PostCodeToLocalAuthorityMissing
import uk.nhs.nhsx.covid19.android.app.battery.BatteryOptimizationActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityInformationActivity
import uk.nhs.nhsx.covid19.android.app.edgecases.DeviceNotSupportedActivity
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider.ContactTracingHubAction
import uk.nhs.nhsx.covid19.android.app.onboarding.PolicyUpdateActivity
import uk.nhs.nhsx.covid19.android.app.onboarding.WelcomeActivity
import uk.nhs.nhsx.covid19.android.app.onboarding.postcode.PostCodeActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<MainViewModel>

    private val viewModel by viewModels<MainViewModel> { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        viewModel.viewState().observe(this) { mainViewState ->
            when (mainViewState) {
                OnboardingStarted -> WelcomeActivity.start(this)
                PolicyUpdated -> PolicyUpdateActivity.start(this)
                PostCodeToLocalAuthorityMissing -> PostCodeActivity.start(this, missingLocalAuthorityMapping = true)
                LocalAuthorityMissing -> startActivity<LocalAuthorityInformationActivity>()
                Completed -> startStatusActivity()
                ExposureNotificationsNotAvailable -> startActivity<DeviceNotSupportedActivity>()
                BatteryOptimizationNotAcknowledged -> startActivity<BatteryOptimizationActivity>()
            }.also {
                finish()
            }
        }

        viewModel.start()
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
    }

    private fun startStatusActivity() {
        NotificationManagerCompat.from(this).cancel(NotificationProvider.EXPOSURE_REMINDER_NOTIFICATION_ID)
        val contactTracingHubAction =
            intent.getSerializableExtra(NotificationProvider.CONTACT_TRACING_HUB_ACTION) as? ContactTracingHubAction
        StatusActivity.start(this, contactTracingHubAction = contactTracingHubAction)
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, MainActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
    }
}
