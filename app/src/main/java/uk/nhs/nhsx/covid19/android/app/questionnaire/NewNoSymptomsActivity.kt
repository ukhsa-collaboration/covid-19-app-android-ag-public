package uk.nhs.nhsx.covid19.android.app.questionnaire

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_new_no_symptoms.backToHomeButton
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener

class NewNoSymptomsActivity : BaseActivity(R.layout.activity_new_no_symptoms) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        backToHomeButton.setOnSingleClickListener {
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
