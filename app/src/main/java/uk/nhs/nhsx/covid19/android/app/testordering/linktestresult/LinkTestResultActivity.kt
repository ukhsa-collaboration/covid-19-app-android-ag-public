package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import androidx.activity.viewModels
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityLinkTestResultBinding
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultError
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultError.INVALID
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultError.NO_CONNECTION
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultError.UNEXPECTED
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultState
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class LinkTestResultActivity : BaseActivity(R.layout.activity_link_test_result) {

    @Inject
    lateinit var factory: ViewModelFactory<LinkTestResultViewModel>

    private val viewModel: LinkTestResultViewModel by viewModels { factory }

    private lateinit var binding: ActivityLinkTestResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityLinkTestResultBinding.inflate(layoutInflater)
        with(binding) {

            setContentView(root)

            setNavigateUpToolbar(
                primaryToolbar.toolbar,
                R.string.status_option_link_test_result,
                upIndicator = R.drawable.ic_arrow_back_white
            )

            setupOnClickListeners()

            startListeningToViewState()

            linkTestResultEnterCodeView.setEnterCodeEditText(viewModel.ctaToken, TextView.BufferType.EDITABLE)

            linkTestResultEnterCodeView.addEnterCodeEditTextWatcher(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    viewModel.ctaToken = s.toString().trim().ifEmpty { null }
                }
            })
        }

        viewModel.fetchInitialViewState()
    }

    private fun setupOnClickListeners() {
        binding.linkTestResultContinue.setOnSingleClickListener {
            clearErrors()
            viewModel.onContinueButtonClicked()
        }
    }

    private fun startListeningToViewState() {
        viewModel.viewState().observe(this) {
            renderViewState(it)
        }

        viewModel.validationOnsetDateNeeded().observe(this) { testResult ->
            LinkTestResultSymptomsActivity.start(this, testResult)
            setResult(RESULT_OK)
            finish()
        }

        viewModel.validationCompleted().observe(this) {
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun renderViewState(viewState: LinkTestResultState) = with(binding) {
        if (viewState.showValidationProgress) {
            linkTestResultEnterCodeView.handleProgress()
            linkTestResultContinue.isEnabled = false
        }

        viewState.errorState?.let { errorState ->
            handleError(errorState.error)
            if (errorState.updated) {
                when (errorState.error) {
                    INVALID, NO_CONNECTION, UNEXPECTED -> scrollToValidationError()
                }
            }
        }
    }

    private fun handleError(error: LinkTestResultError) {
        when (error) {
            INVALID -> handleValidationError(getString(R.string.valid_auth_code_is_required))
            NO_CONNECTION -> handleValidationError(getString(R.string.link_test_result_error_no_connection))
            UNEXPECTED -> handleValidationError(getString(R.string.link_test_result_error_unknown))
        }
    }

    private fun handleValidationError(errorText: String) = with(binding) {
        inputErrorView.gone()
        linkTestResultEnterCodeView.errorText = errorText
        linkTestResultEnterCodeView.handleError()
        linkTestResultContinue.isEnabled = true
    }

    private fun scrollToValidationError() = with(binding) {
        linkTestResultScrollView.post {
            linkTestResultScrollView.smoothScrollTo(
                0,
                linkTestResultEnterCodeView.top
            )
        }
    }

    private fun clearErrors() = with(binding) {
        linkTestResultEnterCodeView.resetState()
        inputErrorView.gone()
    }
}
