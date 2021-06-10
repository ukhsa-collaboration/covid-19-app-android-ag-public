package uk.nhs.nhsx.covid19.android.app.about.mydata

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_my_data_section_item.view.myDataSectionItemTitle
import kotlinx.android.synthetic.main.view_my_data_section_item.view.myDataSectionItemValue
import uk.nhs.nhsx.covid19.android.app.R

class MyDataSectionItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var title: String? = ""
        set(value) {
            field = value
            myDataSectionItemTitle.text = value
        }

    var value: String? = ""
        set(value) {
            field = value
            myDataSectionItemValue.text = value
        }

    var stackVertically: Boolean = false
        set(value) {
            field = value
            orientation = if (stackVertically) VERTICAL else HORIZONTAL
        }

    init {
        View.inflate(context, R.layout.view_my_data_section_item, this)
        configureLayout()
    }

    private fun configureLayout() {
        minimumHeight = getDimensionById(R.dimen.my_data_min_height)
        val horizontalPadding = getDimensionById(R.dimen.my_data_section_item_horizontal_padding)
        val verticalPadding = getDimensionById(R.dimen.my_data_padding_vertical)
        setPaddingRelative(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundResource(R.drawable.user_data_item_background)
    }

    private fun getDimensionById(dimenResId: Int): Int {
        return context.resources.getDimension(dimenResId).toInt()
    }
}
