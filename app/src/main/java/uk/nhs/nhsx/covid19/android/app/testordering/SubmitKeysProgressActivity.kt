package uk.nhs.nhsx.covid19.android.app.testordering

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.Lce
import uk.nhs.nhsx.covid19.android.app.common.ProgressActivity
import uk.nhs.nhsx.covid19.android.app.common.assistedViewModel
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import javax.inject.Inject

class SubmitKeysProgressActivity : ProgressActivity<Unit>() {

    @Inject
    lateinit var factory: SubmitKeysProgressViewModel.Factory
    private val viewModel: SubmitKeysProgressViewModel by assistedViewModel {
        factory.create(
            exposureKeys = requireNotNull(intent.getParcelableArrayListExtra(EXPOSURE_KEYS_TO_SUBMIT)) {
                INVALID_LAUNCH_EXCEPTION_MESSAGE
            },
            diagnosisKeySubmissionToken = requireNotNull(intent.getStringExtra(SHARE_KEY_DIAGNOSIS_SUBMISSION_TOKEN)) {
                INVALID_LAUNCH_EXCEPTION_MESSAGE
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun startAction() {
        viewModel.submitKeys()
    }

    override fun viewModelLiveData(): LiveData<Lce<Unit>> {
        return viewModel.submitKeysResult()
    }

    override fun onSuccess(result: Unit) {
        setResult(RESULT_OK)
        finish()
    }

    companion object {
        private const val SHARE_KEY_DIAGNOSIS_SUBMISSION_TOKEN = "SHARE_KEY_DIAGNOSIS_SUBMISSION_TOKEN"
        private const val EXPOSURE_KEYS_TO_SUBMIT = "EXPOSURE_KEYS_TO_SUBMIT"
        private const val INVALID_LAUNCH_EXCEPTION_MESSAGE = "Start this activity with startForResult call"

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
