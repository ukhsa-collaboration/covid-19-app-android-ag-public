package uk.nhs.nhsx.covid19.android.app.status.localmessage

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_local_message.backToHome
import kotlinx.android.synthetic.main.activity_local_message.localMessageContentList
import kotlinx.android.synthetic.main.activity_local_message.titleLocalMessage
import kotlinx.android.synthetic.main.view_toolbar_background.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.ContentBlock
import uk.nhs.nhsx.covid19.android.app.util.viewutils.openUrl
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setCloseToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class LocalMessageActivity : BaseActivity(R.layout.activity_local_message) {

    @Inject
    lateinit var factory: ViewModelFactory<LocalMessageViewModel>
    private val viewModel: LocalMessageViewModel by viewModels { factory }

    private lateinit var localMessageViewAdapter: LocalMessageViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setCloseToolbar(toolbar, closeIndicator = R.drawable.ic_close_primary, titleResId = R.string.empty)

        registerViewModelListeners()
        registerButtonListeners()

        removeNotificationIfPresent()

        viewModel.onCreate()
    }

    private fun registerViewModelListeners() {
        viewModel.viewState().observe(this) { localMessage ->
            if (localMessage != null) {
                with(localMessage) {
                    titleLocalMessage.text = head
                    head?.let { setAccessibilityTitle(it) }
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
        backToHome.setOnSingleClickListener {
            finish()
        }
    }

    private fun setUpLocalMessageAdapter(contentBlocks: List<ContentBlock>?) {
        localMessageViewAdapter = LocalMessageViewAdapter(contentBlocks?.filter { it.isDisplayable() } ?: listOf(), ::openUrl)
        localMessageContentList.layoutManager = LinearLayoutManager(this)
        localMessageContentList.adapter = localMessageViewAdapter
    }
}
