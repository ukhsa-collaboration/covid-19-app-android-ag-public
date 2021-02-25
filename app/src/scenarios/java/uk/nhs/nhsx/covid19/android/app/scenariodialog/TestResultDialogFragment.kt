package uk.nhs.nhsx.covid19.android.app.scenariodialog

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.CompoundButton
import android.widget.EditText
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.SwitchCompat
import kotlinx.android.synthetic.scenarios.dialog_test_result.view.configuration
import kotlinx.android.synthetic.scenarios.dialog_test_result.view.remainingDaysInIsolation
import kotlinx.android.synthetic.scenarios.dialog_test_result.view.optionalContainer
import kotlinx.android.synthetic.scenarios.dialog_test_result.view.useMock
import uk.nhs.nhsx.covid19.android.app.R.layout
import uk.nhs.nhsx.covid19.android.app.ScenariosDebugAdapter
import uk.nhs.nhsx.covid19.android.app.di.viewmodel.MockTestResultViewModel
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState

class TestResultDialogFragment(positiveAction: (() -> Unit)) :
    ScenarioDialogFragment(positiveAction) {
    override val title: String = "Test Result Config"
    override val layoutId = layout.dialog_test_result

    private lateinit var useMockSwitch: SwitchCompat
    private lateinit var configurationSpinner: AppCompatSpinner
    private lateinit var remainingDaysInIsolationEditText: EditText
    private lateinit var optionalViews: ViewGroup

    val viewStateOptions = TestResultViewState::class.nestedClasses
        .filter { it != TestResultViewState.ButtonAction::class }
        .map { it.simpleName ?: "" }

    override fun setUp(view: View) = with(view) {
        useMockSwitch = useMock
        configurationSpinner = configuration
        remainingDaysInIsolationEditText = remainingDaysInIsolation
        optionalViews = optionalContainer

        setUpUseMock()
        setUpConfiguration()
        setUpRemainingDaysInIsolation()
    }

    private fun setUpUseMock() = with(useMockSwitch) {
        setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
            MockTestResultViewModel.currentOptions =
                MockTestResultViewModel.currentOptions.copy(useMock = checked)
            optionalViews.visibility = checked.toViewState()
        }
        isChecked = MockTestResultViewModel.currentOptions.useMock
    }

    private fun setUpRemainingDaysInIsolation() = with(remainingDaysInIsolationEditText) {
        setText("${MockTestResultViewModel.currentOptions.remainingDaysInIsolation}")

        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) =
                Unit

            override fun afterTextChanged(s: Editable?) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                MockTestResultViewModel.currentOptions.copy(
                    remainingDaysInIsolation =
                        try {
                            s!!.toString().toInt()
                        } catch (e: NumberFormatException) {
                            MockTestResultViewModel.currentOptions.remainingDaysInIsolation
                        }
                ).also { MockTestResultViewModel.currentOptions = it }
            }
        })
    }

    private fun setUpConfiguration() = with(configurationSpinner) {
        adapter = ScenariosDebugAdapter(
            context, viewStateOptions
        )
        setSelection(viewStateOptions.indexOf(MockTestResultViewModel.currentOptions.viewState.javaClass.simpleName))

        onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                MockTestResultViewModel.currentOptions =
                    MockTestResultViewModel.currentOptions.copy(
                        viewState = TestResultViewState::class.nestedClasses
                            .firstOrNull { it.simpleName == viewStateOptions[position] }?.objectInstance as TestResultViewState
                    )
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }
}
