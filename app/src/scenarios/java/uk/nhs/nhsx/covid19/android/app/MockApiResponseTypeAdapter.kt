package uk.nhs.nhsx.covid19.android.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class MockApiResponseTypeAdapter(context: Context) :
    ArrayAdapter<MockApiResponseType>(context, android.R.layout.simple_spinner_item, MockApiResponseType.values()) {

    fun positionOf(responseType: MockApiResponseType) = MockApiResponseType.values().indexOf(responseType)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_spinner_item, parent, false)

        val nameTextView = view.findViewById<TextView>(android.R.id.text1)
        nameTextView.text = MockApiResponseType.values()[position].name
        return view
    }
}
