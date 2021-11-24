package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import android.content.Intent
import android.os.Bundle
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysInformationViewModel.ShareKeysInformationNavigateTo
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysNavigateTo.Finish
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitKeysProgressActivity

abstract class ShareKeysBaseActivity : BaseActivity() {
    protected abstract val viewModel: ShareKeysBaseViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()

        setupBinding()

        setupViewModelListeners()

        setupToolbar()

        viewModel.onCreate()

        setupOnClickListeners()
    }

    protected abstract fun inject()

    protected abstract fun setupBinding()

    protected open fun setupToolbar() = Unit

    protected abstract fun setupOnClickListeners()

    private fun setupViewModelListeners() {
        viewModel.navigation().observe(this) { navigateTo ->
            Timber.d("navigateTo: $navigateTo")
            when (navigateTo) {
                is ShareKeysNavigateTo.ShareKeysResultActivity -> navigateToShareKeysResultActivity(navigateTo)
                is ShareKeysNavigateTo.SubmitKeysProgressActivity ->
                    SubmitKeysProgressActivity.startForResult(
                        this,
                        navigateTo.temporaryExposureKeys,
                        navigateTo.diagnosisKeySubmissionToken,
                        REQUEST_CODE_SUBMIT_KEYS
                    )
                ShareKeysNavigateTo.StatusActivity -> StatusActivity.start(this)
                ShareKeysInformationNavigateTo.BookFollowUpTestActivity -> navigateToBookFollowUpTestActivity()
                Finish -> finish()
            }
        }

        viewModel.permissionRequest().observe(this) { permissionRequest ->
            permissionRequest(this)
        }
    }

    private fun navigateToShareKeysResultActivity(navigateTo: ShareKeysNavigateTo.ShareKeysResultActivity) {
        ShareKeysResultActivity.start(this, navigateTo.bookFollowUpTest)
        finish()
    }

    protected open fun navigateToBookFollowUpTestActivity() = Unit

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Timber.d("onActivityResult: requestCode = $requestCode resultCode = $resultCode")

        viewModel.onActivityResult(requestCode, resultCode)
    }

    override fun onBackPressed() = Unit

    companion object {
        const val REQUEST_CODE_SUBMIT_KEYS = 1338
    }
}
