package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_enter_code.view.enterCodeEditText
import kotlinx.android.synthetic.main.view_enter_code.view.enterCodeErrorIndicator
import kotlinx.android.synthetic.main.view_enter_code.view.enterCodeErrorText
import kotlinx.android.synthetic.main.view_enter_code.view.enterCodeProgress
import kotlinx.android.synthetic.main.view_enter_code.view.enterCodeText
import kotlinx.android.synthetic.main.view_enter_code.view.enterCodeTitle
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.drawable
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.invisible
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible

class EnterCodeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    var errorText: String? = ""
        set(value) {
            field = value
            enterCodeErrorText.text = errorText
        }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_enter_code, this, true)

        applyAttributes(context, attrs)
    }

    fun handleError() {
        showErrorState()
    }

    fun handleProgress() {
        showProgressState()
    }

    fun resetState() {
        enterCodeErrorIndicator.invisible()
        enterCodeErrorText.gone()
        enterCodeEditText.setBackgroundResource(drawable.edit_text_background)
        enterCodeProgress.gone()
    }

    private fun showProgressState() {
        enterCodeErrorIndicator.invisible()
        enterCodeErrorText.gone()
        enterCodeEditText.setBackgroundResource(drawable.edit_text_background)
        enterCodeProgress.visible()
    }

    private fun showErrorState() {
        enterCodeErrorIndicator.visible()
        enterCodeErrorText.visible()
        enterCodeEditText.setBackgroundResource(drawable.edit_text_background_error)
        enterCodeProgress.gone()
        announceForAccessibility(enterCodeErrorText.text.toString())
        enterCodeErrorText.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
    }

    private fun applyAttributes(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.EnterCodeView,
            0,
            0
        ).apply {
            val attrTitle = getString(R.styleable.EnterCodeView_title)
            val attrExample = getString(R.styleable.EnterCodeView_example)
            errorText = getString(R.styleable.EnterCodeView_errorText)

            enterCodeTitle.text = attrTitle
            enterCodeText.text = attrExample

            recycle()
        }
    }
}
