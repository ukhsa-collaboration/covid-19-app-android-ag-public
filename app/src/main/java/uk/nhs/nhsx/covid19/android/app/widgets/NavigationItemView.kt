package uk.nhs.nhsx.covid19.android.app.widgets

import android.R.attr
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_navigation_item.view.navigationItemDescription
import kotlinx.android.synthetic.main.view_navigation_item.view.navigationItemIndicator
import kotlinx.android.synthetic.main.view_navigation_item.view.navigationItemTitle
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.util.viewutils.getString
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpButtonType
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpLinkTypeWithBrowserWarning

class NavigationItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var attributes: NavigationItemAttributes = NavigationItemAttributes()
        set(value) {
            field = value
            with(attributes) {
                setNavigationIndicator(isExternalLink)
                navigationItemTitle.text = title
                navigationItemDescription.text = description
            }
            updateAccessibilityAnnouncement()
        }

    init {
        initializeViews()
        applyAttributes(context, attrs)
        updateAccessibilityAnnouncement()
    }

    private fun updateAccessibilityAnnouncement() {
        val accessibilityText = "${navigationItemTitle.text}. ${navigationItemDescription.text}"
        if (attributes.isExternalLink) {
            setUpLinkTypeWithBrowserWarning(accessibilityText)
        } else {
            setUpButtonType(accessibilityText)
        }
    }

    private fun initializeViews() {
        View.inflate(context, R.layout.view_navigation_item, this)
        configureLayout()
    }

    private fun applyAttributes(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.NavigationItemView,
            0,
            0
        ).apply {
            attributes = NavigationItemAttributes(
                isExternalLink = getBoolean(R.styleable.NavigationItemView_navigationItemIsExternalLink, false),
                title = getString(context, R.styleable.NavigationItemView_navigationItemTitle),
                description = getString(context, R.styleable.NavigationItemView_navigationItemDescription)
            )
            recycle()
        }
    }

    private fun setNavigationIndicator(isExternalLink: Boolean) {
        val navigationIndicator = if (isExternalLink) R.drawable.ic_external_link else R.drawable.ic_chevron_right
        val navigationIndicatorDrawable = context.getDrawable(navigationIndicator)
        navigationIndicatorDrawable?.isAutoMirrored = true
        navigationItemIndicator.setImageDrawable(navigationIndicatorDrawable)
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

    data class NavigationItemAttributes(
        val isExternalLink: Boolean = false,
        val title: String = "",
        val description: String = ""
    )
}
