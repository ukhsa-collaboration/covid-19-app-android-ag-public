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
import uk.nhs.nhsx.covid19.android.app.databinding.ActivitySelectTestDateBinding
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.CannotRememberDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.ExplicitDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.NotStated
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelectTestDateViewModel.NavigationTarget.CheckAnswers
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelectTestDateViewModel.NavigationTarget.ReportedTest
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelectTestDateViewModel.NavigationTarget.Symptoms
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelectTestDateViewModel.NavigationTarget.TestKitType
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelectTestDateViewModel.NavigationTarget.TestOrigin
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

class SelectTestDateActivity : BaseActivity() {

    @Inject
    lateinit var factory: SelectTestDateViewModel.Factory

    private val viewModel: SelectTestDateViewModel by assistedViewModel {
        factory.create(
            questions = intent.getParcelableExtra(SELF_REPORT_QUESTIONS_DATA_KEY)
                ?: throw IllegalStateException("self report questions data was not available from starting intent")
        )
    }

    private lateinit var binding: ActivitySelectTestDateBinding
    private lateinit var calendarConstraints: CalendarConstraints

    private var hasScrolledToError: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivitySelectTestDateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setNavigateUpToolbarWithoutTitle(
            binding.primaryToolbar.toolbar,
            upIndicator = R.drawable.ic_arrow_back_white,
            upContentDescription = R.string.self_report_test_date_back_button_accessibility_description
        )

        setClickListeners()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.viewState().observe(this) { viewState ->
            updateTestDate(viewState.selectedTestDate)
            showError(viewState.hasError)
            calendarConstraints = setupCalendarConstraints(viewState.testDateWindowDays)
        }

        viewModel.datePickerContainerClicked().observe(this) { currentTimeInMs ->
            val datePicker = Builder
                .datePicker()
                .setSelection(currentTimeInMs)
                .setCalendarConstraints(calendarConstraints)
                .build()
            datePicker.show(supportFragmentManager, SelectTestDateActivity::class.java.name)
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
                is TestOrigin -> {
                    startActivity<TestOriginActivity> {
                        putExtra(
                            TestOriginActivity.SELF_REPORT_QUESTIONS_DATA_KEY,
                            navTarget.selfReportTestQuestions
                        )
                    }
                    finish()
                }
                is TestKitType -> {
                    startActivity<TestKitTypeActivity> {
                        putExtra(
                            TestKitTypeActivity.SELF_REPORT_QUESTIONS_DATA_KEY,
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
        selfReportTestDateSelectDateContainer.setOnSingleClickListener {
            viewModel.onDatePickerContainerClicked()
        }

        setTestDateCheckboxNoDateListener()

        selfReportTestDateContinueButton.setOnSingleClickListener {
            viewModel.onButtonContinueClicked()
        }
    }

    private fun updateTestDate(testDate: SelectedDate) = with(binding) {
        textSelectDate.text = when (testDate) {
            is NotStated -> getString(R.string.self_report_test_date_date_picker_box_label)
            is CannotRememberDate -> {
                if (!selfReportTestDateSelectCheckboxNoDate.isChecked) {
                    removeTestDateCheckboxNoDateListener()
                    selfReportTestDateSelectCheckboxNoDate.isChecked = true
                    setTestDateCheckboxNoDateListener()
                }
                getString(R.string.self_report_test_date_date_picker_box_label)
            }
            is ExplicitDate -> {
                if (selfReportTestDateSelectCheckboxNoDate.isChecked) {
                    removeTestDateCheckboxNoDateListener()
                    selfReportTestDateSelectCheckboxNoDate.isChecked = false
                    setTestDateCheckboxNoDateListener()
                }
                DateTimeFormatter.ofPattern(SELECTED_DATE_FORMAT)
                    .format(testDate.date)
            }
        }
    }

    private fun setTestDateCheckboxNoDateListener() {
        binding.selfReportTestDateSelectCheckboxNoDate.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.cannotRememberDateChecked()
            } else {
                viewModel.cannotRememberDateUnchecked()
            }
        }
    }

    private fun removeTestDateCheckboxNoDateListener() {
        binding.selfReportTestDateSelectCheckboxNoDate.setOnCheckedChangeListener(null)
    }

    private fun showError(hasError: Boolean) = with(binding) {
        if (hasError) {
            selfReportTestDateErrorView.visible()
            selfReportTestDateErrorIndicator.visible()
            selfReportTestDateErrorText.visible()
            if (!hasScrolledToError) {
                selfReportTestDateScrollViewContainer.smoothScrollToAndThen(0, 0) {
                    selfReportTestDateErrorView.requestFocusFromTouch()
                    selfReportTestDateErrorView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                    hasScrolledToError = true
                }
            }
        } else {
            selfReportTestDateErrorView.gone()
            selfReportTestDateErrorIndicator.invisible()
            selfReportTestDateErrorText.gone()
        }
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    private fun setupCalendarConstraints(testDateWindowDays: ClosedRange<LocalDate>): CalendarConstraints {

        val maxDateValidator = object : CalendarConstraints.DateValidator {
            override fun isValid(date: Long) = viewModel.isTestDateValid(date, testDateWindowDays)

            override fun writeToParcel(dest: Parcel?, flags: Int) = Unit

            override fun describeContents(): Int = 0
        }

        return CalendarConstraints.Builder()
            .setValidator(maxDateValidator)
            .setOpenAt(testDateWindowDays.endInclusive.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
            .build()
    }

    companion object {
        const val SELF_REPORT_QUESTIONS_DATA_KEY = "SELF_REPORT_QUESTIONS_DATA_KEY"
        const val SELECTED_DATE_FORMAT = "d MMM yyyy"
    }
}
