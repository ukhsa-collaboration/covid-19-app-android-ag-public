package uk.nhs.nhsx.covid19.android.app.scenariodialog

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.EditText
import androidx.appcompat.widget.AppCompatSpinner
import kotlinx.android.synthetic.scenarios.dialog_mock_api.view.mockSettingsDelay
import kotlinx.android.synthetic.scenarios.dialog_mock_api.view.mockSettingsResponseType
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType
import uk.nhs.nhsx.covid19.android.app.R.layout
import uk.nhs.nhsx.covid19.android.app.ScenariosDebugAdapter
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule

class MockApiDialogFragment(positiveAction: (() -> Unit)) : ScenarioDialogFragment(positiveAction) {
    override val title: String = "Mock API Config"
    override val layoutId = layout.dialog_mock_api

    private lateinit var mockSettingsDelayText: EditText
    private lateinit var mockSettingsResponseTypeSpinner: AppCompatSpinner

    override fun setUp(view: View) = with(view) {
        mockSettingsDelayText = mockSettingsDelay
        mockSettingsResponseTypeSpinner = mockSettingsResponseType
        setUpDelayText()
        setUpResponseType()
    }

    private fun updateMockDelay() {
        MockApiModule.behaviour.delayMillis = try {
            mockSettingsDelayText.text.toString().toLong()
        } catch (e: NumberFormatException) {
            MockApiModule.behaviour.delayMillis
        }
    }

    private fun setUpDelayText() = with(mockSettingsDelayText) {
        setText("${MockApiModule.behaviour.delayMillis}")
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) =
                Unit

            override fun afterTextChanged(s: Editable?) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) =
                updateMockDelay()
        })
    }

    private fun setUpResponseType() = with(mockSettingsResponseTypeSpinner) {
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
