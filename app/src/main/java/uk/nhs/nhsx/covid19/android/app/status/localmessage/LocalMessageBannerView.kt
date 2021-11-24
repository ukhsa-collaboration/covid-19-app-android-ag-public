package uk.nhs.nhsx.covid19.android.app.status.localmessage

import android.R.attr
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.LinearLayout
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.databinding.ViewLocalMessageBannerBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpButtonType

class LocalMessageBannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewLocalMessageBannerBinding =
        ViewLocalMessageBannerBinding.inflate(LayoutInflater.from(context), this)

    var title: String? = ""
        set(value) {
            field = value
            binding.bannerTitle.text = value
            setUpAccessibility()
        }

    init {
        initializeViews()
    }

    private fun setUpAccessibility() {
        val accessibilityText = "$title ${context.getString(R.string.local_message_banner_read_more)}"
        setUpButtonType(accessibilityText)
    }

    private fun initializeViews() {
        configureLayout()
    }

    private fun configureLayout() {
        orientation = HORIZONTAL
        val padding = resources.getDimension(R.dimen.local_message_banner_padding).toInt()
        setPadding(padding, padding, padding, padding)
        setBackgroundResource(R.color.amber)
        setSelectableItemForeground()
    }

    private fun setSelectableItemForeground() {
        val outValue = TypedValue()
        context.theme.resolveAttribute(attr.selectableItemBackground, outValue, true)
        foreground = context.getDrawable(outValue.resourceId)
    }
}
