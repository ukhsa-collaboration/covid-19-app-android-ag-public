package uk.nhs.nhsx.covid19.android.app.common.postcode

import android.content.Intent
import android.os.Bundle
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityLocalAuthorityInformationBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener

class LocalAuthorityInformationActivity : BaseActivity() {

    private lateinit var binding: ActivityLocalAuthorityInformationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocalAuthorityInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonContinue.setOnSingleClickListener {
            val intent = LocalAuthorityActivity.getIntent(this, backAllowed = false)
            startActivityForResult(intent, LOCAL_AUTHORITY_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == LOCAL_AUTHORITY_REQUEST && resultCode == RESULT_OK) {
            MainActivity.start(this)
            finish()
        }
    }

    override fun onBackPressed() {
    }

    companion object {
        private const val LOCAL_AUTHORITY_REQUEST = 1337
    }
}
