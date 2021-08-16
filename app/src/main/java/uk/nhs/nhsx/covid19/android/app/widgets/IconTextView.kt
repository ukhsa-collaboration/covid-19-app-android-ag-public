package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.android.synthetic.main.view_icon_text.view.iconViewImage
import kotlinx.android.synthetic.main.view_icon_text.view.iconViewText
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.util.viewutils.dpToPx

class IconTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    @StringRes var stringResId: Int = -1
        private set

    @DrawableRes var drawableResId: Int = -1
        private set

    constructor(context: Context, @StringRes stringResId: Int, @DrawableRes drawableResId: Int) : this(context) {
        this.stringResId = stringResId
        this.drawableResId = drawableResId

        View.inflate(context, R.layout.view_icon_text, this)
        iconViewText.text = context.getString(stringResId)
        iconViewImage.setImageResource(drawableResId)
        configureLayoutParams()
    }

    private fun configureLayoutParams() {
        val layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        val verticalMargin = VERTICAL_MARGIN_IN_DP.dpToPx.toInt()
        layoutParams.setMargins(0, verticalMargin, 0, verticalMargin)
        this.layoutParams = layoutParams
    }

    companion object {
        private const val VERTICAL_MARGIN_IN_DP = 12
    }
}
