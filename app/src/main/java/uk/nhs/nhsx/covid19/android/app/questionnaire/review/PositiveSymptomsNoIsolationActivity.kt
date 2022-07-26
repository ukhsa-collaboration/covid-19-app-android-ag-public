package uk.nhs.nhsx.covid19.android.app.questionnaire.review

import android.os.Bundle
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityPositiveSymptomsNoIsolationBinding
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.openUrl
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setCloseToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener

class PositiveSymptomsNoIsolationActivity : BaseActivity() {
    private lateinit var binding: ActivityPositiveSymptomsNoIsolationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPositiveSymptomsNoIsolationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            setCloseToolbar(primaryToolbar.toolbar, R.string.empty, R.drawable.ic_close_primary)

            primaryToolbar.toolbar.setNavigationOnClickListener {
                navigateToStatusActivity()
            }

            positiveSymptomsNoIsolationNhsLink.setOnSingleClickListener {
                openUrl(
                    getString(R.string.nhs_111_online_link_wls)
                )
            }

            positiveSymptomsNoIsolationFinishButton.setOnSingleClickListener {
                navigateToStatusActivity()
            }
        }
    }

    override fun onBackPressed() {
        navigateToStatusActivity()
    }

    private fun navigateToStatusActivity() {
        StatusActivity.start(this)
        finish()
    }
}
