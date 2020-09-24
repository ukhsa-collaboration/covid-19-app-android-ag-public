package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_enter_code.view.enterCodeEditText
import kotlinx.android.synthetic.main.view_enter_code.view.enterCodeErrorIndicator
import kotlinx.android.synthetic.main.view_enter_code.view.enterCodeErrorText
import kotlinx.android.synthetic.main.view_enter_code.view.enterCodeProgress
import kotlinx.android.synthetic.main.view_enter_code.view.enterCodeText
import kotlinx.android.synthetic.main.view_enter_code.view.enterCodeTitle
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.drawable
import uk.nhs.nhsx.covid19.android.app.util.CodeInputTextWatcher
import uk.nhs.nhsx.covid19.android.app.util.gone
import uk.nhs.nhsx.covid19.android.app.util.invisible
import uk.nhs.nhsx.covid19.android.app.util.visible

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

    private var textWatcher: CodeInputTextWatcher

    init {
        LayoutInflater.from(context).inflate(R.layout.view_enter_code, this, true)

        textWatcher = CodeInputTextWatcher(enterCodeEditText)

        enterCodeEditText.addTextChangedListener(textWatcher)

        applyAttributes(context, attrs)
    }

    fun handleError() {
        showErrorState()
    }

    fun handleProgress() {
        showNormalState()
    }

    private fun showNormalState() {
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
            val codeInputRegex = getString(R.styleable.EnterCodeView_codeInputRegex)

            enterCodeTitle.text = attrTitle
            enterCodeText.text = attrExample
            codeInputRegex?.let {
                textWatcher.codeInputRegex = it.toRegex()
            }

            recycle()
        }
    }
}
