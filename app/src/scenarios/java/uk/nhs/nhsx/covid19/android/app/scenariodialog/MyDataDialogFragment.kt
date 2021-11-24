package uk.nhs.nhsx.covid19.android.app.scenariodialog

import android.view.LayoutInflater
import android.widget.CompoundButton
import uk.nhs.nhsx.covid19.android.app.about.mydata.BaseMyDataViewModel.IsolationViewState
import uk.nhs.nhsx.covid19.android.app.databinding.DialogUserDataBinding
import uk.nhs.nhsx.covid19.android.app.di.viewmodel.MockMyDataViewModel
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import java.time.LocalDate

class MyDataDialogFragment(positiveAction: (() -> Unit)) :
    ScenarioDialogFragment<DialogUserDataBinding>(positiveAction) {
    override val title: String = "User Data Config"

    override fun setupBinding(inflater: LayoutInflater) = DialogUserDataBinding.inflate(inflater)
    override fun setupView() {
        setUpUseMock()
        setUpConfiguration()
    }

    private fun setUpUseMock() = with(binding.useMock) {
        setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
            MockMyDataViewModel.currentOptions =
                MockMyDataViewModel.currentOptions.copy(useMock = checked)
            binding.optionalContainer.visibility = checked.toViewState()
        }
        isChecked = MockMyDataViewModel.currentOptions.useMock
    }

    private fun setUpConfiguration() {
        with(binding.mockIsolationState) {
            isChecked = MockMyDataViewModel.currentOptions.isolationViewState != null
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    MockMyDataViewModel.currentOptions = MockMyDataViewModel.currentOptions.copy(
                        isolationViewState = IsolationViewState(
                            lastDayOfIsolation = lastDayOfIsolation,
                            contactCaseEncounterDate = contactCaseEncounterDate,
                            contactCaseNotificationDate = contactCaseNotificationDate,
                            indexCaseSymptomOnsetDate = indexCaseSymptomOnsetDate,
                            optOutOfContactIsolationDate = optOutOfContactIsolationDate
                        )
                    )
                } else {
                    MockMyDataViewModel.currentOptions = MockMyDataViewModel.currentOptions.copy(
                        isolationViewState = null
                    )
                }
            }
        }

        with(binding.mockLastRiskyVenueVisitDate) {
            isChecked = MockMyDataViewModel.currentOptions.lastRiskyVenueVisitDate != null
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    MockMyDataViewModel.currentOptions = MockMyDataViewModel.currentOptions.copy(
                        lastRiskyVenueVisitDate = LocalDate.now().minusDays(5)
                    )
                } else {
                    MockMyDataViewModel.currentOptions = MockMyDataViewModel.currentOptions.copy(
                        lastRiskyVenueVisitDate = null
                    )
                }
            }
        }

        with(binding.mockAcknowledgedTestResult) {
            isChecked = MockMyDataViewModel.currentOptions.acknowledgedTestResult != null
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    MockMyDataViewModel.currentOptions = MockMyDataViewModel.currentOptions.copy(
                        acknowledgedTestResult = AcknowledgedTestResult(
                            testEndDate = LocalDate.now(),
                            testResult = POSITIVE,
                            acknowledgedDate = LocalDate.now(),
                            testKitType = LAB_RESULT,
                            requiresConfirmatoryTest = false,
                            confirmedDate = null
                        )
                    )
                } else {
                    MockMyDataViewModel.currentOptions = MockMyDataViewModel.currentOptions.copy(
                        acknowledgedTestResult = null
                    )
                }
            }
        }
    }

    private val contactCaseEncounterDate = LocalDate.now().minusDays(5)
    private val contactCaseNotificationDate = LocalDate.now().minusDays(4)
    private val optOutOfContactIsolationDate = LocalDate.now().plusDays(5)
    private val indexCaseSymptomOnsetDate = LocalDate.now().minusDays(10)
    private val lastDayOfIsolation = LocalDate.now().plusDays(4)
}
