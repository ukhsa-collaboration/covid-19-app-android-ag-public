package uk.nhs.nhsx.covid19.android.app.testordering

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.observe
import kotlinx.android.synthetic.main.activity_progress.buttonTryAgain
import kotlinx.android.synthetic.main.activity_progress.errorStateContainer
import kotlinx.android.synthetic.main.activity_progress.loadingProgress
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.exposure.ShareKeysInformationActivity
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setCloseToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import javax.inject.Inject

class SubmitKeysProgressActivity : BaseActivity(R.layout.activity_progress) {

    @Inject
    lateinit var factory: ViewModelFactory<SubmitKeysProgressViewModel>

    private val viewModel: SubmitKeysProgressViewModel by viewModels { factory }

    private var exposureKeys: List<NHSTemporaryExposureKey> = listOf()
    private lateinit var diagnosisKeySubmissionToken: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setCloseToolbar(toolbar, R.string.empty, R.drawable.ic_close_primary)

        exposureKeys = intent.getParcelableArrayListExtra(EXPOSURE_KEYS_TO_SUBMIT) ?: return
        diagnosisKeySubmissionToken = intent.getStringExtra(ShareKeysInformationActivity.SHARE_KEY_DIAGNOSIS_SUBMISSION_TOKEN) ?: return

        setupListeners()
        startViewModelListeners()

        submitKeys()
    }

    private fun submitKeys() {
        showLoadingSpinner()
        viewModel.submitKeys(exposureKeys, diagnosisKeySubmissionToken)
    }

    private fun setupListeners() {
        buttonTryAgain.setOnClickListener {
            submitKeys()
        }
    }

    private fun startViewModelListeners() {
        viewModel.submitKeysResult().observe(this) { submissionResult ->
            when (submissionResult) {
                is Result.Success -> StatusActivity.start(this)
                is Result.Failure -> showErrorState()
            }
        }
    }

    private fun showLoadingSpinner() {
        errorStateContainer.gone()
        loadingProgress.visible()
    }

    private fun showErrorState() {
        errorStateContainer.visible()
        loadingProgress.gone()
    }

    companion object {
        private const val EXPOSURE_KEYS_TO_SUBMIT = "EXPOSURE_KEYS_TO_SUBMIT"

        fun start(
            context: Context,
            exposureKeys: List<NHSTemporaryExposureKey>,
            diagnosisKeySubmissionToken: String?
        ) =
            context.startActivity(
                getIntent(context)
                    .putParcelableArrayListExtra(EXPOSURE_KEYS_TO_SUBMIT, ArrayList(exposureKeys))
                    .putExtra(ShareKeysInformationActivity.SHARE_KEY_DIAGNOSIS_SUBMISSION_TOKEN, diagnosisKeySubmissionToken)
            )

        fun getIntent(context: Context): Intent {
            return Intent(context, SubmitKeysProgressActivity::class.java)
        }
    }
}
