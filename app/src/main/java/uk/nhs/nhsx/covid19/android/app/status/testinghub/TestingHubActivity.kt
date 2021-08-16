package uk.nhs.nhsx.covid19.android.app.status.testinghub

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import kotlinx.android.synthetic.main.activity_testing_hub.itemBookTest
import kotlinx.android.synthetic.main.activity_testing_hub.itemEnterTestResult
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.SymptomsAfterRiskyVenueActivity
import uk.nhs.nhsx.covid19.android.app.widgets.NavigationItemView.NavigationItemAttributes
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.BookTestButtonState
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.BookTestButtonState.LfdTest
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.BookTestButtonState.PcrTest
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.NavigationTarget.BookPcrTest
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.NavigationTarget.OrderLfdTest
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingActivity.Companion.REQUEST_CODE_ORDER_A_TEST
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.openInExternalBrowserForResult
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class TestingHubActivity : BaseActivity(R.layout.activity_testing_hub) {

    @Inject
    lateinit var factory: ViewModelFactory<TestingHubViewModel>
    private val viewModel: TestingHubViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setNavigateUpToolbar(toolbar, R.string.testing_hub_title, upIndicator = R.drawable.ic_arrow_back_white)

        setUpViewModelListeners()

        setUpOnClickListeners()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == REQUEST_CODE_ORDER_A_TEST || requestCode == REQUEST_CODE_ENTER_TEST_RESULT) &&
            resultCode == Activity.RESULT_OK
        ) {
            finish()
        }
    }

    private fun setUpViewModelListeners() {
        viewModel.viewState().observe(this) { viewState ->
            handleBookTestButtonState(viewState.bookTestButtonState)
        }

        viewModel.navigationTarget().observe(this) { navigationTarget ->
            when (navigationTarget) {
                BookPcrTest ->
                    startActivityForResult(
                        TestOrderingActivity.getIntent(this),
                        REQUEST_CODE_ORDER_A_TEST
                    )
                is OrderLfdTest -> {
                    openInExternalBrowserForResult(getString(navigationTarget.urlResId))
                    finish()
                }
                NavigationTarget.SymptomsAfterRiskyVenue ->
                    SymptomsAfterRiskyVenueActivity.start(
                        this,
                        shouldShowCancelConfirmationDialogOnCancelButtonClick = false
                    )
            }
        }
    }

    private fun handleBookTestButtonState(buttonState: BookTestButtonState) {
        when (buttonState) {
            PcrTest -> handlePcrTestButtonState()
            LfdTest -> handleLfdTestButtonState()
        }
    }

    private fun handlePcrTestButtonState() {
        itemBookTest.attributes = NavigationItemAttributes(
            isExternalLink = false,
            title = getString(R.string.testing_hub_book_test_title),
            description = getString(R.string.testing_hub_book_test_description)
        )
        itemBookTest.setOnSingleClickListener { viewModel.onBookPcrTestClicked() }
    }

    private fun handleLfdTestButtonState() {
        itemBookTest.attributes = NavigationItemAttributes(
            isExternalLink = true,
            title = getString(R.string.testing_hub_book_lfd_test_title),
            description = getString(R.string.testing_hub_book_lfd_test_description)
        )
        itemBookTest.setOnSingleClickListener { viewModel.onOrderLfdTestClicked() }
    }

    private fun setUpOnClickListeners() {
        itemEnterTestResult.setOnSingleClickListener {
            val intent = Intent(this, LinkTestResultActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_ENTER_TEST_RESULT)
        }
    }

    companion object {
        private const val REQUEST_CODE_ENTER_TEST_RESULT = 1123
    }
}
