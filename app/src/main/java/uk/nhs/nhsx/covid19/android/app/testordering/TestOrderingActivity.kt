package uk.nhs.nhsx.covid19.android.app.testordering

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_test_ordering.appPrivacyNoticeLink
import kotlinx.android.synthetic.main.activity_test_ordering.bookTestForSomeoneElseLink
import kotlinx.android.synthetic.main.activity_test_ordering.orderTest
import kotlinx.android.synthetic.main.activity_test_ordering.orderTestPrivacyNoticeLink
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.util.URL_ORDER_TEST_FOR_SOMEONE_ELSE
import uk.nhs.nhsx.covid19.android.app.util.URL_ORDER_TEST_PRIVACY
import uk.nhs.nhsx.covid19.android.app.util.URL_PRIVACY_NOTICE
import uk.nhs.nhsx.covid19.android.app.util.openUrl
import uk.nhs.nhsx.covid19.android.app.util.setNavigateUpToolbar

class TestOrderingActivity : AppCompatActivity(R.layout.activity_test_ordering) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setNavigateUpToolbar(toolbar, R.string.book_free_test, R.drawable.ic_arrow_back_white)

        setupListeners()
    }

    private fun setupListeners() {
        orderTestPrivacyNoticeLink.setOnClickListener {
            openUrl(URL_ORDER_TEST_PRIVACY)
        }

        appPrivacyNoticeLink.setOnClickListener {
            openUrl(URL_PRIVACY_NOTICE)
        }

        bookTestForSomeoneElseLink.setOnClickListener {
            openUrl(URL_ORDER_TEST_FOR_SOMEONE_ELSE)
        }

        orderTest.setOnClickListener {
            startActivityForResult(
                TestOrderingProgressActivity.getIntent(this),
                REQUEST_CODE_ORDER_A_TEST
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ORDER_A_TEST && resultCode == Activity.RESULT_OK) {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    companion object {
        const val REQUEST_CODE_ORDER_A_TEST = 1338

        fun getIntent(context: Context) =
            Intent(context, TestOrderingActivity::class.java)
    }
}
