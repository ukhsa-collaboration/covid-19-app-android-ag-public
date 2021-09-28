package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import android.content.Context
import android.content.Intent
import kotlinx.android.synthetic.main.activity_share_keys_information.shareKeysConfirm
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.assistedViewModel
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setToolbarNoNavigation
import javax.inject.Inject

class ShareKeysInformationActivity : ShareKeysBaseActivity(R.layout.activity_share_keys_information) {

    @Inject
    lateinit var factory: ShareKeysInformationViewModel.Factory
    override val viewModel: ShareKeysInformationViewModel by assistedViewModel { factory.create(bookFollowUpTest = intent.getBooleanExtra(BOOK_FOLLOW_UP_TEST_EXTRA, false)) }

    override fun inject() = appComponent.inject(this)

    override fun setupToolbar() {
        setToolbarNoNavigation(
            toolbar,
            R.string.submit_keys_information_title
        )
    }

    override fun setupOnClickListeners() {
        shareKeysConfirm.setOnSingleClickListener {
            viewModel.onShareKeysButtonClicked()
        }
    }

    override fun navigateToBookFollowUpTestActivity() {
        startActivity<BookFollowUpTestActivity>()
        setResult(RESULT_OK)
        finish()
    }

    companion object {

        fun getIntent(context: Context, bookFollowUpTest: Boolean) =
            Intent(context, ShareKeysInformationActivity::class.java)
                .apply {
                    putExtra(BOOK_FOLLOW_UP_TEST_EXTRA, bookFollowUpTest)
                }

        private const val BOOK_FOLLOW_UP_TEST_EXTRA = "BOOK_FOLLOW_UP_TEST_EXTRA"
    }
}
