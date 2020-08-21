package uk.nhs.nhsx.covid19.android.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.observe
import uk.nhs.nhsx.covid19.android.app.MainViewModel.MainViewState.ExposureNotificationsNotAvailable
import uk.nhs.nhsx.covid19.android.app.MainViewModel.MainViewState.OnboardingCompleted
import uk.nhs.nhsx.covid19.android.app.MainViewModel.MainViewState.OnboardingPermissionsCompleted
import uk.nhs.nhsx.covid19.android.app.MainViewModel.MainViewState.OnboardingStarted
import uk.nhs.nhsx.covid19.android.app.MainViewModel.MainViewState.TabletNotSupported
import uk.nhs.nhsx.covid19.android.app.MainViewModel.MainViewState.UserNotAuthenticated
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.edgecases.DeviceNotSupportedActivity
import uk.nhs.nhsx.covid19.android.app.edgecases.TabletNotSupportedActivity
import uk.nhs.nhsx.covid19.android.app.onboarding.MainOnboardingActivity
import uk.nhs.nhsx.covid19.android.app.onboarding.authentication.AuthenticationCodeActivity
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
                TabletNotSupported -> TabletNotSupportedActivity.start(this)
                UserNotAuthenticated -> AuthenticationCodeActivity.start(this)
                OnboardingStarted -> MainOnboardingActivity.start(this)
                OnboardingCompleted -> StatusActivity.start(this)
                OnboardingPermissionsCompleted -> PostCodeActivity.start(this)
                ExposureNotificationsNotAvailable -> startActivity<DeviceNotSupportedActivity>()
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
