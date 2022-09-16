package uk.nhs.nhsx.covid19.android.app.widgets

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.util.viewutils.openUrl
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpOpensInBrowserWarning

class NonDistrictLinkTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : UnderlinedTextView(context, attrs, defStyleAttr) {

    var linkUrl: String? = null

    init {
        applyAttributes(context, attrs)
        setupStyling()
        setUpOpensInBrowserWarning()
        setOnSingleClickListener {
            linkUrl?.let {
                getActivity()?.openUrl(it)
            }
        }
    }

    fun setUrl(linkUrl: String) {
        this.linkUrl = linkUrl
    }

    private fun applyAttributes(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.LinkTextView,
            0,
            0
        ).apply {
            recycle()
        }
    }

    private fun getActivity(): Activity? {
        var context = context
        while (context is ContextWrapper) {
            if (context is Activity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }

    private fun setupStyling() {
        this.setTextAppearance(R.style.LinkText)
        this.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_link, 0)
        this.layoutParams =
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        this.compoundDrawablePadding = context.resources.getDimension(R.dimen.vertical_margin_small).toInt()
        this.setPadding(
            0,
            resources.getDimension(R.dimen.vertical_margin_small).toInt(),
            0,
            resources.getDimension(R.dimen.vertical_margin_small).toInt()
        )
    }
}
