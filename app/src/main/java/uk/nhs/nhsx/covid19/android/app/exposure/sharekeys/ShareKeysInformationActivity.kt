package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_share_keys_information.shareKeysConfirm
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.assistedViewModel
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysInformationViewModel.ShareKeysInformationNavigateTo
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysNavigateTo.Finish
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitKeysProgressActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setToolbarNoNavigation
import javax.inject.Inject

class ShareKeysInformationActivity : BaseActivity(R.layout.activity_share_keys_information) {

    @Inject
    lateinit var factory: ShareKeysInformationViewModel.Factory
    private val viewModel: ShareKeysInformationViewModel by assistedViewModel { factory.create(bookFollowUpTest = intent.getBooleanExtra(BOOK_FOLLOW_UP_TEST_EXTRA, false)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setupViewModelListeners()

        setToolbarNoNavigation(
            toolbar,
            R.string.submit_keys_information_title
        )

        viewModel.onCreate()

        shareKeysConfirm.setOnSingleClickListener {
            viewModel.onShareKeysButtonClicked()
        }
    }

    private fun setupViewModelListeners() {
        viewModel.navigation().observe(this) { navigateTo ->
            Timber.d("navigateTo: $navigateTo")
            when (navigateTo) {
                is ShareKeysNavigateTo.ShareKeysResultActivity -> {
                    ShareKeysResultActivity.start(this, navigateTo.bookFollowUpTest)
                    finish()
                }
                is ShareKeysNavigateTo.SubmitKeysProgressActivity -> SubmitKeysProgressActivity.startForResult(
                    this,
                    navigateTo.temporaryExposureKeys,
                    navigateTo.diagnosisKeySubmissionToken,
                    REQUEST_CODE_SUBMIT_KEYS
                )
                ShareKeysNavigateTo.StatusActivity -> StatusActivity.start(this)
                ShareKeysInformationNavigateTo.BookFollowUpTestActivity -> {
                    startActivity<BookFollowUpTestActivity>()
                    setResult(RESULT_OK)
                    finish()
                }
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

    companion object {

        fun getIntent(context: Context, bookFollowUpTest: Boolean) =
            Intent(context, ShareKeysInformationActivity::class.java)
                .apply {
                    putExtra(BOOK_FOLLOW_UP_TEST_EXTRA, bookFollowUpTest)
                }

        private const val BOOK_FOLLOW_UP_TEST_EXTRA = "BOOK_FOLLOW_UP_TEST_EXTRA"

        const val REQUEST_CODE_SUBMIT_KEYS = 1338
    }
}
