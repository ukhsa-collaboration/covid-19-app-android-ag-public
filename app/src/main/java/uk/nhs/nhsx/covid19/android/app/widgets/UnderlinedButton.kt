package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.util.AttributeSet
import androidx.core.text.parseAsHtml
import com.google.android.material.button.MaterialButton
import uk.nhs.nhsx.covid19.android.app.R

class UnderlinedButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.style.LinkText
) : MaterialButton(context, attrs, defStyleAttr) {

    init {
        text = ("<u>$text</u>").parseAsHtml()
    }
}
