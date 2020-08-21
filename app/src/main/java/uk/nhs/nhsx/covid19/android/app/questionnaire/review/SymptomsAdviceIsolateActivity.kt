package uk.nhs.nhsx.covid19.android.app.questionnaire.review

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import kotlinx.android.synthetic.main.activity_symptoms_advice_isolate.daysUntilExpirationTextView
import kotlinx.android.synthetic.main.activity_symptoms_advice_isolate.onlineServiceLinkTextView
import kotlinx.android.synthetic.main.activity_symptoms_advice_isolate.postDaysTextView
import kotlinx.android.synthetic.main.activity_symptoms_advice_isolate.preDaysTextView
import kotlinx.android.synthetic.main.activity_symptoms_advice_isolate.stateActionButton
import kotlinx.android.synthetic.main.activity_symptoms_advice_isolate.stateExplanation
import kotlinx.android.synthetic.main.activity_symptoms_advice_isolate.stateInfoView
import kotlinx.android.synthetic.main.activity_symptoms_advice_isolate.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingActivity
import uk.nhs.nhsx.covid19.android.app.util.gone
import uk.nhs.nhsx.covid19.android.app.util.openUrl
import uk.nhs.nhsx.covid19.android.app.util.setNavigateUpToolbar

class SymptomsAdviceIsolateActivity :
    BaseActivity(R.layout.activity_symptoms_advice_isolate) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setNavigateUpToolbar(toolbar, R.string.empty, R.drawable.ic_close_primary)
        toolbar.setNavigationOnClickListener {
            navigateToStatusActivity()
        }

        onlineServiceLinkTextView.setOnClickListener {
            openUrl(R.string.url_nhs_111_online)
        }

        val isPositiveSymptoms = intent.getBooleanExtra(EXTRA_IS_POSITIVE_SYMPTOMS, false)
        val isolationDuration = intent.getIntExtra(EXTRA_ISOLATION_DURATION, 0)

        if (isPositiveSymptoms) {
            setupPositiveSymptomsUi(isolationDuration)
        } else {
            setupNegativeSymptomsUi(isolationDuration)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ORDER_A_TEST && resultCode == Activity.RESULT_OK) {
            navigateToStatusActivity()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        navigateToStatusActivity()
    }

    private fun navigateToStatusActivity() {
        StatusActivity.start(this)
        finish()
    }

    private fun setupPositiveSymptomsUi(daysUntilExpiration: Int) {
        preDaysTextView.text = getString(R.string.self_isolate_for)
        daysUntilExpirationTextView.text = resources.getQuantityString(
            R.plurals.state_isolation_days,
            daysUntilExpiration,
            daysUntilExpiration
        )
        postDaysTextView.text = getString(R.string.state_and_book_a_test)

        stateInfoView.stateText = getString(R.string.state_index_info)
        stateInfoView.stateColor = getColor(R.color.amber)

        stateExplanation.addAllParagraphs(
            getString(R.string.isolate_after_corona_virus_symptoms),
            getString(R.string.for_further_advice_visit)
        )

        stateActionButton.text = getString(R.string.book_free_test)
        stateActionButton.setOnClickListener {
            startActivityForResult(
                TestOrderingActivity.getIntent(this),
                REQUEST_CODE_ORDER_A_TEST
            )
        }
        stateActionButton.isVisible =
            RuntimeBehavior.isFeatureEnabled(FeatureFlag.TEST_ORDERING)
    }

    private fun setupNegativeSymptomsUi(daysUntilExpiration: Int) {
        preDaysTextView.text = getString(R.string.self_isolate_for)
        daysUntilExpirationTextView.text = resources.getQuantityString(
            R.plurals.state_isolation_days,
            daysUntilExpiration,
            daysUntilExpiration
        )
        postDaysTextView.gone()

        stateInfoView.stateText = getString(R.string.you_do_not_appear_to_have_symptoms)
        stateInfoView.stateColor = getColor(R.color.nhs_button_green)

        stateExplanation.addAllParagraphs(
            getString(R.string.isolate_after_no_corona_virus_symptoms)
        )

        stateActionButton.text = getString(R.string.back_to_home)
        stateActionButton.setOnClickListener {
            navigateToStatusActivity()
        }
    }

    companion object {
        const val REQUEST_CODE_ORDER_A_TEST = 1337
        const val EXTRA_IS_POSITIVE_SYMPTOMS = "EXTRA_IS_POSITIVE_SYMPTOMS"
        const val EXTRA_ISOLATION_DURATION = "EXTRA_ISOLATION_DURATION"

        fun start(context: Context, isPositiveSymptoms: Boolean, isolationDuration: Int) =
            context.startActivity(
                getIntent(context)
                    .putExtra(EXTRA_IS_POSITIVE_SYMPTOMS, isPositiveSymptoms)
                    .putExtra(EXTRA_ISOLATION_DURATION, isolationDuration)
            )

        fun getIntent(context: Context): Intent {
            return Intent(context, SymptomsAdviceIsolateActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
        }
    }
}
