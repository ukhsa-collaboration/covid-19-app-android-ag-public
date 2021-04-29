package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import kotlinx.android.synthetic.main.activity_share_keys_reminder.doNotShareResultsButton
import kotlinx.android.synthetic.main.activity_share_keys_reminder.shareResultsButton
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.exposure.ShareKeysResultActivity
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysNavigateTo.Finish
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitKeysProgressActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class ShareKeysReminderActivity : BaseActivity(R.layout.activity_share_keys_reminder) {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory<ShareKeysReminderViewModel>
    private val viewModel: ShareKeysReminderViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setupViewModelListeners()

        viewModel.onCreate()

        setupOnClickListeners()
    }

    private fun setupOnClickListeners() {
        shareResultsButton.setOnSingleClickListener {
            viewModel.onShareKeysButtonClicked()
        }

        doNotShareResultsButton.setOnSingleClickListener {
            viewModel.onDoNotShareKeysClicked()
        }
    }

    private fun setupViewModelListeners() {
        viewModel.navigation().observe(this) { navigateTo ->
            Timber.d("navigateTo: $navigateTo")
            when (navigateTo) {
                ShareKeysNavigateTo.ShareKeysResultActivity -> {
                    ShareKeysResultActivity.start(this)
                    finish()
                }
                is ShareKeysNavigateTo.SubmitKeysProgressActivity ->
                    SubmitKeysProgressActivity.startForResult(
                        this,
                        navigateTo.temporaryExposureKeys,
                        navigateTo.diagnosisKeySubmissionToken,
                        ShareKeysInformationActivity.REQUEST_CODE_SUBMIT_KEYS
                    )
                ShareKeysNavigateTo.StatusActivity -> StatusActivity.start(this)
                Finish -> finish()
            }
        }

        viewModel.permissionRequest().observe(this) { permissionRequest ->
            permissionRequest(this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Timber.d("onActivityResult: requestCode = $requestCode resultCode = $resultCode")

        viewModel.onActivityResult(requestCode, resultCode)
    }

    override fun onBackPressed() = Unit
}
