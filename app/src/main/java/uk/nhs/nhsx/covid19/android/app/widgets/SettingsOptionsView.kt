package uk.nhs.nhsx.covid19.android.app.widgets

import android.R.attr
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.databinding.ViewSettingsBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.getString

class SettingsOptionsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private val binding = ViewSettingsBinding.inflate(LayoutInflater.from(context), this)

    var title: String? = ""
        set(value) {
            field = value
            binding.settingsItemTitle.text = value
        }

    var subtitle: String? = ""
        set(value) {
            field = value
            with(binding) {
                settingsItemSubtitle.text = value
                settingsItemSubtitle.isVisible = !value.isNullOrBlank()
            }
        }

    var showChevron: Boolean = true
        set(value) {
            field = value
            binding.settingsChevron.isVisible = showChevron
        }

    init {
        configureLayout()
        applyAttributes(context, attrs)
    }

    private fun configureLayout() {
        setBackgroundResource(R.drawable.settings_option_background)
        setSelectableItemForeground()
    }

    private fun applyAttributes(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SettingsOptionsView,
            0,
            0
        ).apply {
            title = getString(context, R.styleable.SettingsOptionsView_settingName)
            subtitle = getString(context, R.styleable.SettingsOptionsView_settingValue)
            showChevron = getBoolean(R.styleable.SettingsOptionsView_showChevron, true)
            recycle()
        }
    }

    private fun setSelectableItemForeground() {
        val outValue = TypedValue()
        context.theme.resolveAttribute(attr.selectableItemBackground, outValue, true)
    }
}
