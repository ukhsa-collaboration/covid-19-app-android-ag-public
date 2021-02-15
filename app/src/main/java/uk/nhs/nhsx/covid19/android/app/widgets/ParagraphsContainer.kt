/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.view_paragraph.view.bulletPoint
import kotlinx.android.synthetic.main.view_paragraph.view.paragraphText
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.util.viewutils.dpToPx

class ParagraphsContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private var shouldDisplayBulletPoints = false

    init {
        orientation = VERTICAL
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.ParagraphsContainer,
            0,
            0
        ).apply {
            val rawText = getText(R.styleable.ParagraphsContainer_rawText)
            shouldDisplayBulletPoints = getBoolean(R.styleable.ParagraphsContainer_showBulletPoints, false)

            rawText?.let {
                setRawText(it.toString())
            }

            recycle()
        }
    }

    fun addAllParagraphs(vararg paragraphs: String) {
        removeAllViews()
        for ((index, paragraph) in paragraphs.withIndex()) {
            addParagraph(index, paragraph)
        }
    }

    private fun addParagraph(index: Int, text: String): ParagraphsContainer {
        val view = inflateLayout()
        view.paragraphText.text = text
        view.bulletPoint.isVisible = shouldDisplayBulletPoints
        addView(view)

        if (index > 0) {
            val layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(0, 16.dpToPx.toInt(), 0, 0)
            view.layoutParams = layoutParams
        }
        return this
    }

    private fun inflateLayout(): LinearLayout =
        (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
            .inflate(R.layout.view_paragraph, this, false) as LinearLayout
}

fun ParagraphsContainer.setRawText(rawText: String, separator: String = "\n\n") =
    addAllParagraphs(*rawText.split(separator).toTypedArray())
