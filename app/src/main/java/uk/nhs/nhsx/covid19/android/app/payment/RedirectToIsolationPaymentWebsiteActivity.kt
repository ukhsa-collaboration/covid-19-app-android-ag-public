package uk.nhs.nhsx.covid19.android.app.payment

import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import androidx.activity.viewModels
import androidx.lifecycle.observe
import kotlinx.android.synthetic.main.activity_progress.buttonTryAgain
import kotlinx.android.synthetic.main.activity_progress.errorStateContainer
import kotlinx.android.synthetic.main.activity_progress.loadingProgress
import kotlinx.android.synthetic.main.activity_progress.loadingText
import kotlinx.android.synthetic.main.activity_progress.subtitle
import kotlinx.android.synthetic.main.activity_progress.textErrorTitle
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.payment.RedirectToIsolationPaymentWebsiteViewModel.ViewState.Error
import uk.nhs.nhsx.covid19.android.app.payment.RedirectToIsolationPaymentWebsiteViewModel.ViewState.Loading
import uk.nhs.nhsx.covid19.android.app.payment.RedirectToIsolationPaymentWebsiteViewModel.ViewState.Success
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.openUrl
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setCloseToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import javax.inject.Inject

class RedirectToIsolationPaymentWebsiteActivity : BaseActivity(R.layout.activity_progress) {

    @Inject
    lateinit var factory: ViewModelFactory<RedirectToIsolationPaymentWebsiteViewModel>

    private val viewModel: RedirectToIsolationPaymentWebsiteViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setCloseToolbar(toolbar, R.string.empty, R.drawable.ic_close_primary)

        setupListeners()
        setupViewModelListeners()

        viewModel.loadIsolationPaymentUrl()
    }

    private fun setupListeners() {
        buttonTryAgain.setOnSingleClickListener {
            viewModel.loadIsolationPaymentUrl()
        }
    }

    private fun setupViewModelListeners() {
        viewModel.fetchWebsiteUrl().observe(this) { viewState ->
            when (viewState) {
                is Loading -> showLoadingSpinner()
                is Success -> {
                    openUrl(viewState.url, useInternalBrowser = false)
                    setResult(RESULT_OK)
                    finish()
                }
                is Error -> showErrorState()
            }
        }
    }

    private fun showLoadingSpinner() {
        errorStateContainer.gone()
        loadingProgress.visible()

        val announcementText = loadingText.text
        loadingProgress.announceForAccessibility(announcementText)
        loadingProgress.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
    }

    private fun showErrorState() {
        errorStateContainer.visible()
        loadingProgress.gone()

        val announcementText = "${textErrorTitle.text}. ${subtitle.text}"
        errorStateContainer.announceForAccessibility(announcementText)
        errorStateContainer.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
    }
}
