package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.android.synthetic.main.view_logo.view.daLogo
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostalDistrictProvider
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpAccessibilityHeading
import javax.inject.Inject

class LogoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var isAccessibilityHeadingCompat = filterTouchesWhenObscured

    @Inject
    lateinit var postalDistrictProvider: PostalDistrictProvider

    init {
        context.applicationContext.appComponent.inject(this)
        View.inflate(context, R.layout.view_logo, this)
        getAttributes(attrs)
    }

    private fun getAttributes(attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.LogoView, 0, 0).apply {
            isAccessibilityHeadingCompat =
                getBoolean(R.styleable.LogoView_isAccessibilityHeadingCompat, false)
            recycle()
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == View.VISIBLE) {
            setLogo()
        }
    }

    private fun setLogo() {
        val logoWithDescription =
            LogoWithDescription.forDistrict(postalDistrictProvider.toPostalDistrict())
        daLogo.setImageResource(logoWithDescription.logoImage)

        val description = logoWithDescription.description
        if (isAccessibilityHeadingCompat && description != null) {
            daLogo.contentDescription = context.getString(description)
            daLogo.isFocusable = true
            setUpAccessibilityHeading()
        } else {
            daLogo.contentDescription = null
            daLogo.isFocusable = false
        }
    }
}

enum class LogoWithDescription(@DrawableRes val logoImage: Int, @StringRes val description: Int?) {
    COVID_19(R.drawable.ic_nhs_covid_logo, null),
    ENGLAND_TEST_TRACE(R.drawable.nhs_logo_on_transparent, R.string.logo_nhs_england_description),
    WALES_TEST_TRACE(R.drawable.wales_test_trace_logo, R.string.logo_nhs_wales_description);

    companion object {
        fun forDistrict(district: PostCodeDistrict?): LogoWithDescription {
            return when (district) {
                ENGLAND -> ENGLAND_TEST_TRACE
                WALES -> WALES_TEST_TRACE
                else -> COVID_19
            }
        }
    }
}
