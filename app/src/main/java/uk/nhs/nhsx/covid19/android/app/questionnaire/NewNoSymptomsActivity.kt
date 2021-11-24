package uk.nhs.nhsx.covid19.android.app.questionnaire

import android.os.Bundle
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityNewNoSymptomsBinding
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener

class NewNoSymptomsActivity : BaseActivity() {

    private lateinit var binding: ActivityNewNoSymptomsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewNoSymptomsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backToHomeButton.setOnSingleClickListener {
            navigateToStatusActivity()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        navigateToStatusActivity()
    }

    private fun navigateToStatusActivity() {
        StatusActivity.start(this)
    }
}
