package uk.nhs.nhsx.covid19.android.app.scenariodialog

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType
import uk.nhs.nhsx.covid19.android.app.ScenariosDebugAdapter
import uk.nhs.nhsx.covid19.android.app.databinding.DialogMockApiBinding
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule

class MockApiDialogFragment(positiveAction: (() -> Unit)) :
    ScenarioDialogFragment<DialogMockApiBinding>(positiveAction) {

    override val title: String = "Mock API Config"

    override fun setupBinding(inflater: LayoutInflater): DialogMockApiBinding =
        DialogMockApiBinding.inflate(layoutInflater)

    override fun setupView() {
        setUpDelayText()
        setUpResponseType()
    }

    private fun updateMockDelay() {
        MockApiModule.behaviour.delayMillis = try {
            binding.mockSettingsDelay.text.toString().toLong()
        } catch (e: NumberFormatException) {
            MockApiModule.behaviour.delayMillis
        }
    }

    private fun setUpDelayText() = with(binding.mockSettingsDelay) {
        setText("${MockApiModule.behaviour.delayMillis}")
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) =
                Unit

            override fun afterTextChanged(s: Editable?) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) =
                updateMockDelay()
        })
    }

    private fun setUpResponseType() = with(binding.mockSettingsResponseType) {
        adapter = ScenariosDebugAdapter(
            context,
            MockApiResponseType.values().toList()
        ).apply {
            setSelection(positionOf(MockApiModule.behaviour.responseType))
        }
        onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                MockApiModule.behaviour.responseType = MockApiResponseType.values()[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }
}
