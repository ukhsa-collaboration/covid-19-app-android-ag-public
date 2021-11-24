package uk.nhs.nhsx.covid19.android.app.availability

import android.os.Bundle
import androidx.activity.viewModels
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityViewModel.AppAvailabilityState.AppVersionNotSupported
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityViewModel.AppAvailabilityState.DeviceSdkIsNotSupported
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityViewModel.AppAvailabilityState.Supported
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityViewModel.AppAvailabilityState.UpdateAvailable
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityAppAvailabilityBinding
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import javax.inject.Inject

class AppAvailabilityActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<AppAvailabilityViewModel>
    private val viewModel: AppAvailabilityViewModel by viewModels { factory }

    private lateinit var binding: ActivityAppAvailabilityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityAppAvailabilityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.goToPlayStore.setOnSingleClickListener {
            openAppStore()
        }

        viewModel.appAvailabilityState().observe(this) { viewState ->
            when (viewState) {
                is DeviceSdkIsNotSupported -> setUpLayoutDeviceSdkIsNotSupported(viewState.description)
                is AppVersionNotSupported -> setUpLayoutAppNotSupported(viewState.description)
                is UpdateAvailable -> setUpLayoutStoreUpdateAvailable(viewState.description)
                is Supported -> startActivity<MainActivity>()
            }
        }

        viewModel.checkAvailability()
    }

    private fun setTitleForAccessibility(text: String) {
        binding.titleText.text = text
        title = text
    }

    private fun setUpLayoutAppNotSupported(message: String) = with(binding) {
        goToPlayStore.gone()
        setTitleForAccessibility(getString(R.string.cant_run_app))
        subTitle.gone()
        description.text = message
    }

    private fun setUpLayoutDeviceSdkIsNotSupported(message: String) = with(binding) {
        goToPlayStore.gone()
        setTitleForAccessibility(getString(R.string.update_os))
        subTitle.gone()
        description.text = message
    }

    private fun setUpLayoutStoreUpdateAvailable(message: String) = with(binding) {
        goToPlayStore.visible()
        setTitleForAccessibility(getString(R.string.update_app_title))
        subTitle.gone()
        description.text = message
    }
}
