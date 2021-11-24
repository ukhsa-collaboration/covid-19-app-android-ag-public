package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import android.widget.TextView
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.drawable
import uk.nhs.nhsx.covid19.android.app.databinding.ViewEnterCodeBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.getString
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
            binding.enterCodeErrorText.text = errorText
        }

    private val binding = ViewEnterCodeBinding.inflate(LayoutInflater.from(context), this)

    init {
        applyAttributes(context, attrs)
    }

    fun handleError() {
        showErrorState()
    }

    fun handleProgress() {
        showProgressState()
    }

    fun resetState() = with(binding) {
        enterCodeErrorIndicator.invisible()
        enterCodeErrorText.gone()
        enterCodeEditText.setBackgroundResource(drawable.edit_text_background)
        enterCodeProgress.gone()
    }

    fun setEnterCodeEditText(content: String?, bufferType: TextView.BufferType) {
        binding.enterCodeEditText.setText(content, bufferType)
    }

    fun addEnterCodeEditTextWatcher(watcher: TextWatcher) {
        binding.enterCodeEditText.addTextChangedListener(watcher)
    }

    private fun showProgressState() = with(binding) {
        enterCodeErrorIndicator.invisible()
        enterCodeErrorText.gone()
        enterCodeEditText.setBackgroundResource(drawable.edit_text_background)
        enterCodeProgress.visible()
    }

    private fun showErrorState() = with(binding) {
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
            val attrTitle = getString(context, R.styleable.EnterCodeView_title)
            val attrExample = getString(context, R.styleable.EnterCodeView_example)
            errorText = getString(context, R.styleable.EnterCodeView_errorText)

            binding.enterCodeTitle.text = attrTitle
            binding.enterCodeText.text = attrExample

            recycle()
        }
    }
}
