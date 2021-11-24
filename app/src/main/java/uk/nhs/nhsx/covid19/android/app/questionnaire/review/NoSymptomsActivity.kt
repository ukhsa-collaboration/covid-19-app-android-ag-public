package uk.nhs.nhsx.covid19.android.app.questionnaire.review

import android.os.Bundle
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityNoSymptomsBinding
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener

class NoSymptomsActivity : BaseActivity() {

    private lateinit var binding: ActivityNoSymptomsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoSymptomsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonReturnToHomeScreen.setOnSingleClickListener {
            finish()
            StatusActivity.start(this)
        }
    }
}
