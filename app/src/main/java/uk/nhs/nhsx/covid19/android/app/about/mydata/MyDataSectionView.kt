package uk.nhs.nhsx.covid19.android.app.about.mydata

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.LinearLayout
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.databinding.ViewMyDataSectionBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.getString
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible

class MyDataSectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var sectionItems = mutableListOf<MyDataSectionItemView>()
    private val binding = ViewMyDataSectionBinding.inflate(LayoutInflater.from(context), this)

    init {
        initializeViews()
        applyAttributes(context, attrs)
    }

    fun addItems(vararg items: MyDataSectionItem) {
        items.forEach { item ->
            addSectionItem(item)
        }
        invalidateVisibility()
    }

    fun clear() {
        sectionItems.forEach { removeView(it) }
        sectionItems.clear()
        invalidateVisibility()
    }

    fun setSectionItemStackVertically(shouldItemsStackVertically: Boolean) {
        sectionItems.forEach { it.stackVertically = shouldItemsStackVertically }
    }

    private fun initializeViews() {
        configureLayout()
    }

    private fun applyAttributes(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.MyDataSectionView, 0, 0).apply {
            val title = getString(context, R.styleable.MyDataSectionView_myDataSectionTitle)
            binding.myDataSectionTitle.text = title
            recycle()
        }
    }

    private fun configureLayout() {
        gone()
        orientation = VERTICAL
        setBackgroundColor(getBackgroundColorFromTheme())
    }

    private fun getBackgroundColorFromTheme(): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        return typedValue.data
    }

    private fun addSectionItem(item: MyDataSectionItem) {
        val sectionItemView = MyDataSectionItemView(context)
        sectionItemView.title = item.title
        sectionItemView.value = item.value
        sectionItems.add(sectionItemView)
        addView(sectionItemView)
    }

    private fun invalidateVisibility() {
        if (sectionItems.isNotEmpty()) visible()
    }
}

data class MyDataSectionItem(val title: String, val value: String)
