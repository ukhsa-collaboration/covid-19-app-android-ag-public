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
import uk.nhs.nhsx.covid19.android.app.about.BaseMyDataViewModel.IsolationState
import uk.nhs.nhsx.covid19.android.app.di.viewmodel.MockMyDataViewModel
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import java.time.Instant
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
            isChecked = MockMyDataViewModel.currentOptions.isolationState != null
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    MockMyDataViewModel.currentOptions = MockMyDataViewModel.currentOptions.copy(
                        isolationState = IsolationState(
                            lastDayOfIsolation = contactCaseOnlyIsolation.lastDayOfIsolation,
                            contactCaseEncounterDate = contactCaseEncounterDate,
                            contactCaseNotificationDate = contactCaseNotificationDate,
                            indexCaseSymptomOnsetDate = contactCaseOnlyIsolation.indexCase?.symptomsOnsetDate,
                            dailyContactTestingOptInDate = dailyContactTestingOptInDate
                        )
                    )
                } else {
                    MockMyDataViewModel.currentOptions = MockMyDataViewModel.currentOptions.copy(
                        isolationState = null
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
                            diagnosisKeySubmissionToken = "token",
                            testEndDate = Instant.now(),
                            testResult = POSITIVE,
                            acknowledgedDate = Instant.now(),
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

    private val contactCaseEncounterDate = Instant.parse("2020-05-19T12:00:00Z")
    private val contactCaseNotificationDate = Instant.parse("2020-05-20T12:00:00Z")
    private val dailyContactTestingOptInDate = LocalDate.now().plusDays(5)
    private val contactCaseOnlyIsolation = Isolation(
        isolationStart = Instant.now(),
        isolationConfiguration = DurationDays(),
        contactCase = ContactCase(
            startDate = contactCaseEncounterDate,
            notificationDate = contactCaseNotificationDate,
            expiryDate = LocalDate.now().plusDays(5),
            dailyContactTestingOptInDate = dailyContactTestingOptInDate
        )
    )
}
