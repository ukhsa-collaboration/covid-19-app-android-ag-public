package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.util.SparseArray
import android.view.LayoutInflater
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import kotlinx.android.parcel.Parcelize
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.databinding.ViewBinaryRadioGroupBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.getString
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_1
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_2

class BinaryRadioGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewBinaryRadioGroupBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        initializeViews()
        applyAttributes(context, attrs)
    }

    var selectedOption: BinaryRadioGroupOption? = null
        set(value) {
            field = value
            updateButtonSelection()
        }

    private var listener: ((BinaryRadioGroupOption) -> Unit)? = null

    private fun createOnCheckedChangeListener(response: BinaryRadioGroupOption) =
        OnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedOption = response
                listener?.invoke(response)
            }
        }

    fun setOption1Text(text: String, contentDescription: String) = with(binding) {
        binaryRadioButtonOption1.text = text
        binaryRadioButtonOption1.contentDescription = contentDescription
    }

    fun setOption2Text(text: String, contentDescription: String) = with(binding) {
        binaryRadioButtonOption2.text = text
        binaryRadioButtonOption2.contentDescription = contentDescription
    }

    private fun initializeViews() = with(binding) {
        binaryRadioButtonOption1.setOnCheckedChangeListener(createOnCheckedChangeListener(OPTION_1))
        binaryRadioButtonOption2.setOnCheckedChangeListener(createOnCheckedChangeListener(OPTION_2))
    }

    private fun applyAttributes(context: Context, attrs: AttributeSet?) = with(binding) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.BinaryRadioGroup, 0, 0)
            .apply {
                val option1Text = getString(context, R.styleable.BinaryRadioGroup_option1Text)
                binaryRadioButtonOption1.text = option1Text
                val option1ContentDesc = getString(context, R.styleable.BinaryRadioGroup_option1ContentDescription)
                binaryRadioButtonOption1.contentDescription = option1ContentDesc

                val option2ContentDesc = getString(context, R.styleable.BinaryRadioGroup_option2ContentDescription)
                binaryRadioButtonOption2.contentDescription = option2ContentDesc
                val option2Text = getString(context, R.styleable.BinaryRadioGroup_option2Text)
                binaryRadioButtonOption2.text = option2Text

                recycle()
            }
    }

    fun setOnValueChangedListener(listener: (BinaryRadioGroupOption) -> Unit) {
        this.listener = listener
    }

    fun setOptionContentDescriptions(option1ContentDescription: String, option2ContentDescription: String) =
        with(binding) {
            binaryRadioButtonOption1.contentDescription = option1ContentDescription
            binaryRadioButtonOption2.contentDescription = option2ContentDescription
        }

    private fun updateButtonSelection() = with(binding) {
        binaryRadioButtonOption1.setOnCheckedChangeListener(null)
        binaryRadioButtonOption1.isChecked = selectedOption == OPTION_1
        val radioBackground1 =
            if (selectedOption == OPTION_1) R.drawable.radio_selected_background else R.drawable.radio_not_selected_background
        (binaryRadioButtonOption1.parent as FrameLayout).background =
            ContextCompat.getDrawable(context, radioBackground1)

        binaryRadioButtonOption2.setOnCheckedChangeListener(null)
        binaryRadioButtonOption2.isChecked = selectedOption == OPTION_2
        val radioBackground2 =
            if (selectedOption == OPTION_2) R.drawable.radio_selected_background else R.drawable.radio_not_selected_background
        (binaryRadioButtonOption2.parent as FrameLayout).background =
            ContextCompat.getDrawable(context, radioBackground2)

        binaryRadioButtonOption1.setOnCheckedChangeListener(createOnCheckedChangeListener(OPTION_1))
        binaryRadioButtonOption2.setOnCheckedChangeListener(createOnCheckedChangeListener(OPTION_2))
    }

    override fun onSaveInstanceState(): Parcelable? {
        super.onSaveInstanceState()
        return selectedOption
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(BaseSavedState(state))
        selectedOption = state as? BinaryRadioGroupOption
    }

    override fun dispatchRestoreInstanceState(container: SparseArray<Parcelable>?) {
        super.dispatchThawSelfOnly(container)
    }

    override fun dispatchSaveInstanceState(container: SparseArray<Parcelable>?) {
        super.dispatchFreezeSelfOnly(container)
    }

    @Parcelize
    enum class BinaryRadioGroupOption : Parcelable {
        OPTION_1,
        OPTION_2
    }
}
