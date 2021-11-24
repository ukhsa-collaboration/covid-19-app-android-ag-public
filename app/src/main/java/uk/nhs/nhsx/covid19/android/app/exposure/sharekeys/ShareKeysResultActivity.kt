package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityShareKeysResultBinding
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class ShareKeysResultActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<ShareKeysResultViewModel>
    private val viewModel: ShareKeysResultViewModel by viewModels { factory }

    private lateinit var binding: ActivityShareKeysResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityShareKeysResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.onCreate()

        val bookFollowUpTest = intent.getBooleanExtra(BOOK_FOLLOW_UP_TEST_EXTRA, false)

        setUpActionButton(bookFollowUpTest)
    }

    private fun setUpActionButton(bookFollowUpTest: Boolean) {
        val buttonTextResId = if (bookFollowUpTest) R.string.continue_button else R.string.back_to_home

        with(binding) {
            actionButton.text = getString(buttonTextResId)

            actionButton.setOnSingleClickListener {
                if (bookFollowUpTest) {
                    navigateToBookFollowUpTestActivity()
                } else {
                    navigateToStatusActivity()
                }
            }
        }
    }

    private fun navigateToStatusActivity() {
        StatusActivity.start(this)
        finish()
    }

    private fun navigateToBookFollowUpTestActivity() {
        startActivity<BookFollowUpTestActivity>()
        finish()
    }

    override fun onBackPressed() = Unit

    companion object {
        fun start(context: Context, bookFollowUpTest: Boolean = false) {
            context.startActivity(getIntent(context, bookFollowUpTest))
        }

        private fun getIntent(context: Context, bookFollowUpTest: Boolean) =
            Intent(context, ShareKeysResultActivity::class.java)
                .apply {
                    putExtra(BOOK_FOLLOW_UP_TEST_EXTRA, bookFollowUpTest)
                }

        private const val BOOK_FOLLOW_UP_TEST_EXTRA = "BOOK_FOLLOW_UP_TEST_EXTRA"
    }
}
