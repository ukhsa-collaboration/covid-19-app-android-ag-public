package uk.nhs.nhsx.covid19.android.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.R.layout

class LanguageAdapter(
    context: Context,
    languages: List<SupportedLanguage>
) : ArrayAdapter<SupportedLanguage>(context, android.R.layout.simple_spinner_item, languages) {

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup) =
        getLanguageView(position, convertView, parent)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
        getLanguageView(position, convertView, parent)

    private fun getLanguageView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView
            ?: LayoutInflater.from(context).inflate(layout.item_language, parent, false)

        val nameTextView = view.findViewById<TextView>(id.languageName)
        val language: Int = getItem(position)?.languageName ?: R.string.empty
        nameTextView.text = context.getString(language)

        return view
    }
}
