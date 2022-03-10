package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import androidx.annotation.LayoutRes
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.core.view.isVisible
import kotlinx.parcelize.Parcelize
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.databinding.ViewAccordionBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.getString
import uk.nhs.nhsx.covid19.android.app.util.viewutils.overriddenResources
import uk.nhs.nhsx.covid19.android.app.widgets.AccordionButtonView.AccordionIconType.CHEVRON
import uk.nhs.nhsx.covid19.android.app.widgets.AccordionButtonView.AccordionIconType.PLUS_MINUS

class AccordionButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var isExpanded: Boolean = DEFAULT_STATE
        set(value) {
            field = value
            updateContentVisibility()
        }

    private var iconType: AccordionIconType = PLUS_MINUS
        set(value) {
            field = value
            updateContentVisibility()
        }

    private val accessibleView: View by lazy { binding.accordionTitle }

    var title: String? = null
        get() = binding.titleTextView.text.toString()
        set(value) {
            field = value
            binding.titleTextView.text = field
        }

    @LayoutRes
    var content: Int = -1
        set(value) {
            field = value
            binding.accordionContent.removeAllViews()
            LayoutInflater.from(context).inflate(field, binding.accordionContent, true)
        }

    private val expandIcon: Int
        get() = when (iconType) {
            PLUS_MINUS -> R.drawable.ic_accordion_expand
            CHEVRON -> R.drawable.ic_accordion_expand_chevron
        }

    private val collapseIcon: Int
        get() = when (iconType) {
            PLUS_MINUS -> R.drawable.ic_accordion_collapse
            CHEVRON -> R.drawable.ic_accordion_collapse_chevron
        }

    private val binding = ViewAccordionBinding.inflate(LayoutInflater.from(context), this)

    init {
        initializeViews()
        applyAttributes(context, attrs)
        setUpAccessibility()
    }

    private fun initializeViews() {
        configureLayout()
    }

    private fun applyAttributes(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.AccordionButtonView,
            0,
            0
        ).apply {
            title = getString(context, R.styleable.AccordionButtonView_accordionTitle)

            val contentRef = getResourceId(R.styleable.AccordionButtonView_accordionContent, -1)
            if (contentRef != -1) {
                LayoutInflater.from(context).inflate(contentRef, binding.accordionContent, true)
            }
            isExpanded = getBoolean(R.styleable.AccordionButtonView_accordionExpanded, DEFAULT_STATE)

            val iconTypeInt = getInteger(R.styleable.AccordionButtonView_accordionIconType, 1)
            iconType = AccordionIconType.from(iconTypeInt)

            recycle()
        }
    }

    private fun updateContentVisibility() = with(binding) {
        if (isExpanded) {
            titleViewIcon.setImageResource(collapseIcon)
        } else {
            titleViewIcon.setImageResource(expandIcon)
        }

        accordionContent.isVisible = isExpanded
    }

    private fun configureLayout() = with(binding) {
        /* ktlint-disable */
        accordionTitle.setOnClickListener {
            isExpanded = !isExpanded
            announceAccordionTitle()
        }
        /* ktlint-enable */
        orientation = VERTICAL
    }

    private fun setUpAccessibility() {
        ViewCompat.setAccessibilityDelegate(
            accessibleView,
            object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfoCompat
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    val roleDescription =
                        if (isExpanded) R.string.accessibility_accordion_role_description_expanded else R.string.accessibility_accordion_role_description_collapsed

                    info.contentDescription =
                        context.overriddenResources.getString(roleDescription).format(binding.titleTextView.text)
                    info.isHeading = true
                    info.addAction(
                        AccessibilityActionCompat(
                            AccessibilityNodeInfoCompat.ACTION_CLICK,
                            context.overriddenResources.getString(
                                if (isExpanded) {
                                    R.string.accessibility_announcement_accordion_collapse_action
                                } else {
                                    R.string.accessibility_announcement_accordion_expand_action
                                }
                            )
                        )
                    )
                }
            }
        )
    }

    private fun announceAccordionTitle() {
        accessibleView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED)
    }

    @Parcelize
    class AccordionState(val superSavedState: Parcelable?, val isExpanded: Boolean) :
        View.BaseSavedState(superSavedState), Parcelable

    override fun onSaveInstanceState(): Parcelable? =
        AccordionState(super.onSaveInstanceState(), isExpanded)

    override fun onRestoreInstanceState(state: Parcelable?) {
        val savedState = state as? AccordionState
        super.onRestoreInstanceState(state)

        isExpanded = savedState?.isExpanded ?: DEFAULT_STATE
    }

    private enum class AccordionIconType {
        PLUS_MINUS,
        CHEVRON;

        companion object {
            fun from(i: Int): AccordionIconType {
                return when (i) {
                    1 -> PLUS_MINUS
                    2 -> CHEVRON
                    else -> throw IllegalArgumentException("This value is not supported for AccordionIconType: $i")
                }
            }
        }
    }

    companion object {
        const val DEFAULT_STATE = false
    }
}
