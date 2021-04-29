package uk.nhs.nhsx.covid19.android.app.exposure

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_share_keys_result.actionButton
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener

class ShareKeysResultActivity : BaseActivity(R.layout.activity_share_keys_result) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        actionButton.setOnSingleClickListener {
            StatusActivity.start(this)
            finish()
        }
    }

    override fun onBackPressed() = Unit

    companion object {
        fun start(context: Context) =
            context.startActivity(Intent(context, ShareKeysResultActivity::class.java))
    }
}
