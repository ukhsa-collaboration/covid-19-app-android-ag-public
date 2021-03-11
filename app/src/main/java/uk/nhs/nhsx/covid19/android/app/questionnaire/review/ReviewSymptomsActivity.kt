package uk.nhs.nhsx.covid19.android.app.questionnaire.review

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.view.accessibility.AccessibilityEvent
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker.Builder
import kotlinx.android.synthetic.main.activity_review_symptoms.buttonConfirmSymptoms
import kotlinx.android.synthetic.main.activity_review_symptoms.checkboxNoDate
import kotlinx.android.synthetic.main.activity_review_symptoms.dateSelectionErrorBar
import kotlinx.android.synthetic.main.activity_review_symptoms.listReviewSymptoms
import kotlinx.android.synthetic.main.activity_review_symptoms.reviewSymptomsErrorContainer
import kotlinx.android.synthetic.main.activity_review_symptoms.scrollViewReviewSymptoms
import kotlinx.android.synthetic.main.activity_review_symptoms.selectDateContainer
import kotlinx.android.synthetic.main.activity_review_symptoms.textSelectDate
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.CannotRememberDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.ExplicitDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.NotStated
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SymptomAdvice.DoNotIsolate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SymptomAdvice.Isolate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.Question
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.SymptomsReviewViewAdapter
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.QuestionnaireActivity.Companion.QUESTION_TO_CHANGE_KEY
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.invisible
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.smoothScrollToAndThen
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ReviewSymptomsActivity : BaseActivity(R.layout.activity_review_symptoms) {

    private lateinit var calendarConstraints: CalendarConstraints

    @Inject
    lateinit var factory: ViewModelFactory<ReviewSymptomsViewModel>

    private val viewModel by viewModels<ReviewSymptomsViewModel> { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setNavigateUpToolbar(
            toolbar,
            R.string.questionnaire_review_symptoms,
            upIndicator = R.drawable.ic_arrow_back_white
        )

        val questions = intent.getParcelableArrayListExtra<Question>(EXTRA_QUESTIONS) ?: return
        val riskThreshold = intent.getFloatExtra(EXTRA_RISK_THRESHOLD, 0.0F)
        val symptomsOnsetWindowDays = intent.getIntExtra(EXTRA_SYMPTOMS_ONSET_WINDOW_DAYS, 0)
        viewModel.setup(questions, riskThreshold, symptomsOnsetWindowDays)

        setupListeners()
        setupViewModelListeners()
    }

    private fun setupViewModelListeners() {
        viewModel.viewState().observe(this) { viewState ->
            updateSymptoms(viewState.reviewSymptomItems)
            updateOnsetDate(viewState.onsetDate)
            setOnsetErrorVisibility(viewState.showOnsetDateError)
            calendarConstraints = setupCalendarConstraints(viewState.symptomsOnsetWindowDays)
        }

        viewModel.navigateToSymptomAdviceScreen().observe(
            this,
            Observer { symptomAdvice: SymptomAdvice ->
                when (symptomAdvice) {
                    is DoNotIsolate -> {
                        startActivity<NoSymptomsActivity> {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    }
                    is Isolate -> {
                        SymptomsAdviceIsolateActivity.start(
                            this,
                            symptomAdvice.isPositiveSymptoms,
                            symptomAdvice.isolationDurationDays
                        )
                        finish()
                    }
                }
            }
        )
    }

    private fun setOnsetErrorVisibility(showOnsetDateError: Boolean) {
        if (showOnsetDateError) {
            reviewSymptomsErrorContainer.visible()
            dateSelectionErrorBar.visible()
            scrollViewReviewSymptoms.smoothScrollToAndThen(0, 0) {
                reviewSymptomsErrorContainer.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
            }
        } else {
            reviewSymptomsErrorContainer.gone()
            dateSelectionErrorBar.invisible()
        }
    }

    private fun updateOnsetDate(onsetDate: SelectedDate) {
        textSelectDate.text = when (onsetDate) {
            NotStated, CannotRememberDate -> getString(R.string.questionnaire_select_a_date)
            is ExplicitDate -> {
                checkboxNoDate.isChecked = false
                DateTimeFormatter.ofPattern(SELECTED_DATE_FORMAT)
                    .format(onsetDate.date)
            }
        }
    }

    private fun setupListeners() {
        selectDateContainer.setOnSingleClickListener {
            val datePicker = Builder
                .datePicker()
                .setCalendarConstraints(calendarConstraints)
                .build()
            datePicker.show(supportFragmentManager, ReviewSymptomsActivity::class.java.name)
            datePicker.addOnPositiveButtonClickListener { dateInMillis ->
                viewModel.onDateSelected(dateInMillis)
            }
        }

        checkboxNoDate.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.cannotRememberDateChecked()
            } else {
                viewModel.cannotRememberDateUnchecked()
            }
        }

        buttonConfirmSymptoms.setOnSingleClickListener {
            viewModel.onButtonConfirmedClicked()
        }
    }

    private fun updateSymptoms(reviewSymptomItems: List<ReviewSymptomItem>) {
        listReviewSymptoms.layoutManager = LinearLayoutManager(this)

        if (listReviewSymptoms.itemDecorationCount == 0) {
            listReviewSymptoms.addItemDecoration(
                DividerItemDecoration(
                    this,
                    DividerItemDecoration.VERTICAL
                )
            )
        }

        listReviewSymptoms.adapter =
            SymptomsReviewViewAdapter(
                reviewSymptomItems,
                ::onChangeClicked
            )
    }

    private fun onChangeClicked(question: Question) {
        setResult(
            Activity.RESULT_OK,
            Intent().apply { putExtra(QUESTION_TO_CHANGE_KEY, question) }
        )
        finish()
    }

    private fun setupCalendarConstraints(symptomsOnsetWindowDays: Int): CalendarConstraints {

        val maxDateValidator = object : CalendarConstraints.DateValidator {
            override fun isValid(date: Long) = viewModel.isOnsetDateValid(date, symptomsOnsetWindowDays)

            override fun writeToParcel(dest: Parcel?, flags: Int) = Unit

            override fun describeContents(): Int = 0
        }

        return CalendarConstraints.Builder()
            .setValidator(maxDateValidator)
            .build()
    }

    companion object {
        const val EXTRA_QUESTIONS = "EXTRA_QUESTIONS"
        const val EXTRA_RISK_THRESHOLD = "EXTRA_RISK_THRESHOLD"
        const val EXTRA_SYMPTOMS_ONSET_WINDOW_DAYS = "EXTRA_SYMPTOMS_ONSET_WINDOW_DAYS"
        const val SELECTED_DATE_FORMAT = "dd MMM yyyy"
    }
}
