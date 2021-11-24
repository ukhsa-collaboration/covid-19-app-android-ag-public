package uk.nhs.nhsx.covid19.android.app.status.localmessage

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityLocalMessageBinding
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.ContentBlock
import uk.nhs.nhsx.covid19.android.app.util.viewutils.openUrl
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setCloseToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class LocalMessageActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<LocalMessageViewModel>
    private val viewModel: LocalMessageViewModel by viewModels { factory }

    private lateinit var binding: ActivityLocalMessageBinding
    private lateinit var localMessageViewAdapter: LocalMessageViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityLocalMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setCloseToolbar(
            binding.primaryToolbar.toolbar,
            closeIndicator = R.drawable.ic_close_primary,
            titleResId = R.string.empty
        )

        registerViewModelListeners()
        registerButtonListeners()

        removeNotificationIfPresent()

        viewModel.onCreate()
    }

    private fun registerViewModelListeners() {
        viewModel.viewState().observe(this) { localMessage ->
            if (localMessage != null) {
                with(localMessage) {
                    binding.titleLocalMessage.text = head
                    setAccessibilityTitle(head)
                    setUpLocalMessageAdapter(content)
                }
            } else {
                finish()
            }
        }
    }

    private fun removeNotificationIfPresent() {
        NotificationManagerCompat.from(this).cancel(NotificationProvider.LOCAL_MESSAGE_NOTIFICATION_ID)
    }

    private fun registerButtonListeners() {
        binding.backToHome.setOnSingleClickListener {
            finish()
        }
    }

    private fun setUpLocalMessageAdapter(contentBlocks: List<ContentBlock>) = with(binding) {
        localMessageViewAdapter = LocalMessageViewAdapter(contentBlocks.filter { it.isDisplayable() }, ::openUrl)
        localMessageContentList.layoutManager = LinearLayoutManager(this@LocalMessageActivity)
        localMessageContentList.adapter = localMessageViewAdapter
    }
}
