package uk.nhs.nhsx.covid19.android.app.scenariodialog

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.CompoundButton
import uk.nhs.nhsx.covid19.android.app.ScenariosDebugAdapter
import uk.nhs.nhsx.covid19.android.app.databinding.DialogTestResultBinding
import uk.nhs.nhsx.covid19.android.app.di.viewmodel.MockTestResultViewModel
import uk.nhs.nhsx.covid19.android.app.testordering.BookTestOption
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState

class TestResultDialogFragment(positiveAction: (() -> Unit)) :
    ScenarioDialogFragment<DialogTestResultBinding>(positiveAction) {
    override val title: String = "Test Result Config"

    val viewStateOptions = TestResultViewState.values()
        .map { it.name }

    override fun setupBinding(inflater: LayoutInflater): DialogTestResultBinding =
        DialogTestResultBinding.inflate(inflater)

    override fun setupView() = with(binding) {
        setUpUseMock()
        setUpConfiguration()
        setUpRemainingDaysInIsolation()
        setUpShareKeys()
        setUpBookTestOptions()
    }

    private fun setUpUseMock() = with(binding) {
        useMock.setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
            MockTestResultViewModel.currentOptions =
                MockTestResultViewModel.currentOptions.copy(useMock = checked)
            optionalContainer.visibility = checked.toViewState()
        }
        useMock.isChecked = MockTestResultViewModel.currentOptions.useMock
    }

    private fun setUpRemainingDaysInIsolation() = with(binding.remainingDaysInIsolation) {
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

    private fun setUpConfiguration() = with(binding.configuration) {
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
                        viewState = TestResultViewState.values()
                            .first { it.name == viewStateOptions[position] }
                    )
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun setUpBookTestOptions() = with(binding.orderTest) {
        val options = BookTestOption.values()
        adapter = ScenariosDebugAdapter(
            context, options.map { it.name }
        )
        setSelection(options.indexOf(MockTestResultViewModel.currentOptions.actions.suggestBookTest))

        onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                MockTestResultViewModel.currentOptions =
                    MockTestResultViewModel.currentOptions.copy(
                        actions = MockTestResultViewModel.currentOptions.actions.copy(
                            suggestBookTest = options[position]
                        )
                    )
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun setUpShareKeys() = with(binding) {
        shareKeys.isChecked = MockTestResultViewModel.currentOptions.actions.shouldAllowKeySubmission
        shareKeys.setOnCheckedChangeListener { _, isChecked ->
            MockTestResultViewModel.currentOptions =
                MockTestResultViewModel.currentOptions.copy(
                    actions = MockTestResultViewModel.currentOptions.actions.copy(
                        shouldAllowKeySubmission =
                        isChecked
                    )
                )
        }
    }
}
