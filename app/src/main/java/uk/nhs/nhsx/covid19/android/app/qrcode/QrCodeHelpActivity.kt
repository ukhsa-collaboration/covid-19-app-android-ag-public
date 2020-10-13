package uk.nhs.nhsx.covid19.android.app.qrcode

import android.os.Bundle
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar

class QrCodeHelpActivity : BaseActivity(R.layout.activity_qr_code_help) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setNavigateUpToolbar(toolbar, R.string.qr_code_help_title, R.drawable.ic_arrow_back_white)
    }
}
