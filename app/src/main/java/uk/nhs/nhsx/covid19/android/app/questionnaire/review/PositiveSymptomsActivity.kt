package uk.nhs.nhsx.covid19.android.app.questionnaire.review

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import kotlinx.android.synthetic.main.activity_positive_symptoms.daysUntilExpirationTextView
import kotlinx.android.synthetic.main.activity_positive_symptoms.onlineServiceLinkTextView
import kotlinx.android.synthetic.main.activity_positive_symptoms.postDaysTextView
import kotlinx.android.synthetic.main.activity_positive_symptoms.preDaysTextView
import kotlinx.android.synthetic.main.activity_positive_symptoms.stateActionButton
import kotlinx.android.synthetic.main.activity_positive_symptoms.stateExplanation
import kotlinx.android.synthetic.main.activity_positive_symptoms.stateInfoView
import kotlinx.android.synthetic.main.activity_positive_symptoms.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingActivity
import uk.nhs.nhsx.covid19.android.app.util.URL_NHS_111_ONLINE
import uk.nhs.nhsx.covid19.android.app.util.openUrl
import uk.nhs.nhsx.covid19.android.app.util.setNavigateUpToolbar
import javax.inject.Inject

class PositiveSymptomsActivity : AppCompatActivity(R.layout.activity_positive_symptoms) {

    @Inject
    lateinit var factory: ViewModelFactory<PositiveSymptomsViewModel>

    private val viewModel: PositiveSymptomsViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setNavigateUpToolbar(toolbar, R.string.empty, R.drawable.ic_close_primary)
        toolbar.setNavigationOnClickListener {
            navigateToStatusActivity()
        }

        onlineServiceLinkTextView.setOnClickListener {
            openUrl(URL_NHS_111_ONLINE)
        }

        setupUi()
        registerViewModelListeners()
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

    private fun registerViewModelListeners() {
        viewModel.daysUntilExpiration().observe(
            this,
            Observer { daysUntilExpiration ->
                daysUntilExpirationTextView.text = resources.getQuantityString(
                    R.plurals.state_isolation_days,
                    daysUntilExpiration.toInt(),
                    daysUntilExpiration
                )
            }
        )
    }

    private fun setupUi() {
        viewModel.calculateDaysUntilExpiration()

        preDaysTextView.text = getString(R.string.self_isolate_for)
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
        stateActionButton.isVisible = RuntimeBehavior.isFeatureEnabled(FeatureFlag.TEST_ORDERING)
    }

    companion object {
        const val REQUEST_CODE_ORDER_A_TEST = 1337

        fun start(context: Context) =
            context.startActivity(
                getIntent(
                    context
                )
            )

        fun getIntent(context: Context): Intent {
            return Intent(context, PositiveSymptomsActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
        }
    }
}
