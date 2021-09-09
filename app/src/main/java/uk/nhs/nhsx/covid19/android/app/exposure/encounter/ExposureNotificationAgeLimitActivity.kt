package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import androidx.activity.viewModels
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_exposure_notification_age_limit.ageLimitBinaryRadioGroup
import kotlinx.android.synthetic.main.activity_exposure_notification_age_limit.ageLimitErrorView
import kotlinx.android.synthetic.main.activity_exposure_notification_age_limit.ageLimitScrollView
import kotlinx.android.synthetic.main.activity_exposure_notification_age_limit.ageLimitSubtitle
import kotlinx.android.synthetic.main.activity_exposure_notification_age_limit.continueButton
import kotlinx.android.synthetic.main.activity_exposure_notification_age_limit.exposureNotificationAgeLimitDate
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationAgeLimitViewModel.NavigationTarget.Finish
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationAgeLimitViewModel.NavigationTarget.IsolationResult
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationAgeLimitViewModel.NavigationTarget.VaccinationStatus
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.util.uiLongFormat
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.smoothScrollToAndThen
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import java.time.LocalDate
import javax.inject.Inject

class ExposureNotificationAgeLimitActivity : BaseActivity(R.layout.activity_exposure_notification_age_limit) {
    @Inject
    lateinit var factory: ViewModelFactory<ExposureNotificationAgeLimitViewModel>

    private val viewModel: ExposureNotificationAgeLimitViewModel by viewModels { factory }

    private var hasScrolledToError = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setNavigateUpToolbar(
            toolbar,
            R.string.exposure_notification_age_title,
            upIndicator = R.drawable.ic_arrow_back_white
        )

        ageLimitBinaryRadioGroup.setOnValueChangedListener(viewModel::onAgeLimitOptionChanged)

        continueButton.setOnSingleClickListener {
            hasScrolledToError = false
            viewModel.onClickContinue()
        }

        setUpViewModelListeners()
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateViewState()
    }

    private fun showError(isVisible: Boolean) {
        if (isVisible) {
            ageLimitErrorView.visible()
            if (!hasScrolledToError) {
                ageLimitScrollView.smoothScrollToAndThen(0, 0) {
                    ageLimitErrorView.requestFocusFromTouch()
                    ageLimitErrorView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                    hasScrolledToError = true
                }
            }
        } else {
            ageLimitErrorView.gone()
        }
    }

    private fun updateDateLabel(dateLimit: LocalDate) {
        val formattedDate = dateLimit.uiLongFormat(this)

        exposureNotificationAgeLimitDate.text =
            getString(R.string.exposure_notification_age_subtitle_template, formattedDate)

        ageLimitBinaryRadioGroup.setOptionContentDescriptions(
            getString(R.string.exposure_notification_age_yes_content_description_template, formattedDate),
            getString(R.string.exposure_notification_age_no_content_description_template, formattedDate)
        )
    }

    private fun setUpViewModelListeners() {
        viewModel.viewState().observe(this) { viewState ->
            ageLimitSubtitle.isVisible = viewState.showSubtitle
            showError(viewState.hasError)
            updateDateLabel(viewState.date)
        }

        viewModel.navigationTarget().observe(this) { navigationTarget ->
            when (navigationTarget) {
                VaccinationStatus -> startActivity<ExposureNotificationVaccinationStatusActivity>()
                IsolationResult -> RiskyContactIsolationAdviceActivity.startAsMinor(this)
                Finish -> finish()
            }
        }
    }
}
