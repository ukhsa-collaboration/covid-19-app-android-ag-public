package uk.nhs.nhsx.covid19.android.app.qrcode

import android.os.Bundle
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityQrCodeHelpBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar

class QrCodeHelpActivity : BaseActivity() {

    private lateinit var binding: ActivityQrCodeHelpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrCodeHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setNavigateUpToolbar(
            binding.primaryToolbar.toolbar,
            R.string.qr_code_help_more_information,
            upIndicator = R.drawable.ic_arrow_back_white
        )
    }
}
