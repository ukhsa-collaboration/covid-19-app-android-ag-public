package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.databinding.ViewErrorBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.dpToPx
import uk.nhs.nhsx.covid19.android.app.util.viewutils.getString

class ErrorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val binding = ViewErrorBinding.inflate(LayoutInflater.from(context), this)

    var errorTitle: String? = ""
        set(value) {
            field = value
            binding.errorTitleView.text = errorTitle
        }

    var errorDescription: String? = ""
        set(value) {
            field = value
            binding.errorDescriptionView.text = errorDescription
        }

    init {
        initializeViews()
        applyAttributes(context, attrs)
    }

    private fun initializeViews() {
        val newPadding = 16.dpToPx.toInt()
        setPadding(newPadding, newPadding, newPadding, newPadding)

        orientation = VERTICAL

        setBackgroundResource(R.drawable.error_background)

        isFocusableInTouchMode = true
        isFocusable = true
    }

    private fun applyAttributes(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.ErrorView, 0, 0)
            .apply {
                errorTitle = getString(context, R.styleable.ErrorView_error_title)
                errorDescription = getString(context, R.styleable.ErrorView_error_description)
                recycle()
            }
    }

    fun setFocus() {
        requestFocus()
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
    }
}
