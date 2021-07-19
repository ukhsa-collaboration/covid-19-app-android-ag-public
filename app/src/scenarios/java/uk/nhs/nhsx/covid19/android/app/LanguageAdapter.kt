package uk.nhs.nhsx.covid19.android.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class LanguageAdapter(
    context: Context,
    languages: List<SupportedLanguageItem>
) : ArrayAdapter<SupportedLanguageItem>(context, android.R.layout.simple_spinner_item, languages) {

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup) =
        getLanguageView(position, convertView, parent)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
        getLanguageView(position, convertView, parent)

    private fun getLanguageView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView
            ?: LayoutInflater.from(context).inflate(R.layout.item_language, parent, false)

        val nameTextView = view.findViewById<TextView>(R.id.languageName)
        val languageNameResId: Int = getItem(position)?.nameResId ?: R.string.empty
        nameTextView.text = context.getString(languageNameResId)

        return view
    }
}
