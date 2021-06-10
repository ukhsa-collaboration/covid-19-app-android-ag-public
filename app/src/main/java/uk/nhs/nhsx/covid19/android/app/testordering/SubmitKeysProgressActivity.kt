package uk.nhs.nhsx.covid19.android.app.testordering

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_progress.buttonTryAgain
import kotlinx.android.synthetic.main.activity_progress.errorStateContainer
import kotlinx.android.synthetic.main.activity_progress.loadingProgress
import kotlinx.android.synthetic.main.activity_progress.textErrorTitle
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.util.viewutils.announce
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.interruptAnnouncement
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setCloseToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import javax.inject.Inject

class SubmitKeysProgressActivity : BaseActivity(R.layout.activity_progress) {

    @Inject
    lateinit var factory: ViewModelFactory<SubmitKeysProgressViewModel>

    private val viewModel: SubmitKeysProgressViewModel by viewModels { factory }

    private lateinit var exposureKeys: List<NHSTemporaryExposureKey>
    private lateinit var diagnosisKeySubmissionToken: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setCloseToolbar(toolbar, R.string.empty, R.drawable.ic_close_primary)

        val exposureKeys = intent.getParcelableArrayListExtra<NHSTemporaryExposureKey>(EXPOSURE_KEYS_TO_SUBMIT)
        val diagnosisKeySubmissionToken = intent.getStringExtra(SHARE_KEY_DIAGNOSIS_SUBMISSION_TOKEN)

        if (exposureKeys == null || diagnosisKeySubmissionToken == null) {
            finish()
            return
        }

        this.exposureKeys = exposureKeys
        this.diagnosisKeySubmissionToken = diagnosisKeySubmissionToken

        setupListeners()
        startViewModelListeners()

        submitKeys()
    }

    private fun submitKeys() {
        showLoadingSpinner()
        viewModel.submitKeys(exposureKeys, diagnosisKeySubmissionToken)
    }

    private fun setupListeners() {
        buttonTryAgain.setOnSingleClickListener {
            submitKeys()
        }
    }

    private fun startViewModelListeners() {
        viewModel.submitKeysResult().observe(this) { submissionResult ->
            when (submissionResult) {
                is Result.Success -> {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                is Result.Failure -> showErrorState()
            }
        }
    }

    private fun showLoadingSpinner() {
        interruptAnnouncement()
        announce(R.string.loading)
        errorStateContainer.gone()
        loadingProgress.visible()
    }

    private fun showErrorState() {
        errorStateContainer.visible()
        loadingProgress.gone()
        interruptAnnouncement()
        textErrorTitle.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null)
    }

    companion object {
        private const val SHARE_KEY_DIAGNOSIS_SUBMISSION_TOKEN = "SHARE_KEY_DIAGNOSIS_SUBMISSION_TOKEN"
        private const val EXPOSURE_KEYS_TO_SUBMIT = "EXPOSURE_KEYS_TO_SUBMIT"

        fun startForResult(
            context: AppCompatActivity,
            exposureKeys: List<NHSTemporaryExposureKey>,
            diagnosisKeySubmissionToken: String,
            requestCode: Int
        ) =
            context.startActivityForResult(
                getIntent(context)
                    .putParcelableArrayListExtra(
                        EXPOSURE_KEYS_TO_SUBMIT,
                        ArrayList(exposureKeys)
                    )
                    .putExtra(
                        SHARE_KEY_DIAGNOSIS_SUBMISSION_TOKEN,
                        diagnosisKeySubmissionToken
                    ),
                requestCode
            )

        fun getIntent(context: Context): Intent {
            return Intent(context, SubmitKeysProgressActivity::class.java)
        }
    }
}
