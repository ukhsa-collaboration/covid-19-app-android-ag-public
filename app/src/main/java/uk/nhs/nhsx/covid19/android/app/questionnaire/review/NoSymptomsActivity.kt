package uk.nhs.nhsx.covid19.android.app.questionnaire.review

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_no_symptoms.buttonReturnToHomeScreen
import kotlinx.android.synthetic.main.activity_no_symptoms.noSymptomsOnlineServiceLink
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.util.URL_NHS_111_ONLINE
import uk.nhs.nhsx.covid19.android.app.util.openUrl

class NoSymptomsActivity : AppCompatActivity(R.layout.activity_no_symptoms) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buttonReturnToHomeScreen.setOnClickListener {
            finish()
            StatusActivity.start(this)
        }

        noSymptomsOnlineServiceLink.setOnClickListener {
            openUrl(URL_NHS_111_ONLINE)
        }
    }
}
