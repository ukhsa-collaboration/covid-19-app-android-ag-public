package uk.nhs.nhsx.covid19.android.app.onboarding.authentication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_authentication_code.authCodeContinue
import kotlinx.android.synthetic.main.activity_authentication_code.authCodeEditText
import kotlinx.android.synthetic.main.activity_authentication_code.errorIndicatorLeft
import kotlinx.android.synthetic.main.activity_authentication_code.errorText
import kotlinx.android.synthetic.main.activity_authentication_code.progressBar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.drawable
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.onboarding.MainOnboardingActivity
import uk.nhs.nhsx.covid19.android.app.onboarding.authentication.AuthenticationCodeViewModel.AuthCodeViewState.Invalid
import uk.nhs.nhsx.covid19.android.app.onboarding.authentication.AuthenticationCodeViewModel.AuthCodeViewState.Progress
import uk.nhs.nhsx.covid19.android.app.onboarding.authentication.AuthenticationCodeViewModel.AuthCodeViewState.Valid
import uk.nhs.nhsx.covid19.android.app.onboarding.authentication.AuthenticationCodeViewModel.Companion.AUTH_CODE_REGEX_FORMAT
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.util.announce
import uk.nhs.nhsx.covid19.android.app.util.gone
import uk.nhs.nhsx.covid19.android.app.util.invisible
import uk.nhs.nhsx.covid19.android.app.util.visible
import javax.inject.Inject

class AuthenticationCodeActivity : AppCompatActivity(R.layout.activity_authentication_code) {
    @Inject
    lateinit var factory: ViewModelFactory<AuthenticationCodeViewModel>

    private val viewModel: AuthenticationCodeViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        authCodeEditText.addTextChangedListener(
            FormattingTextWatcher(
                authCodeEditText
            )
        )

        authCodeContinue.setOnClickListener {
            val authCodeEntry = authCodeEditText.text.toString()
            viewModel.validate(authCodeEntry)
        }

        viewModel.viewState().observe(
            this,
            Observer { authCodeViewState ->
                when (authCodeViewState) {
                    Progress -> handleProgress()
                    Valid -> handleValidAuthCode()
                    Invalid -> handleInvalidAuthCode()
                }
            }
        )
    }

    private fun handleInvalidAuthCode() {
        showErrorState()
        authCodeContinue.isEnabled = true
    }

    private fun showErrorState() {
        errorIndicatorLeft.visible()
        errorText.visible()
        authCodeEditText.setBackgroundResource(drawable.edit_text_background_error)
        progressBar.gone()
        announce(errorText.text.toString())
    }

    private fun handleProgress() {
        showNormalState()
        authCodeContinue.isEnabled = false
    }

    private fun handleValidAuthCode() {
        showNormalState()
        authCodeContinue.isEnabled = true
        finish()
        startActivity<MainOnboardingActivity>()
    }

    private fun showNormalState() {
        errorIndicatorLeft.invisible()
        errorText.gone()
        authCodeEditText.setBackgroundResource(drawable.edit_text_background)
        progressBar.visible()
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(
                getIntent(
                    context
                )
            )

        private fun getIntent(context: Context) =
            Intent(context, AuthenticationCodeActivity::class.java)
    }

    class FormattingTextWatcher(private val editText: EditText) : TextWatcher {
        override fun afterTextChanged(s: Editable) {
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            editText.removeTextChangedListener(this)
            val cleanString = s.toString().replace(AUTH_CODE_REGEX_FORMAT.toRegex(), "")
            val formattedString = if (cleanString.length > 4) {
                cleanString.substring(0..3) + "-" + cleanString.substring(4)
            } else {
                cleanString
            }
            editText.setText(formattedString)

            editText.setSelection(formattedString.length)

            editText.addTextChangedListener(this)
        }
    }
}
