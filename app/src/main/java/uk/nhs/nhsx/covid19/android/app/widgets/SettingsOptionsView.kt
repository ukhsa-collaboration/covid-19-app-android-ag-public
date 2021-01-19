package uk.nhs.nhsx.covid19.android.app.widgets

import android.R.attr
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.RelativeLayout
import kotlinx.android.synthetic.main.view_settings.view.settingsItemSubtitle
import kotlinx.android.synthetic.main.view_settings.view.settingsItemTitle
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.util.viewutils.dpToPx

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
        minimumHeight = 48.dpToPx.toInt()
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
            title = getString(R.styleable.SettingsOptionsView_settingName)
            subtitle = getString(R.styleable.SettingsOptionsView_settingValue)
            recycle()
        }
    }

    private fun setSelectableItemForeground() {
        val outValue = TypedValue()
        context.theme.resolveAttribute(attr.selectableItemBackground, outValue, true)
    }
}
