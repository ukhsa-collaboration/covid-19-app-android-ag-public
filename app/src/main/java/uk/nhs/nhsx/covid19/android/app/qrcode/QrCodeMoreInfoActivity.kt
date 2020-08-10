package uk.nhs.nhsx.covid19.android.app.qrcode

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_qr_code_more_info.buttonReturnToCheckIn
import uk.nhs.nhsx.covid19.android.app.R

class QrCodeMoreInfoActivity : AppCompatActivity(R.layout.activity_qr_code_more_info) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buttonReturnToCheckIn.setOnClickListener {
            finish()
        }
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, QrCodeMoreInfoActivity::class.java)
    }
}
