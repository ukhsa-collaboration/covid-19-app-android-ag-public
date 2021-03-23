package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.view.accessibility.AccessibilityEvent
import androidx.activity.viewModels
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker.Builder
import kotlinx.android.synthetic.main.activity_link_test_result_onset_date.dateSelectionErrorBar
import kotlinx.android.synthetic.main.activity_link_test_result_onset_date.linkTestResultOnsetDateCheckboxNoDate
import kotlinx.android.synthetic.main.activity_link_test_result_onset_date.linkTestResultOnsetDateContinueButton
import kotlinx.android.synthetic.main.activity_link_test_result_onset_date.linkTestResultOnsetDateErrorContainer
import kotlinx.android.synthetic.main.activity_link_test_result_onset_date.linkTestResultOnsetDateSelectDateContainer
import kotlinx.android.synthetic.main.activity_link_test_result_onset_date.linkTestResultScrollView
import kotlinx.android.synthetic.main.activity_link_test_result_onset_date.textSelectDate
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.ReviewSymptomsActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.CannotRememberDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.ExplicitDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.NotStated
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.invisible
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setToolbarNoNavigation
import uk.nhs.nhsx.covid19.android.app.util.viewutils.smoothScrollToAndThen
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class LinkTestResultOnsetDateActivity : BaseActivity(R.layout.activity_link_test_result_onset_date) {

    private lateinit var calendarConstraints: CalendarConstraints

    @Inject
    lateinit var factory: ViewModelFactory<LinkTestResultOnsetDateViewModel>

    private val viewModel by viewModels<LinkTestResultOnsetDateViewModel> { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setToolbarNoNavigation(
            toolbar,
            R.string.link_test_result_symptoms_information_title
        )

        intent.getParcelableExtra<ReceivedTestResult>(EXTRA_TEST_RESULT)?.let {
            viewModel.onCreate(it)

            setupViewModelListeners()
            setupListeners()
        } ?: finish()
    }

    override fun onBackPressed() = Unit

    private fun setupListeners() {
        linkTestResultOnsetDateSelectDateContainer.setOnSingleClickListener {
            viewModel.onDatePickerContainerClicked()
        }

        setOnsetDateCheckboxNoDateListener()

        linkTestResultOnsetDateContinueButton.setOnSingleClickListener {
            viewModel.onButtonContinueClicked()
        }
    }

    private fun setOnsetDateCheckboxNoDateListener() {
        linkTestResultOnsetDateCheckboxNoDate.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.cannotRememberDateChecked()
            } else {
                viewModel.cannotRememberDateUnchecked()
            }
        }
    }

    private fun removeOnsetDateCheckboxNoDateListener() {
        linkTestResultOnsetDateCheckboxNoDate.setOnCheckedChangeListener(null)
    }

    private fun setupViewModelListeners() {
        viewModel.viewState().observe(this) { viewState ->
            updateOnsetDate(viewState.onsetDate)
            setOnsetErrorVisibility(viewState.showOnsetDateError)
            calendarConstraints = setupCalendarConstraints(viewState.symptomsOnsetWindowDays)
        }

        viewModel.datePickerContainerClicked().observe(this) { currentTimeInMs ->
            val datePicker = Builder
                .datePicker()
                .setSelection(currentTimeInMs)
                .setCalendarConstraints(calendarConstraints)
                .build()
            datePicker.show(supportFragmentManager, LinkTestResultOnsetDateActivity::class.java.name)
            datePicker.addOnPositiveButtonClickListener { dateInMillis ->
                viewModel.onDateSelected(dateInMillis)
            }
        }

        viewModel.continueEvent().observe(this) {
            finish()
        }
    }

    private fun setOnsetErrorVisibility(showOnsetDateError: Boolean) {
        if (showOnsetDateError) {
            dateSelectionErrorBar.visible()
            linkTestResultOnsetDateErrorContainer.visible()
            linkTestResultScrollView.smoothScrollToAndThen(0, 0) {
                linkTestResultOnsetDateErrorContainer.requestFocusFromTouch()
                linkTestResultOnsetDateErrorContainer.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
            }
        } else {
            dateSelectionErrorBar.invisible()
            linkTestResultOnsetDateErrorContainer.gone()
        }
    }

    private fun updateOnsetDate(onsetDate: SelectedDate) {
        textSelectDate.text = when (onsetDate) {
            is NotStated, is CannotRememberDate -> getString(R.string.questionnaire_select_a_date)
            is ExplicitDate -> {
                removeOnsetDateCheckboxNoDateListener()
                linkTestResultOnsetDateCheckboxNoDate.isChecked = false
                setOnsetDateCheckboxNoDateListener()

                DateTimeFormatter.ofPattern(ReviewSymptomsActivity.SELECTED_DATE_FORMAT)
                    .format(onsetDate.date)
            }
        }
    }

    private fun setupCalendarConstraints(symptomsOnsetWindowDays: ClosedRange<LocalDate>): CalendarConstraints {

        val maxDateValidator = object : CalendarConstraints.DateValidator {
            override fun isValid(date: Long) = viewModel.isOnsetDateValid(date, symptomsOnsetWindowDays)

            override fun writeToParcel(dest: Parcel?, flags: Int) = Unit

            override fun describeContents(): Int = 0
        }

        return CalendarConstraints.Builder()
            .setValidator(maxDateValidator)
            .setOpenAt(symptomsOnsetWindowDays.endInclusive.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
            .build()
    }

    companion object {
        const val EXTRA_TEST_RESULT = "EXTRA_TEST_RESULT"

        fun start(context: Context, testResult: ReceivedTestResult) =
            context.startActivity(getIntent(context, testResult))

        private fun getIntent(context: Context, testResult: ReceivedTestResult) =
            Intent(context, LinkTestResultOnsetDateActivity::class.java)
                .putExtra(EXTRA_TEST_RESULT, testResult)
    }
}
