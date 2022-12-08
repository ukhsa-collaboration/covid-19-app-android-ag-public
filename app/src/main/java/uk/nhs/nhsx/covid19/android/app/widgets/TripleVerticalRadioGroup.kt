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
import uk.nhs.nhsx.covid19.android.app.databinding.ViewTripleVerticalRadioGroupBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.getString
import uk.nhs.nhsx.covid19.android.app.widgets.TripleVerticalRadioGroup.TripleVerticalRadioGroupOption.OPTION_1
import uk.nhs.nhsx.covid19.android.app.widgets.TripleVerticalRadioGroup.TripleVerticalRadioGroupOption.OPTION_2
import uk.nhs.nhsx.covid19.android.app.widgets.TripleVerticalRadioGroup.TripleVerticalRadioGroupOption.OPTION_3

class TripleVerticalRadioGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewTripleVerticalRadioGroupBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        initializeViews()
        applyAttributes(context, attrs)
    }

    var selectedOption: TripleVerticalRadioGroupOption? = null
        set(value) {
            field = value
            updateButtonSelection()
        }

    private var listener: ((TripleVerticalRadioGroupOption) -> Unit)? = null

    private fun createOnCheckedChangeListener(response: TripleVerticalRadioGroupOption) =
        CompoundButton.OnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedOption = response
                listener?.invoke(response)
            }
        }

    private fun initializeViews() = with(binding) {
        tripleVerticalRadioButtonOption1.setOnCheckedChangeListener(createOnCheckedChangeListener(OPTION_1))
        tripleVerticalRadioButtonOption2.setOnCheckedChangeListener(createOnCheckedChangeListener(OPTION_2))
        tripleVerticalRadioButtonOption3.setOnCheckedChangeListener(createOnCheckedChangeListener(OPTION_3))
    }

    private fun applyAttributes(context: Context, attrs: AttributeSet?) = with(binding) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.TripleVerticalRadioGroup, 0, 0)
            .apply {
                val option1Text = getString(context, R.styleable.TripleVerticalRadioGroup_tripleVerticalOption1Text)
                tripleVerticalRadioButtonOption1.text = option1Text
                val option1ContentDesc = getString(context, R.styleable.TripleVerticalRadioGroup_tripleVerticalOption1ContentDescription)
                tripleVerticalRadioButtonOption1.contentDescription = option1ContentDesc

                val option2ContentDesc = getString(context, R.styleable.TripleVerticalRadioGroup_tripleVerticalOption2ContentDescription)
                tripleVerticalRadioButtonOption2.contentDescription = option2ContentDesc
                val option2Text = getString(context, R.styleable.TripleVerticalRadioGroup_tripleVerticalOption2Text)
                tripleVerticalRadioButtonOption2.text = option2Text

                val option3ContentDesc = getString(context, R.styleable.TripleVerticalRadioGroup_tripleVerticalOption3ContentDescription)
                tripleVerticalRadioButtonOption3.contentDescription = option3ContentDesc
                val option3Text = getString(context, R.styleable.TripleVerticalRadioGroup_tripleVerticalOption3Text)
                tripleVerticalRadioButtonOption3.text = option3Text

                recycle()
            }
    }

    fun setOnValueChangedListener(listener: (TripleVerticalRadioGroupOption) -> Unit) {
        this.listener = listener
    }

    private fun updateButtonSelection() = with(binding) {
        tripleVerticalRadioButtonOption1.setOnCheckedChangeListener(null)
        tripleVerticalRadioButtonOption1.isChecked = selectedOption == OPTION_1
        val radioBackground1 =
            if (selectedOption == OPTION_1) R.drawable.radio_selected_background else R.drawable.radio_not_selected_background
        (tripleVerticalRadioButtonOption1.parent as FrameLayout).background =
            ContextCompat.getDrawable(context, radioBackground1)

        tripleVerticalRadioButtonOption2.setOnCheckedChangeListener(null)
        tripleVerticalRadioButtonOption2.isChecked = selectedOption == OPTION_2
        val radioBackground2 =
            if (selectedOption == OPTION_2) R.drawable.radio_selected_background else R.drawable.radio_not_selected_background
        (tripleVerticalRadioButtonOption2.parent as FrameLayout).background =
            ContextCompat.getDrawable(context, radioBackground2)

        tripleVerticalRadioButtonOption3.setOnCheckedChangeListener(null)
        tripleVerticalRadioButtonOption3.isChecked = selectedOption == OPTION_3
        val radioBackground3 =
            if (selectedOption == OPTION_3) R.drawable.radio_selected_background else R.drawable.radio_not_selected_background
        (tripleVerticalRadioButtonOption3.parent as FrameLayout).background =
            ContextCompat.getDrawable(context, radioBackground3)

        tripleVerticalRadioButtonOption1.setOnCheckedChangeListener(createOnCheckedChangeListener(OPTION_1))
        tripleVerticalRadioButtonOption2.setOnCheckedChangeListener(createOnCheckedChangeListener(OPTION_2))
        tripleVerticalRadioButtonOption3.setOnCheckedChangeListener(createOnCheckedChangeListener(OPTION_3))
    }

    override fun onSaveInstanceState(): Parcelable? {
        super.onSaveInstanceState()
        return selectedOption
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(BaseSavedState(state))
        selectedOption = state as? TripleVerticalRadioGroupOption
    }

    override fun dispatchRestoreInstanceState(container: SparseArray<Parcelable>?) {
        super.dispatchThawSelfOnly(container)
    }

    override fun dispatchSaveInstanceState(container: SparseArray<Parcelable>?) {
        super.dispatchFreezeSelfOnly(container)
    }

    @Parcelize
    enum class TripleVerticalRadioGroupOption : Parcelable {
        OPTION_1,
        OPTION_2,
        OPTION_3
    }
}
