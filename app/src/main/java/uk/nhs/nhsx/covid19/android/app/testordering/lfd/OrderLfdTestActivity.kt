package uk.nhs.nhsx.covid19.android.app.testordering.lfd

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import kotlinx.android.synthetic.main.activity_order_lfd_test.alreadyHaveTestKitButton
import kotlinx.android.synthetic.main.activity_order_lfd_test.orderTestButton
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testordering.lfd.NavigationTarget.Home
import uk.nhs.nhsx.covid19.android.app.testordering.lfd.NavigationTarget.OrderTest
import uk.nhs.nhsx.covid19.android.app.util.viewutils.openInExternalBrowserForResult
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpLinkTypeWithBrowserWarning
import javax.inject.Inject

class OrderLfdTestActivity : BaseActivity(R.layout.activity_order_lfd_test) {

    @Inject
    lateinit var factory: ViewModelFactory<OrderLfdTestViewModel>
    private val viewModel: OrderLfdTestViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        setNavigateUpToolbar(toolbar, R.string.book_free_lfd_test_title, upIndicator = R.drawable.ic_arrow_back_white)

        orderTestButton.setOnSingleClickListener {
            viewModel.onOrderTestClicked()
        }

        orderTestButton.setUpLinkTypeWithBrowserWarning(orderTestButton.text)

        alreadyHaveTestKitButton.setOnSingleClickListener {
            viewModel.onAlreadyHaveTestKitClicked()
        }

        startViewModelListeners()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ORDER_TEST) {
            viewModel.onReturnedFromTestOrdering()
        }
    }

    private fun startViewModelListeners() {
        viewModel.navigationTarget().observe(this) { navigationTarget ->
            when (navigationTarget) {
                is OrderTest -> handleOrderTest(navigationTarget.url)
                Home -> navigateToStatusActivity()
            }
        }
    }

    private fun handleOrderTest(urlId: Int) {
        openInExternalBrowserForResult(getString(urlId), REQUEST_ORDER_TEST)
    }

    private fun navigateToStatusActivity() {
        StatusActivity.start(this)
    }

    companion object {
        const val REQUEST_ORDER_TEST = 1001
    }
}
