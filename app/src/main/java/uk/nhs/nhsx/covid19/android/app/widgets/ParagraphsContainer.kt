/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import uk.nhs.nhsx.covid19.android.app.R
import java.text.NumberFormat

class ParagraphsContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : BaseParagraphsContainer(context, attrs, defStyleAttr, defStyleRes) {
    override val paragraphLayoutRes: Int get() = R.layout.view_paragraph
}

class BulletedParagraphsContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : BaseParagraphsContainer(context, attrs, defStyleAttr, defStyleRes) {
    override val paragraphLayoutRes: Int get() = R.layout.view_paragraph_bulleted
}

class NumberedParagraphsContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : BaseParagraphsContainer(context, attrs, defStyleAttr, defStyleRes) {
    override val paragraphLayoutRes: Int get() = R.layout.view_paragraph_numbered

    override fun configureParagraphLayout(index: Int, text: String) =
        super.configureParagraphLayout(index, text).apply {
            val numberedBulletPointText = findViewById<TextView>(R.id.numberedBulletPointText)
            numberedBulletPointText.text = NumberFormat.getInstance().format(index + 1)
        }
}
