package uk.nhs.nhsx.covid19.android.app.edgecases

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_edge_case.edgeCaseText
import kotlinx.android.synthetic.main.activity_edge_case.edgeCaseTitle
import kotlinx.android.synthetic.main.activity_edge_case.takeActionButton
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.util.gone

class AgeRestrictionActivity : BaseActivity(R.layout.activity_edge_case) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        edgeCaseTitle.setText(R.string.onboarding_age_restriction_title)
        edgeCaseText.setText(R.string.onboarding_age_restriction_text)
        takeActionButton.gone()
    }

    companion object {
        fun start(context: Context) = context.startActivity(getIntent(context))

        fun getIntent(context: Context) = Intent(context, AgeRestrictionActivity::class.java)
    }
}
