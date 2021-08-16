package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_exposure_notification.primaryActionButton
import uk.nhs.nhsx.covid19.android.app.R.layout
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener

class ExposureNotificationActivity : BaseActivity(layout.activity_exposure_notification) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        primaryActionButton.setOnSingleClickListener {
            startActivity<ExposureNotificationAgeLimitActivity>()
        }
    }

    companion object {
        fun start(context: Context) = context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, ExposureNotificationActivity::class.java)
                .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
    }
}
