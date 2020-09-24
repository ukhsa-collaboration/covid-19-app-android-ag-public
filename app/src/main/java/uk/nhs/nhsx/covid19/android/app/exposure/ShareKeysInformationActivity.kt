package uk.nhs.nhsx.covid19.android.app.exposure

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import kotlinx.android.synthetic.main.activity_share_keys_information.shareKeysConfirm
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.exposure.FetchTemporaryExposureKeys.TemporaryExposureKeysFetchResult.Failure
import uk.nhs.nhsx.covid19.android.app.exposure.FetchTemporaryExposureKeys.TemporaryExposureKeysFetchResult.ResolutionRequired
import uk.nhs.nhsx.covid19.android.app.exposure.FetchTemporaryExposureKeys.TemporaryExposureKeysFetchResult.Success
import uk.nhs.nhsx.covid19.android.app.status.ExposureStatusViewModel.Companion.REQUEST_CODE_SUBMIT_KEYS_PERMISSION
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitKeysProgressActivity
import uk.nhs.nhsx.covid19.android.app.util.setNavigateUpToolbar
import javax.inject.Inject

class ShareKeysInformationActivity : BaseActivity(R.layout.activity_share_keys_information) {

    @Inject
    lateinit var factory: ViewModelFactory<ShareKeysInformationViewModel>

    private val viewModel: ShareKeysInformationViewModel by viewModels { factory }

    private var diagnosisKeySubmissionToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setNavigateUpToolbar(
            toolbar,
            R.string.submit_keys_information_title,
            R.drawable.ic_arrow_back_white
        )

        diagnosisKeySubmissionToken = intent.getStringExtra(SHARE_KEY_DIAGNOSIS_SUBMISSION_TOKEN)

        shareKeysConfirm.setOnClickListener {
            viewModel.fetchKeys()
        }

        setupViewModelListeners()
    }

    private fun setupViewModelListeners() {
        viewModel.fetchKeysResult().observe(
            this,
            Observer { result ->
                when (result) {
                    is Success -> {
                        SubmitKeysProgressActivity.start(this, result.temporaryExposureKeys, diagnosisKeySubmissionToken)
                    }
                    is Failure -> {
                        if (result.throwable is ApiException &&
                            result.throwable.statusCode == ConnectionResult.DEVELOPER_ERROR
                        ) {
                            showToast("You have to enable exposure notifications in your settings to be able to share your IDs")
                        } else {
                            finish()
                        }
                    }
                    is ResolutionRequired -> handleSubmitKeysResolution(result.status)
                }
            }
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun handleSubmitKeysResolution(status: Status) {
        status.startResolutionForResult(this, REQUEST_CODE_SUBMIT_KEYS_PERMISSION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_SUBMIT_KEYS_PERMISSION) {
            viewModel.fetchKeys()
        } else {
            StatusActivity.start(this)
        }
    }

    companion object {
        const val SHARE_KEY_DIAGNOSIS_SUBMISSION_TOKEN = "SHARE_KEY_DIAGNOSIS_SUBMISSION_TOKEN"
    }
}
