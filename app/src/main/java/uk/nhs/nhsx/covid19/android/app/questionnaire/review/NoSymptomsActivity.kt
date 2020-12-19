package uk.nhs.nhsx.covid19.android.app.questionnaire.review

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_no_symptoms.buttonReturnToHomeScreen
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener

class NoSymptomsActivity : BaseActivity(R.layout.activity_no_symptoms) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buttonReturnToHomeScreen.setOnSingleClickListener {
            finish()
            StatusActivity.start(this)
        }
    }
}
