package uk.nhs.nhsx.covid19.android.app.questionnaire.review

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_no_symptoms.buttonReturnToHomeScreen
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity

class NoSymptomsActivity : BaseActivity(R.layout.activity_no_symptoms) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buttonReturnToHomeScreen.setOnClickListener {
            finish()
            StatusActivity.start(this)
        }
    }
}
