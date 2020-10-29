package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.observe
import kotlinx.android.synthetic.main.activity_link_test_result.linkTestResultContinue
import kotlinx.android.synthetic.main.activity_link_test_result.linkTestResultEnterCodeView
import kotlinx.android.synthetic.main.activity_link_test_result.linkTestResultScrollView
import kotlinx.android.synthetic.main.view_enter_code.enterCodeEditText
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultErrorType.INVALID
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultErrorType.NO_CONNECTION
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultErrorType.UNEXPECTED
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultViewState.Error
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultViewState.Progress
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultViewState.Valid
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import javax.inject.Inject

class LinkTestResultActivity : BaseActivity(R.layout.activity_link_test_result) {

    @Inject
    lateinit var factory: ViewModelFactory<LinkTestResultViewModel>

    private val viewModel: LinkTestResultViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setNavigateUpToolbar(
            toolbar,
            R.string.status_option_link_test_result,
            R.drawable.ic_arrow_back_white
        )

        linkTestResultContinue.setOnClickListener {
            viewModel.validate(enterCodeEditText.text.toString())
        }

        viewModel.viewState().observe(this) { viewState ->
            when (viewState) {
                Progress -> handleProgress()
                Valid -> finish()
                is Error -> handleError(viewState)
            }
        }
    }

    private fun handleProgress() {
        linkTestResultEnterCodeView.handleProgress()
        linkTestResultContinue.isEnabled = false
    }

    private fun handleError(viewState: Error) {
        linkTestResultScrollView.smoothScrollTo(0, linkTestResultEnterCodeView.top)
        linkTestResultEnterCodeView.errorText = when (viewState.type) {
            INVALID -> getString(R.string.valid_auth_code_is_required)
            NO_CONNECTION -> getString(R.string.link_test_result_error_no_connection)
            UNEXPECTED -> getString(R.string.link_test_result_error_unknown)
        }
        linkTestResultEnterCodeView.handleError()
        linkTestResultContinue.isEnabled = true
    }
}
