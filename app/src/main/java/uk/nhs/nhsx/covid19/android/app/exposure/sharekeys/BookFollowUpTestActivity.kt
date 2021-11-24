package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityBookFollowUpTestBinding
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setCloseToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener

class BookFollowUpTestActivity : BaseActivity() {

    private lateinit var binding: ActivityBookFollowUpTestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookFollowUpTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {

            setCloseToolbar(
                primaryToolbar.toolbar,
                closeIndicator = R.drawable.ic_close_primary,
                titleResId = R.string.empty
            )

            bookFollowUpTestButton.setOnSingleClickListener {
                startActivityForResult(
                    TestOrderingActivity.getIntent(this@BookFollowUpTestActivity),
                    TestOrderingActivity.REQUEST_CODE_ORDER_A_TEST
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TestOrderingActivity.REQUEST_CODE_ORDER_A_TEST && resultCode == Activity.RESULT_OK) {
            finish()
        }
    }
}
