package uk.nhs.nhsx.covid19.android.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import uk.nhs.covid19.config.EnvironmentConfiguration
import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.R.layout

class EnvironmentAdapter(context: Context, environments: List<EnvironmentConfiguration>) :
    ArrayAdapter<EnvironmentConfiguration>(context, android.R.layout.simple_spinner_item, environments) {
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(layout.item_environment, parent, false)

        val nameTextView = view.findViewById<TextView>(id.environmentName)
        val urlTextView = view.findViewById<TextView>(id.environmentUrl)
        nameTextView.text = getItem(position)?.name ?: ""
        urlTextView.text = getItem(position)?.distributedRemoteBaseUrl ?: ""
        return view
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(layout.item_environment_title, parent, false)

        val nameTextView = view.findViewById<TextView>(id.environmentName)
        nameTextView.text = getItem(position)?.name ?: ""
        return view
    }
}
