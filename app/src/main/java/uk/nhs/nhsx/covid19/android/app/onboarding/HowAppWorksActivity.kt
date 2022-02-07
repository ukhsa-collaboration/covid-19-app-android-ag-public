package uk.nhs.nhsx.covid19.android.app.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityHowAppWorksBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener

class HowAppWorksActivity : BaseActivity() {

    private lateinit var binding: ActivityHowAppWorksBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityHowAppWorksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            continueHowAppWorks.setOnSingleClickListener {
                startActivityForResult(Intent(this@HowAppWorksActivity, DataAndPrivacyActivity::class.java), FINISH_ACTIVITY_REQUEST)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                FINISH_ACTIVITY_REQUEST -> finish()
            }
        }
    }

    override fun onBackPressed() {
    }

    companion object {

        private const val FINISH_ACTIVITY_REQUEST = 1000

        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, HowAppWorksActivity::class.java)
    }
}
