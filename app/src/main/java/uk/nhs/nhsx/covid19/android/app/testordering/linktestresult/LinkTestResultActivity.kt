package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_link_test_result.dailyContactTestingContainer
import kotlinx.android.synthetic.main.activity_link_test_result.dailyContactTestingNegativeResultConfirmationCheckBox
import kotlinx.android.synthetic.main.activity_link_test_result.dailyContactTestingNegativeResultConfirmationContainer
import kotlinx.android.synthetic.main.activity_link_test_result.inputErrorView
import kotlinx.android.synthetic.main.activity_link_test_result.linkTestResultContinue
import kotlinx.android.synthetic.main.activity_link_test_result.linkTestResultEnterCodeView
import kotlinx.android.synthetic.main.activity_link_test_result.linkTestResultScrollView
import kotlinx.android.synthetic.main.view_enter_code.enterCodeEditText
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultError
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultError.BOTH_PROVIDED
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultError.INVALID
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultError.NEITHER_PROVIDED
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultError.NO_CONNECTION
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultError.UNEXPECTED
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultState
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
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
            upIndicator = R.drawable.ic_arrow_back_white
        )

        setupOnClickListeners()

        startListeningToViewState()

        enterCodeEditText.setText(viewModel.ctaToken, TextView.BufferType.EDITABLE)

        enterCodeEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                viewModel.ctaToken = s.toString().trim().ifEmpty { null }
            }
        })

        viewModel.fetchInitialViewState()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == DAILY_CONTACT_TESTING_REQUEST && resultCode == RESULT_OK) {
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun setupOnClickListeners() {
        linkTestResultContinue.setOnSingleClickListener {
            clearErrors()
            viewModel.onContinueButtonClicked()
        }

        dailyContactTestingNegativeResultConfirmationContainer.setOnSingleClickListener {
            viewModel.onDailyContactTestingOptInChecked()
        }
    }

    private fun startListeningToViewState() {
        viewModel.viewState().observe(this) {
            renderViewState(it)
        }

        viewModel.confirmedDailyContactTestingNegative().observe(this) {
            val intent = Intent(this, DailyContactTestingConfirmationActivity::class.java)
            startActivityForResult(intent, DAILY_CONTACT_TESTING_REQUEST)
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

    private fun renderViewState(viewState: LinkTestResultState) {
        dailyContactTestingContainer.isVisible = viewState.showDailyContactTesting

        dailyContactTestingNegativeResultConfirmationCheckBox.isChecked =
            viewState.confirmedNegativeDailyContactTestingResult

        dailyContactTestingNegativeResultConfirmationContainer.background =
            if (viewState.confirmedNegativeDailyContactTestingResult) {
                getDrawable(R.drawable.question_selected_background)
            } else getDrawable(R.drawable.question_not_selected_background)

        if (viewState.showValidationProgress) {
            linkTestResultEnterCodeView.handleProgress()
            linkTestResultContinue.isEnabled = false
        }

        viewState.errorState?.let { errorState ->
            handleError(errorState.error)
            if (errorState.updated) {
                when (errorState.error) {
                    INVALID, NO_CONNECTION, UNEXPECTED -> scrollToValidationError()
                    NEITHER_PROVIDED, BOTH_PROVIDED -> scrollToInputError()
                }
            }
        }
    }

    private fun handleError(error: LinkTestResultError) {
        when (error) {
            INVALID -> handleValidationError(getString(R.string.valid_auth_code_is_required))
            NO_CONNECTION -> handleValidationError(getString(R.string.link_test_result_error_no_connection))
            UNEXPECTED -> handleValidationError(getString(R.string.link_test_result_error_unknown))
            NEITHER_PROVIDED -> handleInputError(getString(R.string.link_test_result_error_neither_input_provided))
            BOTH_PROVIDED -> handleInputError(getString(R.string.link_test_result_error_both_inputs_provided))
        }
    }

    private fun handleValidationError(errorText: String) {
        inputErrorView.gone()
        linkTestResultEnterCodeView.errorText = errorText
        linkTestResultEnterCodeView.handleError()
        linkTestResultContinue.isEnabled = true
    }

    private fun scrollToValidationError() {
        linkTestResultScrollView.smoothScrollTo(0, linkTestResultEnterCodeView.top)
    }

    private fun handleInputError(inputError: String) {
        linkTestResultEnterCodeView.resetState()
        inputErrorView.announceForAccessibility("${inputErrorView.errorTitle} $inputError")
        inputErrorView.errorDescription = inputError
        inputErrorView.visible()
    }

    private fun scrollToInputError() {
        linkTestResultScrollView.smoothScrollTo(0, 0)
    }

    private fun clearErrors() {
        linkTestResultEnterCodeView.resetState()
        inputErrorView.gone()
    }

    companion object {
        private const val DAILY_CONTACT_TESTING_REQUEST = 1398
    }
}
