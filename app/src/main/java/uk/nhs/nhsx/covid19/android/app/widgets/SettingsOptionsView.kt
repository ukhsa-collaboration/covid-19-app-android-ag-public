package uk.nhs.nhsx.covid19.android.app.widgets

import android.R.attr
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.view_settings.view.settingsChevron
import kotlinx.android.synthetic.main.view_settings.view.settingsItemSubtitle
import kotlinx.android.synthetic.main.view_settings.view.settingsItemTitle
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import uk.nhs.nhsx.covid19.android.app.util.viewutils.getString

class SettingsOptionsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    var title: String? = ""
        set(value) {
            field = value
            settingsItemTitle.text = value
        }

    var subtitle: String? = ""
        set(value) {
            field = value
            settingsItemSubtitle.text = value
            if (value.isNullOrBlank()) {
                settingsItemSubtitle.gone()
            } else {
                settingsItemSubtitle.visible()
            }
        }

    var showChevron: Boolean = true
        set(value) {
            field = value
            settingsChevron.isVisible = showChevron
        }

    init {
        initializeViews()
        applyAttributes(context, attrs)
    }

    private fun initializeViews() {
        View.inflate(context, R.layout.view_settings, this)
        configureLayout()
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
