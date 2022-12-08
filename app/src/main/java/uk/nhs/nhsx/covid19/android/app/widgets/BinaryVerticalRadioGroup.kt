package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.util.SparseArray
import android.view.LayoutInflater
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import kotlinx.parcelize.Parcelize
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.databinding.ViewBinaryVerticalRadioGruopBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.getString
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryVerticalRadioGroup.BinaryVerticalRadioGroupOption.OPTION_1
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryVerticalRadioGroup.BinaryVerticalRadioGroupOption.OPTION_2

class BinaryVerticalRadioGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {
    private val binding = ViewBinaryVerticalRadioGruopBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        initializeViews()
        applyAttributes(context, attrs)
    }

    var selectedOption: BinaryVerticalRadioGroupOption? = null
        set(value) {
            field = value
            updateButtonSelection()
        }

    private var listener: ((BinaryVerticalRadioGroupOption) -> Unit)? = null

    private fun createOnCheckedChangeListener(response: BinaryVerticalRadioGroupOption) =
        CompoundButton.OnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedOption = response
                listener?.invoke(response)
            }
        }

    private fun initializeViews() = with(binding) {
        binaryVerticalRadioButtonOption1.setOnCheckedChangeListener(createOnCheckedChangeListener(OPTION_1))
        binaryVerticalRadioButtonOption2.setOnCheckedChangeListener(createOnCheckedChangeListener(OPTION_2))
    }

    private fun applyAttributes(context: Context, attrs: AttributeSet?) = with(binding) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.BinaryRadioGroup, 0, 0)
            .apply {
                val option1Text = getString(context, R.styleable.BinaryRadioGroup_option1Text)
                binaryVerticalRadioButtonOption1.text = option1Text
                val option1ContentDesc = getString(context, R.styleable.BinaryRadioGroup_option1ContentDescription)
                binaryVerticalRadioButtonOption1.contentDescription = option1ContentDesc

                val option2ContentDesc = getString(context, R.styleable.BinaryRadioGroup_option2ContentDescription)
                binaryVerticalRadioButtonOption2.contentDescription = option2ContentDesc
                val option2Text = getString(context, R.styleable.BinaryRadioGroup_option2Text)
                binaryVerticalRadioButtonOption2.text = option2Text

                recycle()
            }
    }

    fun setOnValueChangedListener(listener: (BinaryVerticalRadioGroupOption) -> Unit) {
        this.listener = listener
    }

    private fun updateButtonSelection() = with(binding) {
        binaryVerticalRadioButtonOption1.setOnCheckedChangeListener(null)
        binaryVerticalRadioButtonOption1.isChecked = selectedOption == OPTION_1
        val radioBackground1 =
            if (selectedOption == OPTION_1) R.drawable.radio_selected_background else R.drawable.radio_not_selected_background
        (binaryVerticalRadioButtonOption1.parent as FrameLayout).background =
            ContextCompat.getDrawable(context, radioBackground1)

        binaryVerticalRadioButtonOption2.setOnCheckedChangeListener(null)
        binaryVerticalRadioButtonOption2.isChecked = selectedOption == OPTION_2
        val radioBackground2 =
            if (selectedOption == OPTION_2) R.drawable.radio_selected_background else R.drawable.radio_not_selected_background
        (binaryVerticalRadioButtonOption2.parent as FrameLayout).background =
            ContextCompat.getDrawable(context, radioBackground2)

        binaryVerticalRadioButtonOption1.setOnCheckedChangeListener(createOnCheckedChangeListener(OPTION_1))
        binaryVerticalRadioButtonOption2.setOnCheckedChangeListener(createOnCheckedChangeListener(OPTION_2))
    }

    override fun onSaveInstanceState(): Parcelable? {
        super.onSaveInstanceState()
        return selectedOption
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(BaseSavedState(state))
        selectedOption = state as? BinaryVerticalRadioGroupOption
    }

    override fun dispatchRestoreInstanceState(container: SparseArray<Parcelable>?) {
        super.dispatchThawSelfOnly(container)
    }

    override fun dispatchSaveInstanceState(container: SparseArray<Parcelable>?) {
        super.dispatchFreezeSelfOnly(container)
    }

    @Parcelize
    enum class BinaryVerticalRadioGroupOption : Parcelable {
        OPTION_1,
        OPTION_2
    }
}
