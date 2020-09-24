package uk.nhs.nhsx.covid19.android.app.widgets

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.AttributeSet
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.util.DistrictAreaStringProvider
import uk.nhs.nhsx.covid19.android.app.util.openUrl
import uk.nhs.nhsx.covid19.android.app.util.setUpOpensInBrowserWarning
import javax.inject.Inject

class LinkTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : UnderlinedTextView(context, attrs, defStyleAttr) {

    @Inject
    lateinit var districtAreaStringProvider: DistrictAreaStringProvider

    private var linkUrl: Int = 0

    init {
        context.applicationContext.appComponent.inject(this)
        applyAttributes(context, attrs)
        setUpOpensInBrowserWarning()
        setOnClickListener {
            getActivity()?.openUrl(districtAreaStringProvider.provide(linkUrl))
        }
    }

    private fun applyAttributes(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.LinkTextView,
            0,
            0
        ).apply {
            linkUrl = getResourceId(R.styleable.LinkTextView_linkUrl, 0)

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
}
