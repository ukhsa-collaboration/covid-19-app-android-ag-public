package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import android.os.Bundle
import android.os.Parcel
import android.view.accessibility.AccessibilityEvent
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker.Builder
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.assistedViewModel
import uk.nhs.nhsx.covid19.android.app.databinding.ActivitySelfReportSymptomsOnsetBinding
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.CannotRememberDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.ExplicitDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.NotStated
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSymptomsOnsetViewModel.NavigationTarget.CheckAnswers
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSymptomsOnsetViewModel.NavigationTarget.ReportedTest
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSymptomsOnsetViewModel.NavigationTarget.Symptoms
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.invisible
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbarWithoutTitle
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.smoothScrollToAndThen
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class SelfReportSymptomsOnsetActivity : BaseActivity() {

    @Inject
    lateinit var factory: SelfReportSymptomsOnsetViewModel.Factory

    private val viewModel: SelfReportSymptomsOnsetViewModel by assistedViewModel {
        factory.create(
            questions = intent.getParcelableExtra(SELF_REPORT_QUESTIONS_DATA_KEY)
                ?: throw IllegalStateException("self report questions data was not available from starting intent")
        )
    }

    private lateinit var binding: ActivitySelfReportSymptomsOnsetBinding
    private lateinit var calendarConstraints: CalendarConstraints

    private var hasScrolledToError: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivitySelfReportSymptomsOnsetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setNavigateUpToolbarWithoutTitle(
            binding.primaryToolbar.toolbar,
            upIndicator = R.drawable.ic_arrow_back_white,
            upContentDescription = R.string.self_report_symptoms_date_back_button_accessibility_description
        )

        setClickListeners()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.viewState().observe(this) { viewState ->
            updateSymptomsOnsetDate(viewState.selectedOnsetDate)
            showError(viewState.hasError)
            calendarConstraints = setupCalendarConstraints(viewState.symptomsOnsetWindowDays)
        }

        viewModel.datePickerContainerClicked().observe(this) { testEndTimeInMs ->
            val datePicker = Builder
                .datePicker()
                .setSelection(testEndTimeInMs)
                .setCalendarConstraints(calendarConstraints)
                .build()
            datePicker.show(supportFragmentManager, SelfReportSymptomsOnsetActivity::class.java.name)
            datePicker.addOnPositiveButtonClickListener { dateInMillis ->
                viewModel.onDateSelected(dateInMillis)
            }
        }

        viewModel.navigate().observe(this) { navTarget ->
            when (navTarget) {
                is Symptoms -> {
                    startActivity<SelfReportSymptomsActivity> {
                        putExtra(
                            SelfReportSymptomsActivity.SELF_REPORT_QUESTIONS_DATA_KEY,
                            navTarget.selfReportTestQuestions
                        )
                    }
                    finish()
                }
                is ReportedTest -> {
                    startActivity<ReportedTestActivity> {
                        putExtra(
                            ReportedTestActivity.SELF_REPORT_QUESTIONS_DATA_KEY,
                            navTarget.selfReportTestQuestions
                        )
                    }
                    finish()
                }
                is CheckAnswers -> {
                    startActivity<SelfReportCheckAnswersActivity> {
                        putExtra(
                            SelfReportCheckAnswersActivity.SELF_REPORT_QUESTIONS_DATA_KEY,
                            navTarget.selfReportTestQuestions
                        )
                    }
                    finish()
                }
            }
        }
    }

    private fun setClickListeners() = with(binding) {
        selfReportSymptomsOnsetDateSelectDateContainer.setOnSingleClickListener {
            viewModel.onDatePickerContainerClicked()
        }

        setSymptomsOnsetDateCheckboxNoDateListener()

        selfReportSymptomsOnsetDateContinueButton.setOnSingleClickListener {
            viewModel.onButtonContinueClicked()
        }
    }

    private fun updateSymptomsOnsetDate(symptomsOnsetDate: SelectedDate) = with(binding) {
        textSelectDate.text = when (symptomsOnsetDate) {
            is NotStated -> getString(R.string.self_report_symptoms_date_date_picker_box_label)
            is CannotRememberDate -> {
                if (!selfReportSymptomsOnsetDateSelectCheckboxNoDate.isChecked) {
                    removeSymptomsOnsetDateCheckboxNoDateListener()
                    selfReportSymptomsOnsetDateSelectCheckboxNoDate.isChecked = true
                    setSymptomsOnsetDateCheckboxNoDateListener()
                }
                getString(R.string.self_report_symptoms_date_date_picker_box_label)
            }
            is ExplicitDate -> {
                if (selfReportSymptomsOnsetDateSelectCheckboxNoDate.isChecked) {
                    removeSymptomsOnsetDateCheckboxNoDateListener()
                    selfReportSymptomsOnsetDateSelectCheckboxNoDate.isChecked = false
                    setSymptomsOnsetDateCheckboxNoDateListener()
                }
                DateTimeFormatter.ofPattern(SELECTED_DATE_FORMAT)
                    .format(symptomsOnsetDate.date)
            }
        }
    }

    private fun setSymptomsOnsetDateCheckboxNoDateListener() {
        binding.selfReportSymptomsOnsetDateSelectCheckboxNoDate.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.cannotRememberDateChecked()
            } else {
                viewModel.cannotRememberDateUnchecked()
            }
        }
    }

    private fun removeSymptomsOnsetDateCheckboxNoDateListener() {
        binding.selfReportSymptomsOnsetDateSelectCheckboxNoDate.setOnCheckedChangeListener(null)
    }

    private fun showError(hasError: Boolean) = with(binding) {
        if (hasError) {
            selfReportSymptomsOnsetDateErrorView.visible()
            selfReportSymptomsOnsetDateErrorIndicator.visible()
            selfReportSymptomsOnsetDateErrorText.visible()
            if (!hasScrolledToError) {
                selfReportSymptomsOnsetDateScrollViewContainer.smoothScrollToAndThen(0, 0) {
                    selfReportSymptomsOnsetDateErrorView.requestFocusFromTouch()
                    selfReportSymptomsOnsetDateErrorView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                    hasScrolledToError = true
                }
            }
        } else {
            selfReportSymptomsOnsetDateErrorView.gone()
            selfReportSymptomsOnsetDateErrorIndicator.invisible()
            selfReportSymptomsOnsetDateErrorText.gone()
        }
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    private fun setupCalendarConstraints(symptomsOnsetWindowDays: ClosedRange<LocalDate>): CalendarConstraints {

        val maxDateValidator = object : CalendarConstraints.DateValidator {
            override fun isValid(date: Long) = viewModel.isSymptomsOnsetDateValid(date, symptomsOnsetWindowDays)

            override fun writeToParcel(dest: Parcel?, flags: Int) = Unit

            override fun describeContents(): Int = 0
        }

        return CalendarConstraints.Builder()
            .setValidator(maxDateValidator)
            .setOpenAt(symptomsOnsetWindowDays.endInclusive.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
            .build()
    }

    companion object {
        const val SELF_REPORT_QUESTIONS_DATA_KEY = "SELF_REPORT_QUESTIONS_DATA_KEY"
        const val SELECTED_DATE_FORMAT = "d MMM yyyy"
    }
}
