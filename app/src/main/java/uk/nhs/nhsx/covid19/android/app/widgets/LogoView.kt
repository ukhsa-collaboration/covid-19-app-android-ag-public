package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.databinding.ViewLogoBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpAccessibilityHeading
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class LogoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), CoroutineScope {

    lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private var isAccessibilityHeadingCompat = filterTouchesWhenObscured

    @Inject
    lateinit var localAuthorityPostCodeProvider: LocalAuthorityPostCodeProvider

    private val binding by lazy {
        ViewLogoBinding.inflate(LayoutInflater.from(context), this, true)
    }

    init {
        context.applicationContext.appComponent.inject(this)
        getAttributes(attrs)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        job = Job()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        job.cancel()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == View.VISIBLE) {
            setLogo()
        }
    }

    private fun setLogo() = launch {
        val postCodeDistrict = localAuthorityPostCodeProvider.getPostCodeDistrict()

        val logoWithDescription = LogoWithDescription.forDistrict(postCodeDistrict)

        with(binding) {
            daLogo.setImageResource(logoWithDescription.logoImage)

            val description = logoWithDescription.description
            if (isAccessibilityHeadingCompat && description != null) {
                contentDescription = context.getString(description)
                isFocusable = true
                setUpAccessibilityHeading()
            } else {
                contentDescription = null
                isFocusable = false
            }
        }
    }

    private fun getAttributes(attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.LogoView, 0, 0).apply {
            isAccessibilityHeadingCompat =
                getBoolean(R.styleable.LogoView_isAccessibilityHeadingCompat, false)
            recycle()
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
