package uk.nhs.nhsx.covid19.android.app.questionnaire.selection

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_questionnaire.buttonTryAgain
import kotlinx.android.synthetic.main.activity_questionnaire.errorStateContainer
import kotlinx.android.synthetic.main.activity_questionnaire.loadingContainer
import kotlinx.android.synthetic.main.activity_questionnaire.questionListContainer
import kotlinx.android.synthetic.main.activity_questionnaire.textErrorMessage
import kotlinx.android.synthetic.main.activity_questionnaire.textErrorTitle
import kotlinx.android.synthetic.main.include_show_questionnaire.buttonReviewSymptoms
import kotlinx.android.synthetic.main.include_show_questionnaire.errorPanel
import kotlinx.android.synthetic.main.include_show_questionnaire.questionsRecyclerView
import kotlinx.android.synthetic.main.include_show_questionnaire.textNoSymptoms
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.Lce.Error
import uk.nhs.nhsx.covid19.android.app.common.Lce.Loading
import uk.nhs.nhsx.covid19.android.app.common.Lce.Success
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.NoSymptomsActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.ReviewSymptomsActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.ReviewSymptomsActivity.Companion.EXTRA_RISK_THRESHOLD
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.ReviewSymptomsActivity.Companion.EXTRA_SYMPTOMS_ONSET_WINDOW_DAYS
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.Question
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.adapter.QuestionnaireViewAdapter
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.ScrollableLayoutManager
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setCloseToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.smoothScrollToAndThen
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import javax.inject.Inject

class QuestionnaireActivity : BaseActivity(R.layout.activity_questionnaire) {

    private lateinit var questionnaireViewAdapter: QuestionnaireViewAdapter

    @Inject
    lateinit var factory: ViewModelFactory<QuestionnaireViewModel>

    private val viewModel: QuestionnaireViewModel by viewModels { factory }

    private lateinit var nestedScrollView: NestedScrollView

    /**
     * Dialog currently displayed, or null if none are displayed
     */
    private var currentDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setCloseToolbar(toolbar, R.string.select_symptoms)

        appComponent.inject(this)

        nestedScrollView = questionListContainer as NestedScrollView

        setupListeners()
        setupAdapter()
        setupViewModelListeners()
        viewModel.loadQuestionnaire()
    }

    private fun setupViewModelListeners() {
        viewModel.viewState().observe(
            this,
            Observer {
                when (it) {
                    is Success -> handleSuccess(it.data)
                    is Error -> showErrorState()
                    is Loading -> showLoadingSpinner()
                }
            }
        )

        viewModel.navigateToReviewScreen().observe(
            this,
            Observer { viewState ->
                val extraQuestions = ArrayList<Question>().apply {
                    addAll(viewState.questions)
                }

                val intent = Intent(this, ReviewSymptomsActivity::class.java).apply {
                    putParcelableArrayListExtra(
                        ReviewSymptomsActivity.EXTRA_QUESTIONS,
                        extraQuestions
                    )
                    putExtra(EXTRA_RISK_THRESHOLD, viewState.riskThreshold)
                    putExtra(
                        EXTRA_SYMPTOMS_ONSET_WINDOW_DAYS,
                        viewState.symptomsOnsetWindowDays
                    )
                }

                startActivityForResult(intent, CHANGE_QUESTION_REQUEST_CODE)
            }
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == CHANGE_QUESTION_REQUEST_CODE) {
            val question = data?.getParcelableExtra<Question>(QUESTION_TO_CHANGE_KEY)

            val index =
                question?.let { questionnaireViewAdapter.currentList.indexOf(question) } ?: -1

            if (index >= 0) {
                (questionsRecyclerView.layoutManager as ScrollableLayoutManager).scrollToIndex(index)
            }
        }
    }

    private fun handleSuccess(state: QuestionnaireState) {
        showQuestionnaire(state.questions)

        if (state.showError) {
            errorPanel.visible()
            nestedScrollView.smoothScrollToAndThen(0, 0) {
                errorPanel.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
            }
        } else {
            errorPanel.gone()
        }

        if (state.showDialog) {
            showNoSymptomsConfirmationDialog()
        }
    }

    private fun setupListeners() {
        buttonTryAgain.setOnSingleClickListener {
            viewModel.loadQuestionnaire()
        }

        textNoSymptoms.setOnSingleClickListener {
            viewModel.onNoSymptomsClicked()
        }

        buttonReviewSymptoms.setOnSingleClickListener {
            viewModel.onButtonReviewSymptomsClicked()
        }
    }

    private fun showLoadingSpinner() {
        loadingContainer.visible()
        errorStateContainer.gone()
        questionListContainer.gone()

        setAccessibilityTitle(R.string.loading)
    }

    private fun showErrorState() {
        errorStateContainer.visible()
        loadingContainer.gone()
        questionListContainer.gone()

        setAccessibilityTitle("${textErrorTitle.text}. ${textErrorMessage.text}")
    }

    private fun showQuestionnaire(questions: List<Question>) {
        setAccessibilityTitle(R.string.select_symptoms)

        questionListContainer.visible()
        loadingContainer.gone()
        errorStateContainer.gone()
        submitQuestions(questions)
    }

    private fun submitQuestions(questions: List<Question>) {
        questionnaireViewAdapter.submitList(questions)
    }

    private fun showNoSymptomsConfirmationDialog() {
        // To ensure that we don't display the dialog multiple times, we dismiss the current one, if any
        currentDialog?.let { dialog ->
            dialog.setOnDismissListener { }
            dialog.dismiss()
        }

        currentDialog = AlertDialog.Builder(this)
            .setTitle(R.string.questionnaire_discard_symptoms_dialog_title)
            .setMessage(R.string.questionnaire_discard_symptoms_dialog_message)
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.confirm) { _, _ ->
                finish()
                startActivity<NoSymptomsActivity>()
            }
            .setOnDismissListener {
                currentDialog = null
                viewModel.onDialogDismissed()
            }
            .show()
    }

    private fun setupAdapter() {
        questionnaireViewAdapter = QuestionnaireViewAdapter {
            viewModel.toggleQuestion(it)
        }
        questionsRecyclerView.layoutManager =
            ScrollableLayoutManager(
                this,
                nestedScrollView,
                questionsRecyclerView
            )
        questionsRecyclerView.adapter = questionnaireViewAdapter
    }

    override fun onDestroy() {
        currentDialog?.setOnDismissListener { }
        // To avoid leaking the window
        currentDialog?.dismiss()
        currentDialog = null

        super.onDestroy()
    }

    companion object {
        private const val CHANGE_QUESTION_REQUEST_CODE = 12345
        const val QUESTION_TO_CHANGE_KEY = "QUESTION_TO_CHANGE_KEY"
    }
}
