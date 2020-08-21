package uk.nhs.nhsx.covid19.android.app.edgecases

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_app_availability.description
import kotlinx.android.synthetic.main.activity_app_availability.subTitle
import kotlinx.android.synthetic.main.activity_app_availability.titleText
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity

class DeviceNotSupportedActivity : BaseActivity(R.layout.activity_app_availability) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        titleText.setText(R.string.cant_run_app)
        subTitle.setText(R.string.could_be_due_to_run_app)
        description.setText(R.string.unsupported_device)
    }

    companion object {
        fun start(context: Context) = context.startActivity(getIntent(context))

        fun getIntent(context: Context) = Intent(context, DeviceNotSupportedActivity::class.java)
    }
}
