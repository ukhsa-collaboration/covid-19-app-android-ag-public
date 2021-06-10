package uk.nhs.nhsx.covid19.android.app.status.testinghub

import android.R.attr
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_testing_hub_item.view.testingHubItemDescription
import kotlinx.android.synthetic.main.view_testing_hub_item.view.testingHubItemNavigationIndicator
import kotlinx.android.synthetic.main.view_testing_hub_item.view.testingHubItemTitle
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpButtonType
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpLinkTypeWithBrowserWarning

class TestingHubItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var isExternalLink: Boolean = false

    init {
        initializeViews()
        applyAttributes(context, attrs)
        setUpAccessibility()
    }

    private fun setUpAccessibility() {
        val accessibilityText = "${testingHubItemTitle.text}. ${testingHubItemDescription.text}"
        if (isExternalLink) {
            setUpLinkTypeWithBrowserWarning(accessibilityText)
        } else {
            setUpButtonType(accessibilityText)
        }
    }

    private fun initializeViews() {
        View.inflate(context, R.layout.view_testing_hub_item, this)
        configureLayout()
    }

    private fun applyAttributes(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.TestingHubItemView,
            0,
            0
        ).apply {
            val title = getString(R.styleable.TestingHubItemView_testingHubItemTitle)
            val description = getString(R.styleable.TestingHubItemView_testingHubItemDescription)
            isExternalLink = getBoolean(R.styleable.TestingHubItemView_testingHubItemIsExternalLink, false)

            testingHubItemTitle.text = title
            testingHubItemDescription.text = description

            val navigationIndicator =
                if (isExternalLink) R.drawable.ic_external_link else R.drawable.ic_chevron_right
            val navigationIndicatorDrawable = context.getDrawable(navigationIndicator)
            navigationIndicatorDrawable?.isAutoMirrored = true
            testingHubItemNavigationIndicator.setImageDrawable(navigationIndicatorDrawable)

            recycle()
        }
    }

    private fun configureLayout() {
        orientation = HORIZONTAL
        val paddingHorizontal = resources.getDimension(R.dimen.margin_horizontal).toInt()
        val paddingVertical = resources.getDimension(R.dimen.vertical_margin).toInt()
        setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
        setBackgroundResource(R.color.surface_background)
        setSelectableItemForeground()
    }

    private fun setSelectableItemForeground() {
        val outValue = TypedValue()
        context.theme.resolveAttribute(attr.selectableItemBackground, outValue, true)
        foreground = context.getDrawable(outValue.resourceId)
    }
}
