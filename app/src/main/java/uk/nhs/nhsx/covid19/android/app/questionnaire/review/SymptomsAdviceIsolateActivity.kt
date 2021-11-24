package uk.nhs.nhsx.covid19.android.app.questionnaire.review

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.databinding.ActivitySymptomsAdviceIsolateBinding
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.IndexCaseThenHasSymptomsDidUpdateIsolation
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.IndexCaseThenHasSymptomsNoEffectOnIsolation
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.IndexCaseThenNoSymptoms
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.NoIndexCaseThenIsolationDueToSelfAssessment
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.NoIndexCaseThenSelfAssessmentNoImpactOnIsolation
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setCloseToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpAccessibilityHeading
import uk.nhs.nhsx.covid19.android.app.widgets.StateInfoParams

class SymptomsAdviceIsolateActivity : BaseActivity() {

    private lateinit var binding: ActivitySymptomsAdviceIsolateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivitySymptomsAdviceIsolateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isolationSymptomAdvice = intent.getParcelableExtra<IsolationSymptomAdvice>(EXTRA_ISOLATION_SYMPTOM_ADVICE)

        if (isolationSymptomAdvice == null) {
            finish()
            return
        }

        handleIsolationSymptomAdvice(isolationSymptomAdvice)

        binding.daysToIsolateContainer.setUpAccessibilityHeading()
    }

    private fun handleIsolationSymptomAdvice(isolationSymptomAdvice: IsolationSymptomAdvice) {
        when (isolationSymptomAdvice) {
            is IndexCaseThenHasSymptomsDidUpdateIsolation ->
                handleIndexCaseThenHasSymptomsDidUpdateIsolation(isolationSymptomAdvice.remainingDaysInIsolation)
            IndexCaseThenHasSymptomsNoEffectOnIsolation ->
                handleIndexCaseThenHasSymptomsNoEffectOnIsolation()
            IndexCaseThenNoSymptoms ->
                handleIndexCaseThenNoSymptoms()
            is NoIndexCaseThenIsolationDueToSelfAssessment ->
                handleNoIndexCaseThenIsolationDueToSelfAssessment(isolationSymptomAdvice.remainingDaysInIsolation)
            is NoIndexCaseThenSelfAssessmentNoImpactOnIsolation ->
                handleNoIndexCaseThenSelfAssessmentNoImpactOnIsolation(isolationSymptomAdvice.remainingDaysInIsolation)
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

    private fun handleIndexCaseThenHasSymptomsDidUpdateIsolation(remainingDaysInIsolation: Int) =
        setupUi(
            stateIconResource = R.drawable.ic_isolation_book_test,
            isolationDescription = IsolationDescription(
                preBigText = R.string.self_isolate_for,
                bigText = resources.getQuantityString(
                    R.plurals.state_isolation_days,
                    remainingDaysInIsolation,
                    remainingDaysInIsolation
                ),
            ),
            showExposureFaqsLinkTextView = false,
            stateInfoParams = StateInfoParams(R.string.symptoms_advice_isolate_info_continue_isolation, R.color.amber),
            buttonText = R.string.continue_button,
            buttonAction = { navigateToStatusActivity() },
            explanationParagraphs = intArrayOf(R.string.symptoms_advice_isolate_paragraphs_continue_isolation),
        )

    private fun handleIndexCaseThenHasSymptomsNoEffectOnIsolation() =
        setupUi(
            stateIconResource = R.drawable.ic_isolation_continue,
            isolationDescription = IsolationDescription(
                preBigText = R.string.symptoms_advice_isolate_heading_continue_isolation_no_change,
            ),
            showExposureFaqsLinkTextView = false,
            stateInfoParams = StateInfoParams(R.string.symptoms_advice_isolate_info_continue_isolation_no_change, R.color.error_red),
            buttonText = R.string.continue_button,
            buttonAction = { navigateToStatusActivity() },
            explanationParagraphs = intArrayOf(R.string.symptoms_advice_isolate_paragraphs_continue_isolation_no_change),
        )

    private fun handleIndexCaseThenNoSymptoms() =
        setupUi(
            stateIconResource = R.drawable.ic_isolation_continue,
            isolationDescription = IsolationDescription(
                preBigText = R.string.symptoms_advice_isolate_heading_continue_isolation_no_symptoms,
            ),
            showExposureFaqsLinkTextView = false,
            stateInfoParams = StateInfoParams(R.string.symptoms_advice_isolate_info_continue_isolation_no_symptoms, R.color.error_red),
            buttonText = R.string.continue_button,
            buttonAction = { navigateToStatusActivity() },
            explanationParagraphs = intArrayOf(R.string.symptoms_advice_isolate_paragraphs_continue_isolation_no_symptoms),
        )

    private fun handleNoIndexCaseThenIsolationDueToSelfAssessment(remainingDaysInIsolation: Int) =
        setupUi(
            showCloseButtonInToolbar = true,
            stateIconResource = R.drawable.ic_isolation_book_test,
            isolationDescription = IsolationDescription(
                preBigText = R.string.self_isolate_for,
                bigText = resources.getQuantityString(
                    R.plurals.state_isolation_days,
                    remainingDaysInIsolation,
                    remainingDaysInIsolation
                ),
                postBigText = R.string.state_and_book_a_test
            ),
            showExposureFaqsLinkTextView = true,
            stateInfoParams = StateInfoParams(R.string.state_index_info, R.color.amber),
            buttonText = R.string.book_free_test,
            buttonAction = {
                startActivityForResult(
                    TestOrderingActivity.getIntent(this),
                    REQUEST_CODE_ORDER_A_TEST
                )
            },
            explanationParagraphs = intArrayOf(
                R.string.isolate_after_corona_virus_symptoms,
                R.string.exposure_faqs_title
            ),
        )

    private fun handleNoIndexCaseThenSelfAssessmentNoImpactOnIsolation(remainingDaysInIsolation: Int) =
        setupUi(
            showCloseButtonInToolbar = true,
            stateIconResource = R.drawable.ic_isolation_contact,
            isolationDescription = IsolationDescription(
                preBigText = R.string.continue_to_self_isolate_for,
                bigText = resources.getQuantityString(
                    R.plurals.state_isolation_days,
                    remainingDaysInIsolation,
                    remainingDaysInIsolation
                )
            ),
            showExposureFaqsLinkTextView = false,
            stateInfoParams = StateInfoParams(R.string.you_do_not_appear_to_have_symptoms, R.color.nhs_button_green),
            buttonText = R.string.back_to_home,
            buttonAction = { navigateToStatusActivity() },
            explanationParagraphs = intArrayOf(R.string.isolate_after_no_corona_virus_symptoms),
        )

    private fun setupUi(
        @DrawableRes stateIconResource: Int,
        isolationDescription: IsolationDescription,
        showExposureFaqsLinkTextView: Boolean,
        stateInfoParams: StateInfoParams,
        @StringRes vararg explanationParagraphs: Int,
        @StringRes buttonText: Int,
        showCloseButtonInToolbar: Boolean = false,
        buttonAction: () -> Unit
    ) = with(binding) {
        if (showCloseButtonInToolbar) {
            setCloseToolbar(primaryToolbar.toolbar, R.string.empty, R.drawable.ic_close_primary)

            primaryToolbar.toolbar.setNavigationOnClickListener {
                navigateToStatusActivity()
            }
        }
        stateIcon.setImageResource(stateIconResource)

        setupIsolationDescriptionView(isolationDescription)

        exposureFaqsLinkTextView.isVisible = showExposureFaqsLinkTextView

        stateInfoView.setStateInfoParams(stateInfoParams)

        stateExplanation.addAllParagraphs(explanationParagraphs.map { getString(it) })

        stateActionButton.text = getString(buttonText)
        stateActionButton.setOnSingleClickListener(buttonAction)
    }

    private fun setupIsolationDescriptionView(isolationDescription: IsolationDescription) = with(binding) {
        showTextIfPresent(preDaysTextView, isolationDescription.preBigText)
        showTextIfPresent(daysUntilExpirationTextView, isolationDescription.bigText)
        showTextIfPresent(postDaysTextView, isolationDescription.postBigText)

        setAccessibilityTitle(isolationDescription.accessibilityTitle(this@SymptomsAdviceIsolateActivity))
    }

    private fun showTextIfPresent(view: TextView, stringResId: Int?) {
        val string = if (stringResId != null) getString(stringResId) else null
        showTextIfPresent(view, string)
    }

    private fun showTextIfPresent(view: TextView, string: String?) {
        view.text = string
        view.isVisible = string != null
    }

    private data class IsolationDescription(
        @StringRes val preBigText: Int? = null,
        val bigText: String? = null,
        @StringRes val postBigText: Int? = null,
    ) {
        fun accessibilityTitle(context: Context): String {
            val preBigText = if (preBigText != null) context.getString(preBigText) else null
            val postBigText = if (postBigText != null) context.getString(postBigText) else null

            return listOfNotNull(preBigText, bigText, postBigText).joinToString(separator = " ")
        }
    }

    companion object {
        const val REQUEST_CODE_ORDER_A_TEST = 1337
        const val EXTRA_ISOLATION_SYMPTOM_ADVICE = "EXTRA_ISOLATION_SYMPTOM_ADVICE"

        fun start(context: Context, isolationSymptomAdvice: IsolationSymptomAdvice) =
            context.startActivity(
                getIntent(context).putExtra(EXTRA_ISOLATION_SYMPTOM_ADVICE, isolationSymptomAdvice)
            )

        fun getIntent(context: Context): Intent {
            return Intent(context, SymptomsAdviceIsolateActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
        }
    }
}
