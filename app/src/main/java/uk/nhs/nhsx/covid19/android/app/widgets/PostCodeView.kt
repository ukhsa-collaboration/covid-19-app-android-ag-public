package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_post_code.view.errorIndicatorLeft
import kotlinx.android.synthetic.main.view_post_code.view.errorInfoContainer
import kotlinx.android.synthetic.main.view_post_code.view.errorText
import kotlinx.android.synthetic.main.view_post_code.view.errorTextTitle
import kotlinx.android.synthetic.main.view_post_code.view.postCodeEditText
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible

class PostCodeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    val postCodeDistrict: String
        get() = postCodeEditText.text.toString()

    init {
        View.inflate(context, R.layout.view_post_code, this)
        configureLayout()

        addMaxLengthFilterWorkaround()
    }

    private fun addMaxLengthFilterWorkaround() {
        // See https://issuetracker.google.com/issues/141497575
        postCodeEditText.addTextChangedListener(MaxLengthTextWatcher(postCodeEditText))
    }

    fun showErrorState() {
        with(context) {
            updateErrorStateView(
                "${getString(R.string.post_code_invalid_title)}. ${
                getString(
                    R.string.valid_post_code_is_required
                )
                }",
                getString(R.string.post_code_invalid_title)
            )
        }
        errorText.text = context.getString(R.string.valid_post_code_is_required)
    }

    fun showPostCodeNotSupportedErrorState() {
        updateErrorStateView(
            "${context.getString(R.string.post_code_invalid_title)}. ${context.getString(R.string.postcode_not_supported)}",
            context.getString(R.string.post_code_invalid_title)
        )
        errorText.text = context.getString(R.string.postcode_not_supported)
    }

    private fun updateErrorStateView(
        announcementText: String,
        errorText: String
    ) {
        postCodeEditText.setBackgroundResource(R.drawable.edit_text_background_error)
        errorInfoContainer.announceForAccessibility(announcementText)
        errorInfoContainer.visible()
        errorIndicatorLeft.visible()
        errorTextTitle.text = errorText
    }

    private fun configureLayout() {
        orientation = VERTICAL
    }

    class MaxLengthTextWatcher(private val editText: EditText, private val maxLength: Int = 4) :
        TextWatcher {

        override fun afterTextChanged(s: Editable) {
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            editText.removeTextChangedListener(this)
            val string = s.toString()
            val postCodeDistrict = if (string.length > maxLength) {
                string.substring(0 until maxLength)
            } else {
                string
            }
            if (s.toString() != postCodeDistrict) {
                editText.setText(postCodeDistrict)
                try {
                    editText.setSelection(editText.text.length)
                } catch (e: IndexOutOfBoundsException) {
                    Timber.e(e)
                }
            }

            editText.addTextChangedListener(this)
        }
    }
}
