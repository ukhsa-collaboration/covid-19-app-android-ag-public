package uk.nhs.nhsx.covid19.android.app.testordering

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_test_ordering.orderTest
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.util.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.setUpOpensInBrowserWarning

class TestOrderingActivity : BaseActivity(R.layout.activity_test_ordering) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setNavigateUpToolbar(toolbar, R.string.book_free_test, R.drawable.ic_arrow_back_white)

        orderTest.setUpOpensInBrowserWarning()
        setupListeners()
    }

    private fun setupListeners() {
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
