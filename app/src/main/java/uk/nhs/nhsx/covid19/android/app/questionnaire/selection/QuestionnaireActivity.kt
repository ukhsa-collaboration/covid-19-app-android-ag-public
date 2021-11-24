package uk.nhs.nhsx.covid19.android.app.questionnaire.selection

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.NestedScrollView
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.Lce.Error
import uk.nhs.nhsx.covid19.android.app.common.Lce.Loading
import uk.nhs.nhsx.covid19.android.app.common.Lce.Success
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityQuestionnaireBinding
import uk.nhs.nhsx.covid19.android.app.questionnaire.NewNoSymptomsActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.NoSymptomsActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.ReviewSymptomsActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.ReviewSymptomsActivity.Companion.EXTRA_RISK_THRESHOLD
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.ReviewSymptomsActivity.Companion.EXTRA_SYMPTOMS_ONSET_WINDOW_DAYS
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SymptomsAdviceIsolateActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.Question
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.NavigationTarget.AdviceScreen
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.NavigationTarget.NewNoSymptoms
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.NavigationTarget.ReviewSymptoms
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.adapter.QuestionnaireViewAdapter
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.ScrollableLayoutManager
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setCloseToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.smoothScrollToAndThen
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import javax.inject.Inject

class QuestionnaireActivity : BaseActivity() {

    private lateinit var questionnaireViewAdapter: QuestionnaireViewAdapter

    @Inject
    lateinit var factory: ViewModelFactory<QuestionnaireViewModel>

    private val viewModel: QuestionnaireViewModel by viewModels { factory }

    private lateinit var binding: ActivityQuestionnaireBinding
    private lateinit var nestedScrollView: NestedScrollView

    /**
     * Dialog currently displayed, or null if none are displayed
     */
    private var currentDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityQuestionnaireBinding.inflate(layoutInflater)

        with(binding) {
            setContentView(root)
            setCloseToolbar(primaryToolbar.toolbar, R.string.select_symptoms)
            nestedScrollView = questionListContainer.questionnaireScrollView
        }

        setupListeners()
        setupAdapter()
        setupViewModelListeners()
        viewModel.loadQuestionnaire()
    }

    private fun setupViewModelListeners() {
        viewModel.viewState().observe(this) { viewState ->
            when (viewState) {
                is Success -> handleSuccess(viewState.data)
                is Error -> showErrorState()
                is Loading -> showLoadingSpinner()
            }
        }

        viewModel.navigationTarget().observe(this) { navigationTarget ->
            when (navigationTarget) {
                is ReviewSymptoms -> startReviewSymptomsActivity(navigationTarget)
                is AdviceScreen ->
                    if (navigationTarget.symptomAdvice is IsolationSymptomAdvice) {
                        SymptomsAdviceIsolateActivity.start(this, navigationTarget.symptomAdvice)
                    } else {
                        startActivity<NoSymptomsActivity> {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    }
                NewNoSymptoms -> startActivity<NewNoSymptomsActivity> {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            }
        }
    }

    private fun startReviewSymptomsActivity(reviewSymptomsNavigationTarget: ReviewSymptoms) {
        val extraQuestions = ArrayList<Question>().apply {
            addAll(reviewSymptomsNavigationTarget.questions)
        }

        val intent = Intent(this, ReviewSymptomsActivity::class.java).apply {
            putParcelableArrayListExtra(
                ReviewSymptomsActivity.EXTRA_QUESTIONS,
                extraQuestions
            )
            putExtra(EXTRA_RISK_THRESHOLD, reviewSymptomsNavigationTarget.riskThreshold)
            putExtra(
                EXTRA_SYMPTOMS_ONSET_WINDOW_DAYS,
                reviewSymptomsNavigationTarget.symptomsOnsetWindowDays
            )
        }

        startActivityForResult(intent, CHANGE_QUESTION_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == CHANGE_QUESTION_REQUEST_CODE) {
            val question = data?.getParcelableExtra<Question>(QUESTION_TO_CHANGE_KEY)

            val index =
                question?.let { questionnaireViewAdapter.currentList.indexOf(question) } ?: -1

            if (index >= 0) {
                (binding.questionListContainer.questionsRecyclerView.layoutManager as ScrollableLayoutManager)
                    .scrollToIndex(index)
            }
        }
    }

    private fun handleSuccess(state: QuestionnaireState) = with(binding.questionListContainer) {
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

    private fun setupListeners() = with(binding) {
        buttonTryAgain.setOnSingleClickListener {
            viewModel.loadQuestionnaire()
        }

        with(questionListContainer) {

            noSymptomsButton.setOnSingleClickListener {
                viewModel.onNoSymptomsClicked()
            }

            buttonReviewSymptoms.setOnSingleClickListener {
                viewModel.onButtonReviewSymptomsClicked()
            }
        }
    }

    private fun showLoadingSpinner() = with(binding) {
        loadingContainer.visible()
        errorStateContainer.gone()
        questionListContainer.root.gone()

        setAccessibilityTitle(R.string.loading)
    }

    private fun showErrorState() = with(binding) {
        errorStateContainer.visible()
        loadingContainer.gone()
        questionListContainer.root.gone()

        setAccessibilityTitle("${textErrorTitle.text}. ${textErrorMessage.text}")
    }

    private fun showQuestionnaire(questions: List<Question>) = with(binding) {
        setAccessibilityTitle(R.string.select_symptoms)

        questionListContainer.root.visible()
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
                viewModel.onNoSymptomsConfirmed()
            }
            .setOnDismissListener {
                currentDialog = null
                viewModel.onNoSymptomsDialogDismissed()
            }
            .show()
    }

    private fun setupAdapter() = with(binding.questionListContainer) {
        questionnaireViewAdapter = QuestionnaireViewAdapter {
            viewModel.toggleQuestion(it)
        }
        questionsRecyclerView.layoutManager =
            ScrollableLayoutManager(
                this@QuestionnaireActivity,
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
