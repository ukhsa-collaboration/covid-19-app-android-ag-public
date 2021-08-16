package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.parseAsHtml
import uk.nhs.nhsx.covid19.android.app.util.viewutils.overriddenResources

open class UnderlinedTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        convertToUnderlinedText()
    }

    fun setDisplayText(@StringRes displayTextResId: Int) {
        text = context.overriddenResources.getString(displayTextResId)
        convertToUnderlinedText()
    }

    private fun convertToUnderlinedText() {
        text = ("<u>$text</u>").parseAsHtml()
    }
}
