package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_exposure_notification.closeContactDate
import kotlinx.android.synthetic.main.activity_exposure_notification.primaryActionButton
import kotlinx.android.synthetic.main.activity_exposure_notification.selfIsolationWarning
import kotlinx.android.synthetic.main.activity_exposure_notification.testingInformationContainer
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.util.uiLongFormat
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class ExposureNotificationActivity : BaseActivity(R.layout.activity_exposure_notification) {
    @Inject
    lateinit var factory: ViewModelFactory<ExposureNotificationViewModel>

    private val viewModel: ExposureNotificationViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        viewModel.updateViewState()

        primaryActionButton.setOnSingleClickListener {
            startActivity<ExposureNotificationAgeLimitActivity>()
        }

        viewModel.viewState.observe(this) { viewState ->
            renderViewState(viewState)
        }

        viewModel.finishActivity.observe(this) {
            finish()
        }
    }

    private fun renderViewState(viewState: ViewState) {
        with(viewState) {
            val formattedDate = encounterDate.uiLongFormat(this@ExposureNotificationActivity)
            closeContactDate.text = getString(R.string.contact_case_exposure_info_screen_exposure_date, formattedDate)
            selfIsolationWarning.isVisible = shouldShowTestingAndIsolationAdvice
            testingInformationContainer.isVisible = shouldShowTestingAndIsolationAdvice
        }
    }

    companion object {
        fun start(context: Context) = context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, ExposureNotificationActivity::class.java)
                .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
    }
}
