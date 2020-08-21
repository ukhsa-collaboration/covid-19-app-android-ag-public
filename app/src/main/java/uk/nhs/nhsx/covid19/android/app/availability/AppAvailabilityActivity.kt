package uk.nhs.nhsx.covid19.android.app.availability

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.observe
import kotlinx.android.synthetic.main.activity_app_availability.description
import kotlinx.android.synthetic.main.activity_app_availability.goToPlayStore
import kotlinx.android.synthetic.main.activity_app_availability.subTitle
import kotlinx.android.synthetic.main.activity_app_availability.titleText
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityViewModel.AppAvailabilityState.AppVersionNotSupported
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityViewModel.AppAvailabilityState.DeviceSdkIsNotSupported
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityViewModel.AppAvailabilityState.Supported
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityViewModel.AppAvailabilityState.UpdateAvailable
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.util.gone
import uk.nhs.nhsx.covid19.android.app.util.visible
import javax.inject.Inject

class AppAvailabilityActivity : BaseActivity(R.layout.activity_app_availability) {

    @Inject
    lateinit var factory: ViewModelFactory<AppAvailabilityViewModel>
    private val viewModel: AppAvailabilityViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        goToPlayStore.setOnClickListener {
            viewModel.startUpdate(
                this,
                UPDATE_REQUEST_CODE
            )
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                Timber.d("Update flow failed! Result code: $resultCode")
            }
        }
    }

    private fun setUpLayoutAppNotSupported(message: String) {
        goToPlayStore.gone()
        titleText.text = getString(R.string.cant_run_app)
        subTitle.gone()
        description.text = message
    }

    private fun setUpLayoutDeviceSdkIsNotSupported(message: String) {
        goToPlayStore.gone()
        titleText.text = getString(R.string.update_os)
        subTitle.gone()
        description.text = message
    }

    private fun setUpLayoutStoreUpdateAvailable(message: String) {
        goToPlayStore.visible()
        titleText.text = getString(R.string.update_app_title)
        subTitle.gone()
        description.text = message
    }

    companion object {
        const val UPDATE_REQUEST_CODE = 101
    }
}
