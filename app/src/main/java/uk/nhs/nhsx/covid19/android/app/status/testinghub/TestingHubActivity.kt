package uk.nhs.nhsx.covid19.android.app.status.testinghub

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityTestingHubBinding
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.SymptomsAfterRiskyVenueActivity
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.BookTestButtonState
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.BookTestButtonState.LfdTest
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.BookTestButtonState.PcrTest
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.NavigationTarget.BookPcrTest
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.NavigationTarget.OrderLfdTest
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.NavigationTarget.SymptomsAfterRiskyVenue
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingActivity.Companion.REQUEST_CODE_ORDER_A_TEST
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.openInExternalBrowserForResult
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.widgets.NavigationItemView.NavigationItemAttributes
import javax.inject.Inject

class TestingHubActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<TestingHubViewModel>
    private val viewModel: TestingHubViewModel by viewModels { factory }
    private lateinit var binding: ActivityTestingHubBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityTestingHubBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setNavigateUpToolbar(
            binding.primaryToolbar.toolbar,
            R.string.testing_hub_title,
            upIndicator = R.drawable.ic_arrow_back_white
        )

        setUpViewModelListeners()

        setUpOnClickListeners()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_ORDER_A_TEST, REQUEST_CODE_ENTER_TEST_RESULT -> finish()
            }
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
                SymptomsAfterRiskyVenue ->
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

    private fun handlePcrTestButtonState() = with(binding) {
        itemBookTest.attributes = NavigationItemAttributes(
            isExternalLink = false,
            title = getString(R.string.testing_hub_book_test_title),
            description = getString(R.string.testing_hub_book_test_description)
        )
        itemBookTest.setOnSingleClickListener { viewModel.onBookPcrTestClicked() }
    }

    private fun handleLfdTestButtonState() = with(binding) {
        itemBookTest.attributes = NavigationItemAttributes(
            isExternalLink = true,
            title = getString(R.string.testing_hub_book_lfd_test_title),
            description = getString(R.string.testing_hub_book_lfd_test_description)
        )
        itemBookTest.setOnSingleClickListener { viewModel.onOrderLfdTestClicked() }
    }

    private fun setUpOnClickListeners() = with(binding) {
        itemEnterTestResult.setOnSingleClickListener {
            val intent = Intent(this@TestingHubActivity, LinkTestResultActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_ENTER_TEST_RESULT)
        }
    }

    companion object {
        private const val REQUEST_CODE_ENTER_TEST_RESULT = 1123
    }
}
