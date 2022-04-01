package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.databinding.ViewIconTextBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.dpToPx
import uk.nhs.nhsx.covid19.android.app.util.viewutils.getString

class IconTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private var _text: String = DEFAULT_TEXT,
    @DrawableRes private var _drawableResId: Int = DEFAULT_DRAWABLE_RES_ID
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewIconTextBinding.inflate(LayoutInflater.from(context), this)

    val text get() = _text

    val drawableResId get() = _drawableResId

    constructor(context: Context, @StringRes stringResId: Int, @DrawableRes drawableResId: Int) : this(
        context = context,
        _text = context.getString(stringResId),
        _drawableResId = drawableResId
    )

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.IconTextView, defStyleAttr, 0).apply {
            if (text == DEFAULT_TEXT) {
                _text = getString(context, R.styleable.IconTextView_text)
            }
            if (drawableResId == DEFAULT_DRAWABLE_RES_ID) {
                _drawableResId = getResourceId(R.styleable.IconTextView_drawable, DEFAULT_DRAWABLE_RES_ID)
            }
        }
        binding.iconViewText.text = text
        binding.iconViewImage.setImageResource(drawableResId)
        configureLayoutParams()
    }

    private fun configureLayoutParams() {
        val layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        val verticalMargin = VERTICAL_MARGIN_IN_DP.dpToPx.toInt()
        layoutParams.setMargins(0, verticalMargin, 0, verticalMargin)
        this.layoutParams = layoutParams
    }

    fun updateText(value: String) {
        _text = value
        binding.iconViewText.text = value
    }

    companion object {
        private const val VERTICAL_MARGIN_IN_DP = 12
        private const val DEFAULT_TEXT = ""
        private const val DEFAULT_DRAWABLE_RES_ID = -1
    }
}
