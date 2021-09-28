package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import androidx.activity.viewModels
import kotlinx.android.synthetic.main.activity_share_keys_reminder.doNotShareResultsButton
import kotlinx.android.synthetic.main.activity_share_keys_reminder.shareResultsButton
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class ShareKeysReminderActivity : ShareKeysBaseActivity(R.layout.activity_share_keys_reminder) {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory<ShareKeysReminderViewModel>
    override val viewModel: ShareKeysReminderViewModel by viewModels { viewModelFactory }

    override fun inject() = appComponent.inject(this)

    override fun setupOnClickListeners() {
        shareResultsButton.setOnSingleClickListener {
            viewModel.onShareKeysButtonClicked()
        }

        doNotShareResultsButton.setOnSingleClickListener {
            viewModel.onDoNotShareKeysClicked()
        }
    }
}
