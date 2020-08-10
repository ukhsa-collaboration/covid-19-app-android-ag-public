package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.text.parseAsHtml
import kotlinx.android.synthetic.main.view_area_risk.view.areaRiskIndicator
import kotlinx.android.synthetic.main.view_area_risk.view.areaRiskMoreInfo
import kotlinx.android.synthetic.main.view_area_risk.view.areaRiskText
import uk.nhs.nhsx.covid19.android.app.R

class AreaRiskView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var listener: (() -> Unit)? = null
    var text: String = ""
        set(value) {
            field = value
            areaRiskText.text = text
        }

    var colorResId: Int = 0
        set(value) {
            field = value
            areaRiskIndicator.setBackgroundColor(context.getColor(colorResId))
        }

    init {
        initializeViews()
        applyAttributes(context, attrs)
    }

    override fun announceForAccessibility(error: CharSequence) {
        super.announceForAccessibility(context.getString(R.string.view_area_risk_accessibility))
    }

    private fun initializeViews() {
        LayoutInflater.from(context).inflate(R.layout.view_area_risk, this, true)
        configureLayout()
        areaRiskMoreInfo.setOnClickListener {
            listener?.invoke()
        }
    }

    fun setOnViewMoreClickListener(listener: () -> Unit) {
        this.listener = listener
    }

    private fun applyAttributes(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.AreaRiskView,
            0,
            0
        ).apply {
            val attrText = getString(R.styleable.AreaRiskView_areaRiskText)?.parseAsHtml()
            val attrColor = getColor(R.styleable.AreaRiskView_areaRiskColor, 0)

            areaRiskText.text = attrText ?: ""
            areaRiskIndicator.setBackgroundColor(attrColor)

            recycle()
        }
    }

    private fun configureLayout() {
        setBackgroundColor(context.getColor(R.color.surface_background))
        gravity = Gravity.CENTER_VERTICAL
    }
}
