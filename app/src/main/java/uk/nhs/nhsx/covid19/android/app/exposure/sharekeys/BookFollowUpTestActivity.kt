package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_book_follow_up_test.bookFollowUpTestButton
import kotlinx.android.synthetic.main.view_toolbar_background.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setCloseToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener

class BookFollowUpTestActivity : BaseActivity(R.layout.activity_book_follow_up_test) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setCloseToolbar(toolbar, closeIndicator = R.drawable.ic_close_primary, titleResId = R.string.empty)

        bookFollowUpTestButton.setOnSingleClickListener {
            startActivityForResult(
                TestOrderingActivity.getIntent(this),
                TestOrderingActivity.REQUEST_CODE_ORDER_A_TEST
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TestOrderingActivity.REQUEST_CODE_ORDER_A_TEST && resultCode == Activity.RESULT_OK) {
            finish()
        }
    }
}
