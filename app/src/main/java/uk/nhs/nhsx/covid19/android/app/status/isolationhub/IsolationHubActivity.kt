package uk.nhs.nhsx.covid19.android.app.status.isolationhub

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityIsolationHubBinding
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.SymptomsAfterRiskyVenueActivity
import uk.nhs.nhsx.covid19.android.app.status.isolationhub.IsolationHubViewModel.NavigationTarget.BookTest
import uk.nhs.nhsx.covid19.android.app.status.isolationhub.IsolationHubViewModel.NavigationTarget.IsolationNote
import uk.nhs.nhsx.covid19.android.app.status.isolationhub.IsolationHubViewModel.NavigationTarget.IsolationPayment
import uk.nhs.nhsx.covid19.android.app.status.testinghub.EvaluateBookTestNavigation.NavigationTarget.BookPcrTest
import uk.nhs.nhsx.covid19.android.app.status.testinghub.EvaluateBookTestNavigation.NavigationTarget.SymptomsAfterRiskyVenue
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.openUrl
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class IsolationHubActivity : BaseActivity() {

    private lateinit var binding: ActivityIsolationHubBinding

    @Inject
    lateinit var factory: ViewModelFactory<IsolationHubViewModel>
    private val viewModel: IsolationHubViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityIsolationHubBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setNavigateUpToolbar(
            binding.titleToolbar.root,
            R.string.isolation_hub_title,
            upIndicator = R.drawable.ic_arrow_back_white
        )

        startListeningToViewState()

        setUpClickListeners()

        viewModel.onCreate()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == TestOrderingActivity.REQUEST_CODE_ORDER_A_TEST || requestCode == IsolationPaymentActivity.REQUEST_CODE_URL_FETCHED) &&
            resultCode == RESULT_OK
        ) {
            finish()
        }
    }

    private fun startListeningToViewState() {
        viewModel.viewState().observe(this) { state ->
            with(binding) {
                itemIsolationPayment.isVisible = state.showIsolationPaymentButton
                itemBookTest.isVisible = state.showBookTestButton
            }
        }

        viewModel.navigationTarget().observe(this) {
            when (it) {
                is BookTest ->
                    when (it.navigationTarget) {
                        BookPcrTest ->
                            startActivityForResult(
                                TestOrderingActivity.getIntent(this),
                                TestOrderingActivity.REQUEST_CODE_ORDER_A_TEST
                            )
                        SymptomsAfterRiskyVenue ->
                            SymptomsAfterRiskyVenueActivity.start(
                                this,
                                shouldShowCancelConfirmationDialogOnCancelButtonClick = false
                            )
                    }
                IsolationPayment -> {
                    val intent = Intent(this, IsolationPaymentActivity::class.java)
                    startActivityForResult(intent, IsolationPaymentActivity.REQUEST_CODE_URL_FETCHED)
                }
                is IsolationNote -> {
                    openUrl(it.isolationNoteUrl, useInternalBrowser = false)
                    finish()
                }
            }
        }
    }

    private fun setUpClickListeners() = with(binding) {
        itemIsolationPayment.setOnSingleClickListener {
            viewModel.onItemIsolationPaymentClicked()
        }

        itemBookTest.setOnSingleClickListener {
            viewModel.onItemBookTestClicked()
        }

        itemIsolationNote.setOnSingleClickListener {
            viewModel.onItemIsolationNoteClicked()
        }
    }
}
