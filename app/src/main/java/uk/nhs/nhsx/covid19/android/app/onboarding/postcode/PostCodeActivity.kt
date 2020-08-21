package uk.nhs.nhsx.covid19.android.app.onboarding.postcode

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.observe
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.android.synthetic.main.activity_post_code.errorIndicatorLeft
import kotlinx.android.synthetic.main.activity_post_code.errorInfoContainer
import kotlinx.android.synthetic.main.activity_post_code.postCodeContinue
import kotlinx.android.synthetic.main.activity_post_code.postCodeEditText
import kotlinx.android.synthetic.main.activity_post_code.scrollView
import kotlinx.android.synthetic.main.activity_post_code.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.util.scrollToView
import uk.nhs.nhsx.covid19.android.app.util.setNavigateUpToolbar
import javax.inject.Inject

class PostCodeActivity : BaseActivity(R.layout.activity_post_code) {

    @Inject
    lateinit var factory: ViewModelFactory<PostCodeViewModel>

    private val viewModel by viewModels<PostCodeViewModel> { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setNavigateUpToolbar(toolbar as MaterialToolbar, R.string.empty)

        postCodeContinue.setOnClickListener {
            val postCodeEntry = postCodeEditText.text.toString()
            viewModel.validate(postCodeEntry)
        }

        viewModel.viewState().observe(this) { postCodeViewState ->
            when (postCodeViewState) {
                PostCodeViewModel.PostCodeViewState.Valid -> handleValidPostCode()
                PostCodeViewModel.PostCodeViewState.Invalid -> handleInvalidPostCode()
            }
        }
    }

    private fun handleInvalidPostCode() {
        postCodeEditText.setBackgroundResource(R.drawable.edit_text_background_error)
        errorInfoContainer.isVisible = true
        errorInfoContainer.announceForAccessibility(
            "${getString(R.string.post_code_invalid_title)}. ${getString(R.string.valid_post_code_is_required)}"
        )
        errorIndicatorLeft.isVisible = true
        scrollView.scrollToView(errorInfoContainer)
    }

    private fun handleValidPostCode() {
        postCodeEditText.setBackgroundResource(R.drawable.edit_text_background)
        errorInfoContainer.isVisible = false
        errorIndicatorLeft.isVisible = false
        navigateToStatusActivity()
    }

    private fun navigateToStatusActivity() {
        StatusActivity.start(this)
        finish()
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(
                getIntent(
                    context
                )
            )

        private fun getIntent(context: Context) =
            Intent(context, PostCodeActivity::class.java)
    }
}
