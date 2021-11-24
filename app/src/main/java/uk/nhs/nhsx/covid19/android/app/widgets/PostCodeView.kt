package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.accessibility.AccessibilityEvent
import android.widget.EditText
import android.widget.LinearLayout
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.databinding.ViewPostCodeBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.invisible
import uk.nhs.nhsx.covid19.android.app.util.viewutils.smoothScrollToAndThen
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible

class PostCodeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewPostCodeBinding.inflate(LayoutInflater.from(context), this, true)

    val postCodeDistrict: String
        get() = binding.postCodeEditText.text.toString()

    init {
        configureLayout()

        addMaxLengthFilterWorkaround()
    }

    private fun addMaxLengthFilterWorkaround() {
        // See https://issuetracker.google.com/issues/141497575
        binding.postCodeEditText.addTextChangedListener(MaxLengthTextWatcher(binding.postCodeEditText))
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
        binding.errorText.text = context.getString(R.string.valid_post_code_is_required)
    }

    fun showPostCodeNotSupportedErrorState() {
        updateErrorStateView(
            "${context.getString(R.string.post_code_invalid_title)}. ${context.getString(R.string.postcode_not_supported)}",
            context.getString(R.string.post_code_invalid_title)
        )
        binding.errorText.text = context.getString(R.string.postcode_not_supported)
    }

    private fun updateErrorStateView(
        announcementText: String,
        errorText: String
    ) = with(binding) {
        postCodeEditText.setBackgroundResource(R.drawable.edit_text_background_error)
        errorInfoContainer.smoothScrollToAndThen(0, 0) {
            errorInfoContainer.announceForAccessibility(announcementText)
            errorInfoContainer.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
        }
        errorInfoContainer.visible()
        errorIndicatorLeft.visible()
        errorTextTitle.text = errorText
    }

    fun resetErrorState() = with(binding) {
        errorInfoContainer.gone()
        postCodeEditText.setBackgroundResource(R.drawable.edit_text_background)
        errorIndicatorLeft.invisible()
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
