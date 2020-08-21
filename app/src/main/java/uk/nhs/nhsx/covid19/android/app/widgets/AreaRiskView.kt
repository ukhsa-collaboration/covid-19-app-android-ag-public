package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.util.AttributeSet
import androidx.core.text.parseAsHtml
import kotlinx.android.synthetic.main.view_status_option.view.statusOptionIcon
import kotlinx.android.synthetic.main.view_status_option.view.statusOptionText
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.HIGH
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.LOW
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.MEDIUM

class AreaRiskView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : StatusOptionView(context, attrs, defStyleAttr) {

    var text: String = ""
        set(value) {
            field = value
            statusOptionText.text = text
        }

    var areaRisk: String? = null
        set(value) {
            field = value
            setAreaRiskIndicator(areaRisk)
        }

    init {
        applyAttributes(context, attrs)
    }

    private fun applyAttributes(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.AreaRiskView,
            0,
            0
        ).apply {
            val attrText = getString(R.styleable.AreaRiskView_areaRiskText)?.parseAsHtml()
            val attrAreaRisk = getString(R.styleable.AreaRiskView_areaRisk)

            statusOptionText.text = attrText ?: ""
            statusOptionText.setPaddingRelative(0, 0, 0, 0)
            setAreaRiskIndicator(attrAreaRisk)

            recycle()
        }
    }

    private fun setAreaRiskIndicator(areaRisk: String?) {
        when (areaRisk) {
            LOW.name -> statusOptionIcon.setImageResource(R.drawable.ic_location_green)
            MEDIUM.name -> statusOptionIcon.setImageResource(R.drawable.ic_location_orange)
            HIGH.name -> statusOptionIcon.setImageResource(R.drawable.ic_location_red)
            else -> statusOptionIcon.setImageDrawable(null)
        }
    }
}
