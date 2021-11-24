package uk.nhs.nhsx.covid19.android.app.settings.languages

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uk.nhs.nhsx.covid19.android.app.databinding.ItemSettingsLanguageBinding
import uk.nhs.nhsx.covid19.android.app.settings.languages.LanguagesViewAdapter.LanguagesViewHolder
import uk.nhs.nhsx.covid19.android.app.settings.languages.LanguagesViewModel.Language
import uk.nhs.nhsx.covid19.android.app.util.viewutils.mirrorSystemLayoutDirection
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener

class LanguagesViewAdapter(private val selectionListener: (Language) -> Unit) :
    ListAdapter<Language, LanguagesViewHolder>(ItemCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = LanguagesViewHolder(
        ItemSettingsLanguageBinding.inflate(LayoutInflater.from(parent.context))
    )

    override fun onBindViewHolder(holder: LanguagesViewHolder, position: Int) {
        holder.bind(getItem(position), selectionListener)
    }

    class LanguagesViewHolder(private val itemBinding: ItemSettingsLanguageBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(language: Language, selectionListener: (Language) -> Unit) = with(itemBinding) {
            languageNativeName.text = language.nativeName
            languageTranslatedName.text = language.translatedName
            languageRadio.isChecked = language.isSelected
            languageRadio.mirrorSystemLayoutDirection()

            itemView.setOnSingleClickListener {
                selectionListener(language)
            }
        }
    }

    class ItemCallback : DiffUtil.ItemCallback<Language>() {
        override fun areItemsTheSame(oldItem: Language, newItem: Language) =
            oldItem.code == newItem.code

        override fun areContentsTheSame(oldItem: Language, newItem: Language) =
            oldItem == newItem
    }
}
