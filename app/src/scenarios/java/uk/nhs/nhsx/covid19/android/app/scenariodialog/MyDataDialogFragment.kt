package uk.nhs.nhsx.covid19.android.app.scenariodialog

import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.appcompat.widget.SwitchCompat
import kotlinx.android.synthetic.scenarios.dialog_user_data.view.mockAcknowledgedTestResult
import kotlinx.android.synthetic.scenarios.dialog_user_data.view.mockDailyContactTestingOptInDateForIsolation
import kotlinx.android.synthetic.scenarios.dialog_user_data.view.mockIsolationState
import kotlinx.android.synthetic.scenarios.dialog_user_data.view.mockLastRiskyVenueVisitDate
import kotlinx.android.synthetic.scenarios.dialog_user_data.view.optionalContainer
import kotlinx.android.synthetic.scenarios.dialog_user_data.view.useMock
import uk.nhs.nhsx.covid19.android.app.R.layout
import uk.nhs.nhsx.covid19.android.app.about.mydata.BaseMyDataViewModel.IsolationViewState
import uk.nhs.nhsx.covid19.android.app.di.viewmodel.MockMyDataViewModel
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import java.time.LocalDate

class MyDataDialogFragment(positiveAction: (() -> Unit)) :
    ScenarioDialogFragment(positiveAction) {
    override val title: String = "User Data Config"
    override val layoutId = layout.dialog_user_data

    private lateinit var useMockSwitch: SwitchCompat
    private lateinit var optionalViews: ViewGroup

    override fun setUp(view: View) = with(view) {
        useMockSwitch = useMock
        optionalViews = optionalContainer

        setUpUseMock()
        setUpConfiguration(this)
    }

    private fun setUpUseMock() = with(useMockSwitch) {
        setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
            MockMyDataViewModel.currentOptions =
                MockMyDataViewModel.currentOptions.copy(useMock = checked)
            optionalViews.visibility = checked.toViewState()
        }
        isChecked = MockMyDataViewModel.currentOptions.useMock
    }

    private fun setUpConfiguration(view: View) {
        with(view.mockIsolationState) {
            isChecked = MockMyDataViewModel.currentOptions.isolationViewState != null
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    MockMyDataViewModel.currentOptions = MockMyDataViewModel.currentOptions.copy(
                        isolationViewState = IsolationViewState(
                            lastDayOfIsolation = (IsolationLogicalState.from(contactCaseOnlyIsolation) as PossiblyIsolating).lastDayOfIsolation,
                            contactCaseEncounterDate = contactCaseEncounterDate,
                            contactCaseNotificationDate = contactCaseNotificationDate,
                            indexCaseSymptomOnsetDate = ((contactCaseOnlyIsolation.indexInfo as? IndexCase)?.isolationTrigger as? SelfAssessment)?.onsetDate,
                            dailyContactTestingOptInDate = dailyContactTestingOptInDate
                        )
                    )
                } else {
                    MockMyDataViewModel.currentOptions = MockMyDataViewModel.currentOptions.copy(
                        isolationViewState = null
                    )
                }
            }
        }

        with(view.mockLastRiskyVenueVisitDate) {
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
        with(view.mockDailyContactTestingOptInDateForIsolation) {
            isChecked = MockMyDataViewModel.currentOptions.dailyContactTestingOptInDateForIsolation != null
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    MockMyDataViewModel.currentOptions = MockMyDataViewModel.currentOptions.copy(
                        dailyContactTestingOptInDateForIsolation = LocalDate.now().minusDays(5)
                    )
                } else {
                    MockMyDataViewModel.currentOptions = MockMyDataViewModel.currentOptions.copy(
                        dailyContactTestingOptInDateForIsolation = null
                    )
                }
            }
        }
        with(view.mockAcknowledgedTestResult) {
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

    private val contactCaseEncounterDate = LocalDate.parse("2020-05-19")
    private val contactCaseNotificationDate = LocalDate.parse("2020-05-20")
    private val dailyContactTestingOptInDate = LocalDate.now().plusDays(5)
    private val contactCaseOnlyIsolation = IsolationState(
        isolationConfiguration = DurationDays(),
        contactCase = ContactCase(
            exposureDate = contactCaseEncounterDate,
            notificationDate = contactCaseNotificationDate,
            expiryDate = LocalDate.now().plusDays(5),
            dailyContactTestingOptInDate = dailyContactTestingOptInDate
        )
    )
}
