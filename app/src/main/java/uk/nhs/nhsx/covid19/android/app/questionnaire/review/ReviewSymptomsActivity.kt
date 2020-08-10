package uk.nhs.nhsx.covid19.android.app.questionnaire.review

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.CannotRememberDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.ExplicitDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.NotStated
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.Question
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.SymptomsReviewAdapter
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.QuestionnaireActivity.Companion.QUESTION_TO_CHANGE_KEY
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.util.gone
import uk.nhs.nhsx.covid19.android.app.util.invisible
import uk.nhs.nhsx.covid19.android.app.util.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.visible
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ReviewSymptomsActivity : AppCompatActivity(R.layout.activity_review_symptoms) {
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
            R.drawable.ic_arrow_back_white
        )

        val questions = intent.getParcelableArrayListExtra<Question>(EXTRA_QUESTIONS) ?: return
        val riskThreshold = intent.getFloatExtra(EXTRA_RISK_THRESHOLD, 0.0F)
        val symptomsOnsetWindowDays = intent.getIntExtra(EXTRA_SYMPTOMS_ONSET_WINDOW_DAYS, 0)
        viewModel.setup(questions, riskThreshold, symptomsOnsetWindowDays)

        setupListeners()
        setupViewModelListeners()
    }

    private fun setupViewModelListeners() {
        viewModel.viewState().observe(
            this,
            Observer { viewState ->
                updateSymptoms(viewState.reviewSymptomItems)
                updateOnsetDate(viewState.onsetDate)
                setOnsetErrorVisibility(viewState.showOnsetDateError)
                calendarConstraints = setupCalendarConstraints(viewState.symptomsOnsetWindowDays)
            }
        )

        viewModel.navigateToIsolationScreen().observe(
            this,
            Observer { userInIsolationState: Boolean ->
                if (userInIsolationState) {
                    PositiveSymptomsActivity.start(this)
                    finish()
                } else {
                    startActivity<NoSymptomsActivity> {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                }
            }
        )
    }

    private fun setOnsetErrorVisibility(showOnsetDateError: Boolean) {
        if (showOnsetDateError) {
            reviewSymptomsErrorContainer.visible()
            dateSelectionErrorBar.visible()
            scrollViewReviewSymptoms.smoothScrollTo(0, 0)
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
        selectDateContainer.setOnClickListener {
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

        buttonConfirmSymptoms.setOnClickListener {
            viewModel.onButtonConfirmedClicked()
        }
    }

    private fun updateSymptoms(reviewSymptomItems: List<ReviewSymptomItem>) {
        listReviewSymptoms.layoutManager = LinearLayoutManager(this)
        listReviewSymptoms.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )
        listReviewSymptoms.adapter =
            SymptomsReviewAdapter(
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
            override fun isValid(date: Long) = isTodayOrDefinedPeriodBefore(date)
            private fun isTodayOrDefinedPeriodBefore(date: Long): Boolean =
                date <= LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli() &&
                    date > LocalDateTime.now()
                    .minusDays(symptomsOnsetWindowDays.toLong())
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()

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
