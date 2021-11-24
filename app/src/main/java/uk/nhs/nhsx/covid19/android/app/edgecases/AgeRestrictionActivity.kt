package uk.nhs.nhsx.covid19.android.app.edgecases

import android.content.Context
import android.content.Intent
import android.os.Bundle
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityEdgeCaseBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone

class AgeRestrictionActivity : BaseActivity() {

    private lateinit var binding: ActivityEdgeCaseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEdgeCaseBinding.inflate(layoutInflater)

        with(binding) {

        setContentView(binding.root)

        edgeCaseTitle.setText(R.string.onboarding_age_restriction_title)
        edgeCaseText.setText(R.string.onboarding_age_restriction_text)
        takeActionButton.gone()
        }
    }

    companion object {
        fun start(context: Context) = context.startActivity(getIntent(context))

        fun getIntent(context: Context) = Intent(context, AgeRestrictionActivity::class.java)
    }
}
