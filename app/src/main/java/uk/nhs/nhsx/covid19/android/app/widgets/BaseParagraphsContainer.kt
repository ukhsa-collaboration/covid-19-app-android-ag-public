/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.core.view.children
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.util.viewutils.getString

typealias GetParagraphContentDescription = (Int, String) -> String

abstract class BaseParagraphsContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {
    private var paddingBetweenItems: Int
    private var textColor: Int = 0

    @get:LayoutRes
    protected abstract val paragraphLayoutRes: Int

    init {
        orientation = VERTICAL
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.BaseParagraphsContainer,
            0,
            0
        ).apply {
            val rawText = getString(context, R.styleable.BaseParagraphsContainer_rawText)
            textColor = getColor(R.styleable.BaseParagraphsContainer_textColor, 0)

            val defaultPadding =
                context.resources.getDimension(R.dimen.paragraph_container_padding_between_items_default).toInt()
            paddingBetweenItems =
                getDimensionPixelSize(R.styleable.BaseParagraphsContainer_paddingBetweenItems, defaultPadding)
            setRawText(rawText)

            recycle()
        }
    }

    fun addAllParagraphs(paragraphs: List<String>) =
        addAllParagraphs(*paragraphs.toTypedArray())

    private fun addAllParagraphs(vararg paragraphs: String) {
        removeAllViews()
        for ((index, paragraph) in paragraphs.withIndex()) {
            addParagraph(index, paragraph)
        }
    }

    fun setRawText(rawText: String, separator: String = "\n\n") =
        addAllParagraphs(*rawText.split(separator).toTypedArray())

    private fun addParagraph(index: Int, text: String): BaseParagraphsContainer {
        val view = configureParagraphLayout(index, text)
        val paragraphText = view.findViewById<TextView>(R.id.paragraphText)
        paragraphText.text = text
        addView(view)

        if (textColor != 0) {
            paragraphText.setTextColor(textColor)
        }

        if (index > 0) {
            val layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(0, paddingBetweenItems, 0, 0)
            view.layoutParams = layoutParams
        }
        return this
    }

    fun setParagraphContentDescriptions(contentDescription: GetParagraphContentDescription) {
        children.forEachIndexed { index, view ->
            val paragraphText = view.findViewById<TextView>(R.id.paragraphText)
            view.contentDescription = contentDescription(index, paragraphText.text.toString())
        }
    }

    @CallSuper
    protected open fun configureParagraphLayout(index: Int, text: String): LinearLayout = inflateParagraphLayout()

    private fun inflateParagraphLayout() =
        ((context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
            .inflate(paragraphLayoutRes, this, false) as LinearLayout)
}
