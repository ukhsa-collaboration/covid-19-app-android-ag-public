package uk.nhs.nhsx.covid19.android.app.scenariodialog

import android.content.Context
import android.os.Bundle
import android.os.Parcel
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker.Builder
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.DialogVirologyTestResultMockBinding
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.ReviewSymptomsActivity.Companion.SELECTED_DATE_FORMAT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultOnsetDateActivity
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class VirologyTestResultMockFragment : BottomSheetDialogFragment() {
    @Inject
    lateinit var factory: ViewModelFactory<VirologyTestResultMockViewModel>

    @Inject
    lateinit var clock: Clock
    private val viewModel: VirologyTestResultMockViewModel by viewModels { factory }

    private var testEndDateMs: Long = 0
    private var _binding: DialogVirologyTestResultMockBinding? = null
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().appComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogVirologyTestResultMockBinding.inflate(inflater)
        activity?.let {
            with(binding) {
                virologyTestResultMockSpinner
                    .apply {
                        adapter = ArrayAdapter(
                            context,
                            android.R.layout.simple_spinner_dropdown_item,
                            VirologyTestResult.values()
                        )
                    }

                virologyTestKitTypeMockSpinner
                    .apply {
                        adapter = ArrayAdapter(
                            context,
                            android.R.layout.simple_spinner_dropdown_item,
                            VirologyTestKitType.values()
                        )
                    }

                virologySelectDateContainer.setOnSingleClickListener {
                    val datePicker = Builder
                        .datePicker()
                        .setSelection(testEndDateMs)
                        .setCalendarConstraints(setupCalendarConstraints())
                        .build()
                    datePicker.show(childFragmentManager, LinkTestResultOnsetDateActivity::class.java.name)
                    datePicker.addOnPositiveButtonClickListener { dateInMillis ->
                        testEndDateMs = dateInMillis
                        virologyTextSelectDate.text =
                            DateTimeFormatter.ofPattern(SELECTED_DATE_FORMAT)
                                .format(Instant.ofEpochMilli(testEndDateMs).toLocalDate(ZoneOffset.UTC))

                        Timber.d("testEndDateMs = $testEndDateMs")
                    }
                }
                virologySubmitButton.setOnSingleClickListener {
                    passOnDataToViewModel()
                    dismiss()
                }

                populateViewsWithAvailableData()
            }
        }
        return binding.root
    }

    private fun populateViewsWithAvailableData() = with(binding) {
        viewModel.virologyCtaExchangeResponse.observe(this@VirologyTestResultMockFragment) { response ->
            virologyDiagnosisKeySubmissionSupportedMock.isChecked = response.diagnosisKeySubmissionSupported
            virologyRequiresConfirmatoryTest.isChecked = response.requiresConfirmatoryTest
            virologyShouldOfferFollowUpTest.isChecked = response.shouldOfferFollowUpTest
            virologyTestResultMockDiagnosisKeySubmissionToken.setText(response.diagnosisKeySubmissionToken)
            virologyConfirmatoryDayLimit.setText(response.confirmatoryDayLimit?.toString())
            virologyTestResultMockSpinner.setSelection(VirologyTestResult.values().indexOf(response.testResult))
            virologyTestKitTypeMockSpinner.setSelection(VirologyTestKitType.values().indexOf(response.testKit))
            virologyTextSelectDate.text = DateTimeFormatter.ofPattern(SELECTED_DATE_FORMAT)
                .format(Instant.ofEpochMilli(response.testEndDate.toEpochMilli()).toLocalDate(ZoneOffset.UTC))
            Timber.d("response.testEndDate.toEpochMilli() = ${response.testEndDate.toEpochMilli()}")
            testEndDateMs = response.testEndDate.toEpochMilli()
        }
    }

    private fun passOnDataToViewModel() = with(binding) {
        val diagnosisKeySubmissionToken =
            virologyTestResultMockDiagnosisKeySubmissionToken.text.toString()
        val virologyTestResultValue =
            VirologyTestResult.valueOf(virologyTestResultMockSpinner.selectedItem.toString())
        val virologyTestKitType =
            VirologyTestKitType.valueOf(virologyTestKitTypeMockSpinner.selectedItem.toString())
        val diagnosisKeySubmissionSupported = virologyDiagnosisKeySubmissionSupportedMock.isChecked
        val requiresConfirmatoryTest = virologyRequiresConfirmatoryTest.isChecked
        val shouldOfferFollowUpTest = virologyShouldOfferFollowUpTest.isChecked
        val confirmatoryDayLimit =
            with(virologyConfirmatoryDayLimit.text.toString()) {
                if (isNullOrBlank()) null else toInt()
            }
        viewModel.mockResponse(
            diagnosisKeySubmissionToken = diagnosisKeySubmissionToken,
            virologyTestResultValue = virologyTestResultValue,
            virologyTestKitType = virologyTestKitType,
            diagnosisKeySubmissionSupported = diagnosisKeySubmissionSupported,
            requiresConfirmatoryTest = requiresConfirmatoryTest,
            shouldOfferFollowUpTest = shouldOfferFollowUpTest,
            confirmatoryDayLimit = confirmatoryDayLimit,
            testEndDate = Instant.ofEpochMilli(testEndDateMs)
        )
    }

    private fun setupCalendarConstraints(): CalendarConstraints {

        val maxDateValidator = object : CalendarConstraints.DateValidator {
            override fun isValid(date: Long) = date <= clock.millis()

            override fun writeToParcel(dest: Parcel?, flags: Int) = Unit

            override fun describeContents(): Int = 0
        }

        return CalendarConstraints.Builder()
            .setValidator(maxDateValidator)
            .setOpenAt(testEndDateMs)
            .build()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
